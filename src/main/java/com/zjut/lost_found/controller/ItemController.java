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
 * 物品接口控制器（无需修改，依赖BaseController修复）
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
@Tag(name = "物品管理接口", description = "物品发布、审核、查询等操作")
public class ItemController extends BaseController {

    private final ItemService itemService;

    @PostMapping("/publish")
    @Operation(summary = "发布物品", description = "登录后可发布失物/招领物品，发布后需审核")
    public ApiResponse<Item> publishItem(@Valid @RequestBody ItemPublishDTO dto) {
        User currentUser = getCurrentUser();
        Item item = itemService.publishItem(dto, currentUser);
        return ApiResponse.success("发布成功，等待审核", item);
    }

    @PutMapping("/{itemId}/audit")
    @Operation(summary = "审核物品", description = "仅管理员可调用，审核物品通过/驳回")
    public ApiResponse<Item> auditItem(
            @PathVariable Long itemId,
            @RequestParam ItemStatusEnum status,
            @RequestParam(required = false) String rejectReason
    ) {
        checkAdmin();
        User operator = getCurrentUser();
        Item item = itemService.auditItem(itemId, status, rejectReason, operator);
        return ApiResponse.success("审核成功", item);
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "查询物品详情", description = "公开接口，无需登录即可查询物品详情")
    public ApiResponse<ItemResponseDTO> getItemDetail(@PathVariable Long itemId) {
        Item item = itemService.getItemById(itemId);
        ItemResponseDTO dto = ItemResponseDTO.fromEntity(item);
        return ApiResponse.success("查询成功", dto);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询物品", description = "支持按名称、类型、状态筛选，公开接口")
    public ApiResponse<Page<ItemResponseDTO>> queryItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) ItemStatusEnum status,
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Item queryItem = new Item();
        queryItem.setName(name);
        queryItem.setType(type);
        queryItem.setStatus(status);
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<ItemResponseDTO> itemPage = itemService.queryItems(queryItem, pageable);
        return ApiResponse.success("查询成功", itemPage);
    }

    @PutMapping("/{itemId}/cancel")
    @Operation(summary = "取消物品发布", description = "发布人或超级管理员可取消物品发布")
    public ApiResponse<Item> cancelItem(@PathVariable Long itemId) {
        User currentUser = getCurrentUser();
        Item item = itemService.cancelItem(itemId, currentUser);
        return ApiResponse.success("取消发布成功", item);
    }

    @PostMapping("/auto-archive")
    @Operation(summary = "自动归档物品", description = "仅管理员可调用，归档30天未认领的物品")
    public ApiResponse<Void> autoArchiveItems() {
        checkAdmin();
        itemService.autoArchiveItems();
        return ApiResponse.success("自动归档完成");
    }
}