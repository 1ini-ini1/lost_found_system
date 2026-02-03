package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.ClaimRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认领申请核心业务服务（0基础必懂）
 * 核心功能：提交认领申请、审核认领申请、查询我的认领
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;  // 注入认领申请数据访问层
    private final ItemService itemService;          // 注入物品服务
    private final UserService userService;          // 注入用户服务
    private final NoticeService noticeService;      // 注入通知服务
    private final AuditLogService auditLogService;  // 注入操作日志服务

    /**
     * 提交认领申请（普通用户操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Claim submitClaim(Long itemId, String proof, User claimer) {
        // 1. 校验认领人账号启用
        if (!claimer.getIsEnabled()) {
            throw new RuntimeException("账号已禁用，无法提交认领申请");
        }

        // 2. 校验物品存在且状态为已通过（仅已通过物品可认领）
        Item item = itemService.getItemById(itemId);
        if (!ItemStatusEnum.PASSED.equals(item.getStatus())) {
            throw new RuntimeException("当前物品不可认领，状态：" + item.getStatus().getDesc());
        }

        // 3. 校验是否重复申请（同一用户不可重复申请同一物品）
        List<Claim> existingClaims = claimRepository.findByItemAndClaimer(item, claimer);
        if (!existingClaims.isEmpty()) {
            throw new RuntimeException("您已提交该物品的认领申请，请勿重复提交");
        }

        // 4. 校验认领证明非空
        if (proof == null || proof.trim().isEmpty()) {
            throw new RuntimeException("认领证明不能为空");
        }

        // 5. 构建认领申请实体
        Claim claim = new Claim();
        claim.setProof(proof);
        claim.setItem(item);
        claim.setClaimer(claimer);
        // 状态默认待审核，时间自动填充（继承BaseEntity）

        // 6. 保存申请到数据库
        Claim savedClaim = claimRepository.save(claim);

        // 7. 记录操作日志
        auditLogService.recordLog(claimer, "CLAIM_SUBMIT",
                "提交认领申请：物品" + item.getName() + "，申请ID：" + savedClaim.getId(),
                savedClaim.getId().toString());

        // 8. 发送通知（告知认领人提交成功，同时告知物品发布人有新申请）
        noticeService.sendNotice(claimer, "认领申请提交成功",
                "您对物品《" + item.getName() + "》的认领申请已提交，等待管理员审核");
        noticeService.sendNotice(item.getPublisher(), "新认领申请通知",
                "您发布的物品《" + item.getName() + "》收到新的认领申请，可前往查看");

        return savedClaim;
    }

    /**
     * 审核认领申请（管理员操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Claim auditClaim(Long claimId, String status, String rejectReason, User operator) {
        // 1. 校验审核人权限（仅管理员可操作）
        if (!UserRoleEnum.ADMIN.equals(operator.getRole()) && !UserRoleEnum.SUPER_ADMIN.equals(operator.getRole())) {
            throw new RuntimeException("无权限审核认领申请");
        }

        // 2. 校验申请存在且状态为待审核
        Claim claim = getClaimById(claimId);
        if (!"PENDING_AUDIT".equals(claim.getStatus())) {
            throw new RuntimeException("申请当前状态不可审核，状态：" + claim.getStatus());
        }

        // 3. 校验审核状态合法性
        if (!"PASSED".equals(status) && !"REJECTED".equals(status)) {
            throw new RuntimeException("审核状态无效，仅支持已通过/已驳回");
        }

        // 4. 处理审核结果
        claim.setStatus(status);
        if ("REJECTED".equals(status)) {
            // 驳回需填写理由
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new RuntimeException("驳回认领申请必须填写理由");
            }
            claim.setRejectReason(rejectReason);
        } else {
            // 审核通过：将物品状态改为已认领，避免重复认领
            Item item = claim.getItem();
            item.setStatus(ItemStatusEnum.CLAIMED);
            itemService.saveItem(item); // 调用物品仓库保存状态
        }

        // 5. 保存审核结果
        Claim auditedClaim = claimRepository.save(claim);
        Item item = auditedClaim.getItem();
        User claimer = auditedClaim.getClaimer();

        // 6. 记录操作日志
        String operationDesc = "PASSED".equals(status) ? "审核通过" : "驳回";
        auditLogService.recordLog(operator, "CLAIM_AUDIT",
                operationDesc + "认领申请：物品" + item.getName() + "，申请ID：" + claimId,
                claimId.toString());

        // 7. 发送通知（告知认领人审核结果，同时告知物品发布人）
        String resultDesc = "PASSED".equals(status) ? "已通过" : "已驳回";
        noticeService.sendNotice(claimer, "认领申请审核结果",
                "您对物品《" + item.getName() + "》的认领申请" + resultDesc + "。" +
                        ("REJECTED".equals(status) ? "驳回理由：" + rejectReason : "请联系物品发布人完成交接"));
        noticeService.sendNotice(item.getPublisher(), "认领申请审核结果",
                "您发布的物品《" + item.getName() + "》的认领申请" + resultDesc + "，认领人：" + claimer.getName());

        return auditedClaim;
    }

    /**
     * 按ID查询认领申请详情
     */
    public Claim getClaimById(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new EntityNotFoundException("认领申请不存在，ID：" + claimId));
    }

    /**
     * 按认领人查询我的认领申请
     */
    public List<Claim> getMyClaims(User claimer) {
        return claimRepository.findByClaimer(claimer);
    }

    /**
     * 按物品查询所有认领申请（供物品发布人查看）
     */
    public List<Claim> getClaimsByItem(Long itemId) {
        Item item = itemService.getItemById(itemId);
        return claimRepository.findByItem(item);
    }

    // 提供物品仓库访问权限（供内部调用）
    public ItemService getItemService() {
        return itemService;
    }
}