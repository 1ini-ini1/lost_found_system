package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.dto.ItemPublishDTO;
import com.zjut.lost_found.dto.ItemResponseDTO;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 物品接口控制器（0基础必懂）
 * 暴露物品发布、审核、查询、取消等接口
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
@Tag(name = "物品管理接口", description = "物品发布、审核、查询等操作")
public class ItemController extends BaseController {

    private final ItemService itemService;

    /**
     * 发布物品接口（普通用户/管理员均可）
     */
    @PostMapping("/publish")
    @Operation(summary = "发布物品", description = "登录后可发布失物/招领物品，发布后需审核")
    public ApiResponse<Item> publishItem(@Valid @RequestBody ItemPublishDTO dto) {
        User currentUser = getCurrentUser();
        Item item = itemService.publishItem(dto, currentUser);
        return ApiResponse.success("发布成功，等待审核", item);
    }

    /**
     * 审核物品接口（管理员操作）
     */
    @PutMapping("/{itemId}/audit")
    @Operation(summary = "审核物品", description = "仅管理员可调用，审核物品通过/驳回")
    public ApiResponse<Item> auditItem(
            @PathVariable Long itemId,
            @RequestParam ItemStatusEnum status,
            @RequestParam(required = false) String rejectReason
    ) {
        checkAdmin(); // 校验管理员权限
        User operator = getCurrentUser();
        Item item = itemService.auditItem(itemId, status, rejectReason, operator);
        return ApiResponse.success("审核成功", item);
    }

    /**
     * 按ID查询物品详情
     */
    @GetMapping("/{itemId}")
    @Operation(summary = "查询物品详情", description = "公开接口，无需登录即可查询物品详情")
    public ApiResponse<ItemResponseDTO> getItemDetail(@PathVariable Long itemId) {
        Item item = itemService.getItemById(itemId);
        ItemResponseDTO dto = ItemResponseDTO.fromEntity(item);
        return ApiResponse.success("查询成功", dto);
    }

    /**
     * 多条件分页查询物品列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询物品", description = "支持按名称、类型、状态筛选，公开接口")
    public ApiResponse<Page<ItemResponseDTO>> queryItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) ItemStatusEnum status,
            @RequestParam(defaultValue = "0") Integer pageNum, // 页码（从0开始）
            @RequestParam(defaultValue = "10") Integer pageSize // 每页条数
    ) {
        // 构建查询条件
        Item queryItem = new Item();
        queryItem.setName(name);
        queryItem.setType(type);
        queryItem.setStatus(status);
        // 构建分页参数
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<ItemResponseDTO> itemPage = itemService.queryItems(queryItem, pageable);
        return ApiResponse.success("查询成功", itemPage);
    }

    /**
     * 取消物品发布（发布人/超级管理员操作）
     */
    @PutMapping("/{itemId}/cancel")
    @Operation(summary = "取消物品发布", description = "发布人或超级管理员可取消物品发布")
    public ApiResponse<Item> cancelItem(@PathVariable Long itemId) {
        User currentUser = getCurrentUser();
        Item item = itemService.cancelItem(itemId, currentUser);
        return ApiResponse.success("取消发布成功", item);
    }

    /**
     * 自动归档超期物品（定时任务调用，也可手动触发）
     */
    @PostMapping("/auto-archive")
    @Operation(summary = "自动归档物品", description = "仅管理员可调用，归档30天未认领的物品")
    public ApiResponse<Void> autoArchiveItems() {
        checkAdmin();
        itemService.autoArchiveItems();
        return ApiResponse.success("自动归档完成");
    }
}
