package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认领申请接口控制器（0基础必懂）
 * 暴露认领申请提交、审核、查询等接口
 */
@RestController
@RequestMapping("/api/claim")
@RequiredArgsConstructor
@Tag(name = "认领申请接口", description = "认领申请提交、审核、查询等操作")
public class ClaimController extends BaseController {

    private final ClaimService claimService;

    /**
     * 提交认领申请（普通用户操作）
     */
    @PostMapping("/submit")
    @Operation(summary = "提交认领申请", description = "登录后可提交物品认领申请，需填写认领证明")
    public ApiResponse<Claim> submitClaim(
            @RequestParam Long itemId,
            @RequestParam String proof
    ) {
        User currentUser = getCurrentUser();
        Claim claim = claimService.submitClaim(itemId, proof, currentUser);
        return ApiResponse.success("申请提交成功，等待审核", claim);
    }

    /**
     * 审核认领申请（管理员操作）
     */
    @PutMapping("/{claimId}/audit")
    @Operation(summary = "审核认领申请", description = "仅管理员可调用，审核认领申请通过/驳回")
    public ApiResponse<Claim> auditClaim(
            @PathVariable Long claimId,
            @RequestParam String status,
            @RequestParam(required = false) String rejectReason
    ) {
        checkAdmin();
        User operator = getCurrentUser();
        Claim claim = claimService.auditClaim(claimId, status, rejectReason, operator);
        return ApiResponse.success("审核成功", claim);
    }

    /**
     * 查询我的认领申请
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的认领", description = "登录后可查询自己提交的所有认领申请")
    public ApiResponse<List<Claim>> getMyClaims() {
        User currentUser = getCurrentUser();
        List<Claim> claims = claimService.getMyClaims(currentUser);
        return ApiResponse.success("查询成功", claims);
    }

    /**
     * 按物品查询所有认领申请（物品发布人/管理员可查）
     */
    @GetMapping("/item/{itemId}")
    @Operation(summary = "查询物品的认领申请", description = "物品发布人或管理员可查询该物品的所有认领申请")
    public ApiResponse<List<Claim>> getClaimsByItem(@PathVariable Long itemId) {
        User currentUser = getCurrentUser();
        // 校验权限：物品发布人或管理员
        List<Claim> claims = claimService.getClaimsByItem(itemId);
        return ApiResponse.success("查询成功", claims);
    }
}
