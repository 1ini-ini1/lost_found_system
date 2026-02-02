package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.service.ClaimService;
import com.zjut.lost_found.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认领申请接口层（0基础必懂）
 * 前端调用入口：提交认领、审核认领、撤销认领
 */
@RestController
@RequestMapping("/api/claim")
@RequiredArgsConstructor
@Validated
public class ClaimController {

    private final ClaimService claimService;  // 注入认领申请业务服务
    private final UserService userService;    // 注入用户业务服务

    /**
     * 提交认领申请（POST请求）
     * 访问地址：http://localhost:8080/api/claim/submit?itemId=1&claimerId=1
     */
    @PostMapping("/submit")
    public ApiResponse<Claim> submitClaim(
            @RequestParam Long itemId,        // 物品ID
            @RequestParam Long claimerId,    // 认领人ID
            @RequestParam String proof        // 认领证明
    ) {
        User claimer = userService.getUserById(claimerId);
        Claim savedClaim = claimService.submitClaim(itemId, proof, claimer);
        return ApiResponse.success("认领申请提交成功", savedClaim);
    }

    /**
     * 审核认领申请（PUT请求，管理员）
     * 访问地址：http://localhost:8080/api/claim/audit/1?approve=true&operatorId=2
     */
    @PutMapping("/audit/{claimId}")
    public ApiResponse<Claim> auditClaim(
            @PathVariable Long claimId,          // 申请ID
            @RequestParam Boolean approve,        // 是否通过（true=通过，false=驳回）
            @RequestParam(required = false) String rejectReason,  // 驳回理由
            @RequestParam Long operatorId         // 审核人ID
    ) {
        User operator = userService.getUserById(operatorId);
        Claim auditedClaim = claimService.auditClaim(claimId, approve, rejectReason, operator);
        String message = approve ? "审核通过" : "审核驳回";
        return ApiResponse.success(message, auditedClaim);
    }

    /**
     * 撤销认领申请（PUT请求）
     * 访问地址：http://localhost:8080/api/claim/cancel/1?claimerId=1
     */
    @PutMapping("/cancel/{claimId}")
    public ApiResponse<Claim> cancelClaim(
            @PathVariable Long claimId,    // 申请ID
            @RequestParam Long claimerId   // 认领人ID
    ) {
        User claimer = userService.getUserById(claimerId);
        Claim canceledClaim = claimService.cancelClaim(claimId, claimer);
        return ApiResponse.success("认领申请撤销成功", canceledClaim);
    }

    /**
     * 查询用户的所有认领申请（GET请求）
     * 访问地址：http://localhost:8080/api/claim/my?claimerId=1
     */
    @GetMapping("/my")
    public ApiResponse<List<Claim>> getMyClaims(@RequestParam Long claimerId) {
        User claimer = userService.getUserById(claimerId);
        List<Claim> claims = claimService.findClaimsByClaimer(claimer);
        return ApiResponse.success("查询成功", claims);
    }
}