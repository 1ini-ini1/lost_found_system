package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.entity.Claim;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ClaimStatusEnum;
import com.zjut.lost_found.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认领申请接口控制器
 * 暴露认领申请提交、审核、查询等接口
 */
@RestController
@RequestMapping("/api/claim")
@RequiredArgsConstructor
@Tag(name = "认领申请接口", description = "认领申请提交、审核、查询等操作")
@Validated // 开启参数校验
public class ClaimController extends BaseController {

    private final ClaimService claimService;

    /**
     * 提交认领申请（普通用户操作）
     *
     * @param itemId 物品ID（必填）
     * @param proof  认领证明（必填，如物品特征描述、购买凭证等）
     * @return 提交成功的认领申请信息
     */
    @PostMapping("/submit")
    @Operation(summary = "提交认领申请", description = "登录后可提交物品认领申请，需填写认领证明")
    public ApiResponse<Claim> submitClaim(
            @Parameter(description = "物品ID", required = true)
            @RequestParam @NotNull(message = "物品ID不能为空") Long itemId,
            @Parameter(description = "认领证明", required = true)
            @RequestParam @NotBlank(message = "认领证明不能为空") String proof
    ) {
        User currentUser = getCurrentUser();
        Claim claim = claimService.submitClaim(itemId, proof, currentUser);
        return ApiResponse.success("申请提交成功，等待审核", claim);
    }

    /**
     * 审核认领申请（管理员操作）
     *
     * @param claimId      认领申请ID
     * @param status       审核状态（APPROVED-通过，REJECTED-驳回）
     * @param rejectReason 驳回原因（仅驳回时必填）
     * @return 审核后的认领申请信息
     */
    @PutMapping("/{claimId}/audit")
    @Operation(summary = "审核认领申请", description = "仅管理员可调用，审核认领申请通过/驳回")
    public ApiResponse<Claim> auditClaim(
            @Parameter(description = "认领申请ID", required = true)
            @PathVariable @NotNull(message = "认领申请ID不能为空") Long claimId,
            @Parameter(description = "审核状态（APPROVED-通过，REJECTED-驳回）", required = true)
            @RequestParam @NotBlank(message = "审核状态不能为空") String status,
            @Parameter(description = "驳回原因（仅驳回时必填）")
            @RequestParam(required = false) String rejectReason
    ) {
        // 1. 校验管理员权限
        checkAdmin();
        // 2. 校验状态合法性
        if (!ClaimAuditStatusEnum.contains(status)) {
            return ApiResponse.error("审核状态非法，仅支持：" + ClaimAuditStatusEnum.getValidValues());
        }
        // 3. 驳回时校验驳回原因
        if (ClaimAuditStatusEnum.REJECTED.name().equals(status) && (rejectReason == null || rejectReason.isBlank())) {
            return ApiResponse.error("驳回认领申请时，驳回原因不能为空");
        }

        User operator = getCurrentUser();
        Claim claim = claimService.auditClaim(claimId, status, rejectReason, operator);
        return ApiResponse.success("审核成功", claim);
    }

    /**
     * 查询我的认领申请
     *
     * @return 当前用户提交的所有认领申请列表
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
     *
     * @param itemId 物品ID
     * @return 该物品的所有认领申请列表
     */
    @GetMapping("/item/{itemId}")
    @Operation(summary = "查询物品的认领申请", description = "物品发布人或管理员可查询该物品的所有认领申请")
    public ApiResponse<List<Claim>> getClaimsByItem(
            @Parameter(description = "物品ID", required = true)
            @PathVariable @NotNull(message = "物品ID不能为空") Long itemId
    ) {
        User currentUser = getCurrentUser();
        // 1. 校验权限：物品发布人或管理员（核心修复点）
        boolean hasPermission = claimService.checkItemClaimQueryPermission(itemId, currentUser);
        if (!hasPermission) {
            return ApiResponse.error("无权限查询该物品的认领申请");
        }
        // 2. 查询认领申请
        List<Claim> claims = claimService.getClaimsByItem(itemId);
        return ApiResponse.success("查询成功", claims);
    }

    // 建议新增的审核状态枚举（可单独放到enums包下）
    enum ClaimAuditStatusEnum {
        APPROVED, REJECTED;

        public static boolean contains(String status) {
            for (ClaimAuditStatusEnum value : values()) {
                if (value.name().equals(status)) {
                    return true;
                }
            }
            return false;
        }

        public static String getValidValues() {
            StringBuilder sb = new StringBuilder();
            for (ClaimAuditStatusEnum value : values()) {
                sb.append(value.name()).append(",");
            }
            return sb.deleteCharAt(sb.length() - 1).toString();
        }
    }
}