package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.dto.ItemPublishDTO;
import com.zjut.lost_found.dto.ItemResponseDTO;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.service.ItemService;
import com.zjut.lost_found.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 物品接口层（0基础必懂）
 * 前端调用入口：物品发布、审核、查询、撤销
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
@Validated
public class ItemController {

    private final ItemService itemService;  // 注入物品业务服务
    private final UserService userService;  // 注入用户业务服务

    /**
     * 发布物品接口（POST请求）
     * 访问地址：http://localhost:8080/api/item/publish?publisherId=1
     */
    @PostMapping("/publish")
    public ApiResponse<Item> publishItem(
            @Valid @RequestBody ItemPublishDTO dto,  // 请求体：发布参数
            @RequestParam Long publisherId          // URL参数：发布人ID
    ) {
        // 获取发布人
        User publisher = userService.getUserById(publisherId);
        Item savedItem = itemService.publishItem(dto, publisher);
        return ApiResponse.success("物品发布成功，等待审核", savedItem);
    }

    /**
     * 审核物品接口（PUT请求，管理员）
     * 访问地址：http://localhost:8080/api/item/audit/1?status=PASSED&operatorId=2
     */
    @PutMapping("/audit/{itemId}")
    public ApiResponse<Item> auditItem(
            @PathVariable Long itemId,                // 路径参数：物品ID
            @RequestParam ItemStatusEnum status,      // URL参数：审核结果
            @RequestParam(required = false) String rejectReason,  // 驳回理由
            @RequestParam Long operatorId             // URL参数：审核人ID
    ) {
        User operator = userService.getUserById(operatorId);
        Item auditedItem = itemService.auditItem(itemId, status, rejectReason, operator);
        String message = ItemStatusEnum.PASSED.equals(status) ? "审核通过" : "审核驳回";
        return ApiResponse.success(message, auditedItem);
    }

    /**
     * 分页查询物品列表（GET请求）
     * 访问地址：http://localhost:8080/api/item/list?name=手机&pageNum=0&pageSize=10
     */
    @GetMapping("/list")
    public ApiResponse<Page<ItemResponseDTO>> queryItems(
            @RequestParam(required = false) String name,  // 物品名称（模糊查询）
            @RequestParam(required = false) String type,  // 物品类型
            @RequestParam(required = false) ItemStatusEnum status,  // 物品状态
            @RequestParam(defaultValue = "0") Integer pageNum,  // 页码（默认第0页）
            @RequestParam(defaultValue = "10") Integer pageSize  // 每页条数
    ) {
        // 构建查询条件
        Item queryItem = new Item();
        queryItem.setName(name);
        queryItem.setType(type);
        queryItem.setStatus(status);

        // 构建分页参数（按创建时间倒序）
        Pageable pageable = PageRequest.of(
                pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createTime")
        );

        // 调用服务查询
        Page<ItemResponseDTO> itemPage = itemService.queryItems(queryItem, pageable);
        return ApiResponse.success("查询成功", itemPage);
    }

    /**
     * 物品详情接口（GET请求）
     * 访问地址：http://localhost:8080/api/item/{itemId}
     */
    @GetMapping("/{itemId}")
    public ApiResponse<ItemResponseDTO> getItemDetail(@PathVariable Long itemId) {
        Item item = itemService.getItemById(itemId);
        return ApiResponse.success("查询成功", ItemResponseDTO.fromEntity(item));
    }
}