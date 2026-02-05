package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ClaimStatusEnum;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.ClaimRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 认领申请核心业务服务
 * 核心功能：提交认领申请、审核认领申请、查询我的认领
 * 优化点：
 * 1. 枚举规范化（替换硬编码状态字符串）
 * 2. 增强参数校验，统一异常提示
 * 3. 提取常量，提升代码可读性
 * 4. 标准化异常处理和日志输出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    // ========== 常量定义（避免魔法值） ==========
    private static final String ERROR_ACCOUNT_DISABLED = "账号已禁用，无法提交认领申请";
    private static final String ERROR_ITEM_CANNOT_CLAIM = "当前物品不可认领，状态：%s";
    private static final String ERROR_DUPLICATE_CLAIM = "您已提交该物品的认领申请，请勿重复提交";
    private static final String ERROR_PROOF_EMPTY = "认领证明不能为空";
    private static final String ERROR_NO_AUDIT_PERMISSION = "无权限审核认领申请（仅管理员/超级管理员可操作）";
    private static final String ERROR_CLAIM_STATUS_NOT_PENDING = "申请当前状态不可审核，状态：%s";
    private static final String ERROR_AUDIT_STATUS_INVALID = "审核状态无效，仅支持：%s";
    private static final String ERROR_REJECT_REASON_EMPTY = "驳回认领申请必须填写理由";
    private static final String ERROR_CLAIM_NOT_FOUND = "认领申请不存在，ID：%d";

    // ========== 依赖注入 ==========
    private final ClaimRepository claimRepository;
    private final ItemService itemService;
    private final UserService userService;
    private final NoticeService noticeService;
    private final AuditLogService auditLogService;

    /**
     * 提交认领申请（普通用户操作）
     * @param itemId 物品ID
     * @param proof 认领证明（物品细节描述，用于核实身份）
     * @param claimer 认领人（当前登录用户）
     * @return 保存后的认领申请记录
     * @throws RuntimeException 校验失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Claim submitClaim(Long itemId, String proof, User claimer) {
        // 1. 基础参数校验
        Assert.notNull(itemId, "物品ID不能为空");
        Assert.notNull(claimer, "认领人信息不能为空");

        // 2. 校验认领人账号启用状态
        if (claimer.getIsEnabled() == 0) {
            log.warn("提交认领失败：账号禁用，用户ID：{}", claimer.getId());
            throw new RuntimeException(ERROR_ACCOUNT_DISABLED);
        }

        // 3. 校验物品存在且状态为已通过（仅已通过物品可认领）
        Item item = itemService.getItemById(itemId);
        if (!ItemStatusEnum.PASSED.equals(item.getStatus())) {
            log.warn("提交认领失败：物品不可认领，物品ID：{}，状态：{}", itemId, item.getStatus().getDesc());
            throw new RuntimeException(String.format(ERROR_ITEM_CANNOT_CLAIM, item.getStatus().getDesc()));
        }

        // 4. 校验是否重复申请（同一用户不可重复申请同一物品）
        List<Claim> existingClaims = claimRepository.findByItemAndClaimer(item, claimer);
        if (!existingClaims.isEmpty()) {
            log.warn("提交认领失败：重复申请，物品ID：{}，用户ID：{}", itemId, claimer.getId());
            throw new RuntimeException(ERROR_DUPLICATE_CLAIM);
        }

        // 5. 校验认领证明非空且非空白
        if (!StringUtils.hasText(proof)) {
            log.warn("提交认领失败：认领证明为空，用户ID：{}", claimer.getId());
            throw new RuntimeException(ERROR_PROOF_EMPTY);
        }

        // 6. 构建认领申请实体
        Claim claim = new Claim();
        claim.setProof(proof.trim()); // 去除首尾空格
        claim.setItem(item);
        claim.setClaimer(claimer);
        claim.setStatus(ClaimStatusEnum.PENDING_AUDIT.name()); // 显式设置默认状态（避免依赖BaseEntity）

        // 7. 保存申请到数据库
        Claim savedClaim = claimRepository.save(claim);
        log.info("认领申请提交成功，申请ID：{}，物品ID：{}，用户ID：{}",
                savedClaim.getId(), itemId, claimer.getId());

        // 8. 记录操作日志
        auditLogService.recordLog(
                claimer,
                "CLAIM_SUBMIT",
                String.format("提交认领申请：物品【%s】（ID：%d），申请ID：%d",
                        item.getName(), itemId, savedClaim.getId()),
                savedClaim.getId().toString()
        );

        // 9. 发送通知
        // 9.1 告知认领人提交成功
        noticeService.sendNotice(
                claimer,
                "认领申请提交成功",
                String.format("您对物品《%s》的认领申请已提交，等待管理员审核", item.getName())
        );
        // 9.2 告知物品发布人有新申请
        noticeService.sendNotice(
                item.getPublisher(),
                "新认领申请通知",
                String.format("您发布的物品《%s》收到新的认领申请，可前往管理后台查看", item.getName())
        );

        return savedClaim;
    }

    /**
     * 审核认领申请（管理员操作）
     * @param claimId 认领申请ID
     * @param auditStatus 审核状态（PASSED/REJECTED，对应ClaimStatusEnum）
     * @param rejectReason 驳回理由（仅驳回时必填）
     * @param operator 审核人（当前登录的管理员）
     * @return 审核后的认领申请记录
     * @throws RuntimeException 校验失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Claim auditClaim(Long claimId, String auditStatus, String rejectReason, User operator) {
        // 1. 基础参数校验
        Assert.notNull(claimId, "认领申请ID不能为空");
        Assert.notNull(auditStatus, "审核状态不能为空");
        Assert.notNull(operator, "审核人信息不能为空");

        // 2. 校验审核人权限（仅管理员/超级管理员可操作）
        if (!UserRoleEnum.ADMIN.equals(operator.getRole()) && !UserRoleEnum.SUPER_ADMIN.equals(operator.getRole())) {
            log.warn("审核认领失败：无权限，用户ID：{}，角色：{}", operator.getId(), operator.getRole().name());
            throw new RuntimeException(ERROR_NO_AUDIT_PERMISSION);
        }

        // 3. 校验申请存在且状态为待审核
        Claim claim = getClaimById(claimId);
        if (!ClaimStatusEnum.PENDING_AUDIT.name().equals(claim.getStatus())) {
            log.warn("审核认领失败：状态不可审核，申请ID：{}，当前状态：{}", claimId, claim.getStatus());
            throw new RuntimeException(String.format(ERROR_CLAIM_STATUS_NOT_PENDING, claim.getStatus()));
        }

        // 4. 校验审核状态合法性
        String validStatus = String.format("%s/%s", ClaimStatusEnum.APPROVED.name(), ClaimStatusEnum.REJECTED.name());
        if (!ClaimStatusEnum.APPROVED.name().equals(auditStatus) && !ClaimStatusEnum.REJECTED.name().equals(auditStatus)) {
            log.warn("审核认领失败：状态无效，申请ID：{}，传入状态：{}", claimId, auditStatus);
            throw new RuntimeException(String.format(ERROR_AUDIT_STATUS_INVALID, validStatus));
        }

        // 5. 处理审核结果
        claim.setStatus(auditStatus);
        Item item = claim.getItem();
        User claimer = claim.getClaimer();

        if (ClaimStatusEnum.REJECTED.name().equals(auditStatus)) {
            // 5.1 驳回：校验驳回理由
            if (!StringUtils.hasText(rejectReason)) {
                log.warn("审核认领失败：驳回理由为空，申请ID：{}", claimId);
                throw new RuntimeException(ERROR_REJECT_REASON_EMPTY);
            }
            claim.setRejectReason(rejectReason.trim());
        } else {
            // 5.2 通过：更新物品状态为已认领，避免重复认领
            item.setStatus(ItemStatusEnum.CLAIMED);
            itemService.saveItem(item);
            log.info("认领审核通过，更新物品状态为已认领，物品ID：{}", item.getId());
        }

        // 6. 保存审核结果
        Claim auditedClaim = claimRepository.save(claim);
        String operationDesc = ClaimStatusEnum.APPROVED.name().equals(auditStatus) ? "审核通过" : "驳回";
        log.info("认领申请审核完成，申请ID：{}，审核结果：{}", claimId, operationDesc);

        // 7. 记录操作日志
        auditLogService.recordLog(
                operator,
                "CLAIM_AUDIT",
                String.format("%s认领申请：物品【%s】（ID：%d），申请ID：%d",
                        operationDesc, item.getName(), item.getId(), claimId),
                claimId.toString()
        );

        // 8. 发送审核结果通知
        String resultDesc = ClaimStatusEnum.APPROVED.name().equals(auditStatus) ? "已通过" : "已驳回";
        String claimerNoticeContent = String.format(
                "您对物品《%s》的认领申请%s。%s",
                item.getName(),
                resultDesc,
                ClaimStatusEnum.REJECTED.name().equals(auditStatus) ?
                        "驳回理由：" + rejectReason : "请联系物品发布人完成交接"
        );
        String publisherNoticeContent = String.format(
                "您发布的物品《%s》的认领申请%s，认领人：%s（ID：%d）",
                item.getName(),
                resultDesc,
                claimer.getName(),
                claimer.getId()
        );

        noticeService.sendNotice(claimer, "认领申请审核结果", claimerNoticeContent);
        noticeService.sendNotice(item.getPublisher(), "认领申请审核结果", publisherNoticeContent);

        return auditedClaim;
    }

    /**
     * 按ID查询认领申请详情
     * @param claimId 认领申请ID
     * @return 认领申请详情
     * @throws EntityNotFoundException 申请不存在时抛出
     */
    public Claim getClaimById(Long claimId) {
        Assert.notNull(claimId, "认领申请ID不能为空");
        return claimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.error(ERROR_CLAIM_NOT_FOUND, claimId);
                    return new EntityNotFoundException(String.format(ERROR_CLAIM_NOT_FOUND, claimId));
                });
    }

    /**
     * 按认领人查询我的认领申请
     * @param claimer 认领人（当前登录用户）
     * @return 该用户的所有认领申请列表
     */
    public List<Claim> getMyClaims(User claimer) {
        Assert.notNull(claimer, "认领人信息不能为空");
        log.info("查询我的认领申请，用户ID：{}", claimer.getId());
        return claimRepository.findByClaimer(claimer);
    }

    /**
     * 按物品查询所有认领申请（供物品发布人/管理员查看）
     * @param itemId 物品ID
     * @return 该物品的所有认领申请列表
     */
    public List<Claim> getClaimsByItem(Long itemId) {
        Assert.notNull(itemId, "物品ID不能为空");
        Item item = itemService.getItemById(itemId);
        log.info("查询物品的认领申请，物品ID：{}", itemId);
        return claimRepository.findByItem(item);
    }

    /**
     * 校验用户是否有权限查询物品的认领申请
     * 规则：物品发布人 或 管理员/超级管理员 可查询
     * @param itemId 物品ID
     * @param currentUser 当前登录用户
     * @return true=有权限，false=无权限
     */
    public boolean checkItemClaimQueryPermission(Long itemId, User currentUser) {
        Assert.notNull(itemId, "物品ID不能为空");
        Assert.notNull(currentUser, "当前用户信息不能为空");

        Item item = itemService.getItemById(itemId);
        boolean isPublisher = item.getPublisher().getId().equals(currentUser.getId());
        boolean isAdmin = UserRoleEnum.ADMIN.equals(currentUser.getRole()) ||
                UserRoleEnum.SUPER_ADMIN.equals(currentUser.getRole());

        log.info("校验认领申请查询权限，物品ID：{}，用户ID：{}，是否发布人：{}，是否管理员：{}",
                itemId, currentUser.getId(), isPublisher, isAdmin);
        return isPublisher || isAdmin;
    }
}