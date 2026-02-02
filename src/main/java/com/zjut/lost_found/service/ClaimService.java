package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.ClaimRepository;
import com.zjut.lost_found.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认领申请核心业务服务（0基础必懂）
 * 核心功能：提交认领申请、审核申请、撤销申请
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;  // 注入认领申请数据访问层
    private final ItemRepository itemRepository;    // 注入物品数据访问层
    private final NoticeService noticeService;      // 注入消息通知服务
    private final AuditLogService auditLogService;  // 注入操作日志服务

    // ========== 新增缺失的 findClaimsByClaimer 方法 ==========
    /**
     * 根据认领人查询所有认领申请
     */
    public List<Claim> findClaimsByClaimer(User claimer) {
        return claimRepository.findByClaimer(claimer);
    }

    /**
     * 提交认领申请（核心方法）
     */
    @Transactional(rollbackFor = Exception.class)
    public Claim submitClaim(Long itemId, String proof, User claimer) {
        // 1. 校验认领人账号启用
        if (!claimer.getIsEnabled()) {
            throw new RuntimeException("账号已禁用，无法提交认领申请");
        }

        // 2. 校验物品存在且状态为已通过（仅已审核物品可认领）
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("物品不存在，ID：" + itemId));
        if (!ItemStatusEnum.PASSED.equals(item.getStatus())) {
            throw new RuntimeException("物品当前状态不可认领，状态：" + item.getStatus().getDesc());
        }

        // 3. 校验是否重复申请（同一用户不能重复申请同一物品）
        List<Claim> existingClaims = claimRepository.findByItemAndClaimer(item, claimer);
        if (!existingClaims.isEmpty()) {
            throw new RuntimeException("您已提交过该物品的认领申请，请勿重复提交");
        }

        // 4. 构建认领申请实体
        Claim claim = new Claim();
        claim.setItem(item);        // 关联被认领物品
        claim.setClaimer(claimer);  // 关联认领人
        claim.setProof(proof);      // 认领证明
        claim.setStatus("PENDING_AUDIT");  // 状态默认待审核

        // 5. 保存申请
        Claim savedClaim = claimRepository.save(claim);

        // 6. 记录日志+发送通知
        auditLogService.recordLog(claimer, "CLAIM_SUBMIT",
                "提交认领申请：物品《" + item.getName() + "》，申请ID：" + savedClaim.getId(),
                savedClaim.getId().toString());

        // 通知认领人（申请成功）
        noticeService.sendNotice(claimer, "认领申请提交成功",
                "您对物品《" + item.getName() + "》的认领申请已提交，等待管理员审核");
        // 通知物品发布人（新申请提醒）
        noticeService.sendNotice(item.getPublisher(), "新认领申请通知",
                "您发布的物品《" + item.getName() + "》收到新的认领申请，请及时查看");

        return savedClaim;
    }

    /**
     * 审核认领申请（管理员操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Claim auditClaim(Long claimId, Boolean approve, String rejectReason, User operator) {
        // 1. 校验审核人权限
        if (!UserRoleEnum.ADMIN.equals(operator.getRole()) && !UserRoleEnum.SUPER_ADMIN.equals(operator.getRole())) {
            throw new RuntimeException("无权限审核认领申请");
        }

        // 2. 校验申请存在且状态为待审核
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new EntityNotFoundException("认领申请不存在，ID：" + claimId));
        if (!"PENDING_AUDIT".equals(claim.getStatus())) {
            throw new RuntimeException("申请当前状态不可审核，状态：" + claim.getStatus());
        }

        // 3. 校验物品状态合法
        Item item = claim.getItem();
        if (!ItemStatusEnum.PASSED.equals(item.getStatus())) {
            throw new RuntimeException("物品当前状态不可审核认领申请");
        }

        // 4. 处理审核结果
        String newStatus = approve ? "APPROVED" : "REJECTED";
        claim.setStatus(newStatus);
        if (!approve) {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new RuntimeException("驳回申请必须填写理由");
            }
            claim.setRejectReason(rejectReason);
        }

        // 5. 审核通过：更新物品状态为已认领（避免重复认领）
        if (approve) {
            item.setStatus(ItemStatusEnum.CLAIMED);
            itemRepository.save(item);
        }

        // 6. 保存申请+记录日志+发送通知
        Claim updatedClaim = claimRepository.save(claim);

        String operationDesc = approve ? "审核通过" : "驳回";
        auditLogService.recordLog(operator, "CLAIM_AUDIT",
                operationDesc + "认领申请：申请ID" + claimId + "，物品《" + item.getName() + "》",
                claimId.toString());

        // 通知认领人
        noticeService.sendNotice(claim.getClaimer(), "认领申请审核结果",
                "您对物品《" + item.getName() + "》的认领申请已" + operationDesc + "。" +
                        (approve ? "请联系发布人办理认领" : "驳回理由：" + rejectReason));
        // 通知物品发布人
        noticeService.sendNotice(item.getPublisher(), "认领申请审核结果",
                "您发布的物品《" + item.getName() + "》的认领申请已" + operationDesc);

        return updatedClaim;
    }

    /**
     * 撤销认领申请（认领人操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Claim cancelClaim(Long claimId, User claimer) {
        // 1. 校验申请存在
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new EntityNotFoundException("认领申请不存在，ID：" + claimId));

        // 2. 校验权限（仅本人可撤销）
        if (!claimer.getId().equals(claim.getClaimer().getId())) {
            throw new RuntimeException("无权限撤销他人的申请");
        }

        // 3. 校验状态（仅待审核可撤销）
        if (!"PENDING_AUDIT".equals(claim.getStatus())) {
            throw new RuntimeException("申请当前状态不可撤销，状态：" + claim.getStatus());
        }

        // 4. 更新状态为已撤销
        claim.setStatus("CANCELED");
        Claim updatedClaim = claimRepository.save(claim);

        // 5. 记录日志+发送通知
        Item item = claim.getItem();
        auditLogService.recordLog(claimer, "CLAIM_CANCEL",
                "撤销认领申请：申请ID" + claimId + "，物品《" + item.getName() + "》",
                claimId.toString());

        noticeService.sendNotice(claimer, "认领申请撤销成功",
                "您对物品《" + item.getName() + "》的认领申请已撤销");
        noticeService.sendNotice(item.getPublisher(), "认领申请撤销通知",
                "您发布的物品《" + item.getName() + "》的一条认领申请已被撤销");

        return updatedClaim;
    }
}