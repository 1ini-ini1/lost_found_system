package com.zjut.lost_found.service;

import com.zjut.lost_found.dto.ItemPublishDTO;
import com.zjut.lost_found.dto.ItemResponseDTO;
import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.ItemStatusEnum;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品核心业务服务（0基础必懂）
 * 核心功能：物品发布、审核、撤销、自动归档
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;  // 注入物品数据访问层
    private final NoticeService noticeService;    // 注入消息通知服务（后续实现）
    private final AuditLogService auditLogService; // 注入操作日志服务（后续实现）

    /**
     * 发布物品（核心方法）
     */
    @Transactional(rollbackFor = Exception.class)
    public Item publishItem(ItemPublishDTO dto, User publisher) {
        // 1. 校验发布人账号启用
        if (!publisher.getIsEnabled()) {
            throw new RuntimeException("账号已禁用，无法发布物品");
        }

        // 2. 构建物品实体
        Item item = new Item();
        item.setName(dto.getName());
        item.setType(dto.getType());
        item.setTime(dto.getTime());
        item.setDescription(dto.getDescription());
        item.setPhotoUrl(dto.getPhotoUrl());
        item.setIsReward(dto.getIsReward());
        item.setRewardDesc(dto.getRewardDesc());
        item.setPublisher(publisher);  // 关联发布人
        // 状态默认待审核，时间自动填充（继承BaseEntity）

        // 3. 保存物品到数据库
        Item savedItem = itemRepository.save(item);

        // 4. 记录操作日志（审计溯源）
        auditLogService.recordLog(publisher, "ITEM_PUBLISH",
                "发布物品：" + savedItem.getName() + "，ID：" + savedItem.getId(),
                savedItem.getId().toString());

        // 5. 发送通知（告知发布人发布成功）
        noticeService.sendNotice(publisher, "物品发布成功",
                "您发布的物品《" + savedItem.getName() + "》已提交，等待管理员审核");

        return savedItem;
    }

    /**
     * 审核物品（管理员操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Item auditItem(Long itemId, ItemStatusEnum status, String rejectReason, User operator) {
        // 1. 校验审核人权限（仅管理员可操作）
        if (!UserRoleEnum.ADMIN.equals(operator.getRole()) && !UserRoleEnum.SUPER_ADMIN.equals(operator.getRole())) {
            throw new RuntimeException("无权限审核物品");
        }

        // 2. 校验物品存在且状态为待审核
        Item item = getItemById(itemId);
        if (!ItemStatusEnum.PENDING_AUDIT.equals(item.getStatus())) {
            throw new RuntimeException("物品当前状态不可审核，状态：" + item.getStatus().getDesc());
        }

        // 3. 处理审核结果
        item.setStatus(status);
        if (ItemStatusEnum.REJECTED.equals(status)) {
            // 驳回需填写理由
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new RuntimeException("驳回物品必须填写理由");
            }
        }

        // 4. 保存审核结果
        Item auditedItem = itemRepository.save(item);

        // 5. 记录日志+发送通知
        String operationDesc = ItemStatusEnum.PASSED.equals(status) ? "审核通过" : "驳回";
        auditLogService.recordLog(operator, "ITEM_AUDIT",
                operationDesc + "物品：" + item.getName() + "，ID：" + itemId,
                itemId.toString());

        noticeService.sendNotice(item.getPublisher(), "物品审核结果通知",
                "您发布的物品《" + item.getName() + "》已" + operationDesc + "。" +
                        (ItemStatusEnum.REJECTED.equals(status) ? "驳回理由：" + rejectReason : "可正常被认领"));

        return auditedItem;
    }

    /**
     * 按ID查询物品详情
     */
    public Item getItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("物品不存在，ID：" + itemId));
    }

    /**
     * 分页查询物品列表（支持多条件）
     */
    public Page<ItemResponseDTO> queryItems(Item queryItem, Pageable pageable) {
        Page<Item> itemPage = itemRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            // 按名称模糊查询
            if (queryItem.getName() != null && !queryItem.getName().trim().isEmpty()) {
                predicate.getExpressions().add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + queryItem.getName().toLowerCase() + "%"
                ));
            }
            // 按类型查询
            if (queryItem.getType() != null && !queryItem.getType().trim().isEmpty()) {
                predicate.getExpressions().add(criteriaBuilder.equal(root.get("type"), queryItem.getType()));
            }
            // 按状态查询
            if (queryItem.getStatus() != null) {
                predicate.getExpressions().add(criteriaBuilder.equal(root.get("status"), queryItem.getStatus()));
            }
            return predicate;
        }, pageable);

        // 转换为响应DTO
        return itemPage.map(ItemResponseDTO::fromEntity);
    }

    /**
     * 自动归档超期未认领物品（定时任务调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoArchiveItems() {
        // 1. 计算归档时间（30天前）
        LocalDateTime archiveTime = LocalDateTime.now().minusDays(30);

        // 2. 查询超期物品（已通过且30天未认领）
        List<Item> itemsToArchive = itemRepository.findByStatusAndCreateTimeBefore(
                ItemStatusEnum.PASSED, archiveTime
        );

        // 3. 批量更新为归档状态
        itemsToArchive.forEach(item -> {
            item.setStatus(ItemStatusEnum.ARCHIVED);
            // 发送归档通知
            noticeService.sendNotice(item.getPublisher(), "物品自动归档通知",
                    "您发布的物品《" + item.getName() + "》因超30天未认领，已自动归档");
        });

        // 4. 批量保存
        itemRepository.saveAll(itemsToArchive);
    }
}