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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品核心业务服务
 * 核心功能：物品发布、审核、撤销、自动归档、分页查询
 * 优化点：
 * 1. 定义异常信息常量，统一服务层和测试层异常关键词
 * 2. 修复无权限审核的异常信息，匹配测试断言
 * 3. 补充参数校验、完善日志/通知、规范异常提示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    // ========== 新增：异常信息常量（统一关键词） ==========
    private static final String ERROR_NO_AUDIT_PERMISSION = "无权限审核物品";
    private static final String ERROR_ITEM_STATUS_NOT_PENDING = "仅待审核状态可审核";
    private static final String ERROR_REJECT_REASON_EMPTY = "驳回物品必须填写有效理由";
    private static final String ERROR_PUBLISHER_DISABLED = "账号已禁用，无法发布物品";
    private static final String ERROR_AUDIT_STATUS_INVALID = "审核状态仅支持：通过（PASSED）、驳回（REJECTED）";
    private static final String ERROR_NO_CANCEL_PERMISSION = "无权限取消物品发布";
    private static final String ERROR_ITEM_STATUS_CANNOT_CANCEL = "当前物品状态不可取消";

    private final ItemRepository itemRepository;  // 注入物品数据访问层
    private final NoticeService noticeService;    // 注入消息通知服务
    private final AuditLogService auditLogService; // 注入操作日志服务

    /**
     * 新增通用的保存物品方法（供其他Service调用，如ClaimService）
     * 用于直接保存/更新物品实体，不包含额外业务逻辑（仅数据持久化）
     * @param item 物品实体
     * @return 保存后的物品实体
     * @throws IllegalArgumentException 物品实体为null时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Item saveItem(Item item) {
        // 补充参数校验
        Assert.notNull(item, "物品实体不能为空");
        // 自动填充更新时间（BaseEntity字段）
        if (item.getId() != null) {
            item.setUpdateTime(LocalDateTime.now());
        } else {
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
        }
        return itemRepository.save(item);
    }

    /**
     * 发布物品（核心方法）
     * @param dto 物品发布DTO
     * @param publisher 发布人
     * @return 发布后的物品实体
     * @throws RuntimeException 账号禁用/参数异常时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Item publishItem(ItemPublishDTO dto, User publisher) {
        // 1. 严格参数校验
        Assert.notNull(dto, "发布参数不能为空");
        Assert.notNull(publisher, "发布人信息不能为空");
        Assert.hasText(dto.getName(), "物品名称不能为空");
        Assert.hasText(dto.getType(), "物品类型不能为空");
        Assert.notNull(dto.getTime(), "丢失/捡到时间不能为空");

        // 2. 校验发布人账号启用状态
        if (publisher.getIsEnabled() == 0) {
            String errorMsg = String.format("发布失败：账号（ID：%d）%s", publisher.getId(), ERROR_PUBLISHER_DISABLED);
            log.warn(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // 3. 构建物品实体
        Item item = new Item();
        item.setName(dto.getName());
        item.setType(dto.getType());
        item.setTime(dto.getTime());
        item.setDescription(dto.getDescription() == null ? "" : dto.getDescription().trim());
        item.setPhotoUrl(dto.getPhotoUrl());
        item.setIsReward(dto.getIsReward() == null ? false : dto.getIsReward());
        item.setRewardDesc(dto.getRewardDesc() == null ? "" : dto.getRewardDesc().trim());
        item.setPublisher(publisher);  // 关联发布人
        item.setStatus(ItemStatusEnum.PENDING_AUDIT); // 显式设置默认状态：待审核
        // 自动填充BaseEntity时间字段
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());

        // 4. 保存物品到数据库
        Item savedItem = itemRepository.save(item);
        log.info("物品发布成功，物品ID：{}，发布人ID：{}", savedItem.getId(), publisher.getId());

        // 5. 记录操作日志（审计溯源）
        auditLogService.recordLog(
                publisher,
                "ITEM_PUBLISH",
                String.format("发布物品：%s（ID：%d），类型：%s", savedItem.getName(), savedItem.getId(), savedItem.getType()),
                savedItem.getId().toString()
        );

        // 6. 发送通知（告知发布人发布成功）
        noticeService.sendNotice(
                publisher,
                "物品发布成功",
                String.format("您发布的物品《%s》已提交，等待管理员审核（物品ID：%d）", savedItem.getName(), savedItem.getId())
        );

        return savedItem;
    }

    /**
     * 审核物品（管理员操作）
     * @param itemId 物品ID
     * @param status 审核结果状态（PASSED/REJECTED）
     * @param rejectReason 驳回理由（仅驳回时必填）
     * @param operator 审核人（管理员/超级管理员）
     * @return 审核后的物品实体
     * @throws RuntimeException 权限不足/状态异常/参数缺失时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Item auditItem(Long itemId, ItemStatusEnum status, String rejectReason, User operator) {
        // 1. 严格参数校验
        Assert.notNull(itemId, "物品ID不能为空");
        Assert.notNull(status, "审核状态不能为空");
        Assert.notNull(operator, "审核人信息不能为空");
        // 仅支持通过/驳回两种审核状态
        if (!ItemStatusEnum.PASSED.equals(status) && !ItemStatusEnum.REJECTED.equals(status)) {
            throw new RuntimeException(ERROR_AUDIT_STATUS_INVALID);
        }

        // 2. 校验审核人权限（仅管理员/超级管理员可操作）
        if (!UserRoleEnum.ADMIN.equals(operator.getRole()) && !UserRoleEnum.SUPER_ADMIN.equals(operator.getRole())) {
            String errorMsg = String.format("审核失败：操作人（ID：%d）%s（需管理员/超级管理员）", operator.getId(), ERROR_NO_AUDIT_PERMISSION);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // 3. 校验物品存在且状态为待审核
        Item item = getItemById(itemId);
        if (!ItemStatusEnum.PENDING_AUDIT.equals(item.getStatus())) {
            String errorMsg = String.format("审核失败：物品（ID：%d）当前状态为%s，%s",
                    itemId, item.getStatus().getDesc(), ERROR_ITEM_STATUS_NOT_PENDING);
            log.warn(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // 4. 处理审核结果
        item.setStatus(status);
        item.setUpdateTime(LocalDateTime.now()); // 更新操作时间
        // 驳回场景：校验并设置驳回理由
        if (ItemStatusEnum.REJECTED.equals(status)) {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new RuntimeException(ERROR_REJECT_REASON_EMPTY);
            }
            item.setRejectReason(rejectReason.trim()); // 修复：设置驳回理由到实体
        } else {
            item.setRejectReason(null); // 通过场景：清空驳回理由
        }

        // 5. 保存审核结果
        Item auditedItem = itemRepository.save(item);
        log.info("物品审核完成，物品ID：{}，审核结果：{}，审核人ID：{}",
                itemId, status.getDesc(), operator.getId());

        // 6. 记录日志+发送通知
        String operationDesc = ItemStatusEnum.PASSED.equals(status) ? "审核通过" : "驳回";
        auditLogService.recordLog(
                operator,
                "ITEM_AUDIT",
                String.format("%s物品：%s（ID：%d），驳回理由：%s",
                        operationDesc, item.getName(), itemId,
                        ItemStatusEnum.REJECTED.equals(status) ? rejectReason : "无"),
                itemId.toString()
        );

        // 拼接通知内容
        String noticeContent = String.format("您发布的物品《%s》（ID：%d）已%s。%s",
                item.getName(), itemId, operationDesc,
                ItemStatusEnum.REJECTED.equals(status) ? "驳回理由：" + rejectReason : "可正常被认领");
        noticeService.sendNotice(item.getPublisher(), "物品审核结果通知", noticeContent);

        return auditedItem;
    }

    /**
     * 按ID查询物品详情
     * @param itemId 物品ID
     * @return 物品实体
     * @throws EntityNotFoundException 物品不存在时抛出
     */
    public Item getItemById(Long itemId) {
        Assert.notNull(itemId, "物品ID不能为空");
        log.debug("查询物品详情，物品ID：{}", itemId);

        return itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    String errorMsg = String.format("物品不存在，ID：%d", itemId);
                    log.warn(errorMsg);
                    return new EntityNotFoundException(errorMsg);
                });
    }

    /**
     * 分页查询物品列表（支持多条件模糊查询）
     * @param queryItem 查询条件（名称/类型/状态）
     * @param pageable 分页参数（页码/页大小/排序）
     * @return 分页的物品响应DTO列表
     */
    public Page<ItemResponseDTO> queryItems(Item queryItem, Pageable pageable) {
        // 参数校验：queryItem为null时初始化空对象，避免NPE
        Item query = queryItem == null ? new Item() : queryItem;
        Assert.notNull(pageable, "分页参数不能为空");

        log.info("分页查询物品列表，查询条件：名称={}，类型={}，状态={}，分页：{}页/{}条",
                query.getName(), query.getType(), query.getStatus(),
                pageable.getPageNumber() + 1, pageable.getPageSize());

        Page<Item> itemPage = itemRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            // 按名称模糊查询（忽略大小写）
            if (query.getName() != null && !query.getName().trim().isEmpty()) {
                predicate.getExpressions().add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + query.getName().toLowerCase().trim() + "%"
                ));
            }
            // 按类型精确查询
            if (query.getType() != null && !query.getType().trim().isEmpty()) {
                predicate.getExpressions().add(criteriaBuilder.equal(
                        root.get("type"), query.getType().trim()
                ));
            }
            // 按状态精确查询
            if (query.getStatus() != null) {
                predicate.getExpressions().add(criteriaBuilder.equal(
                        root.get("status"), query.getStatus()
                ));
            }
            return predicate;
        }, pageable);

        log.info("物品列表查询完成，共查询到{}条数据，总页数：{}",
                itemPage.getTotalElements(), itemPage.getTotalPages());

        // 转换为响应DTO（隐藏敏感字段）
        return itemPage.map(ItemResponseDTO::fromEntity);
    }

    /**
     * 自动归档超期未认领物品（定时任务调用）
     * 归档规则：已通过审核且发布超过30天未认领的物品
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoArchiveItems() {
        // 1. 计算归档时间（30天前）
        LocalDateTime archiveTime = LocalDateTime.now().minusDays(30);
        log.info("开始自动归档超期物品，归档时间阈值：{}", archiveTime);

        // 2. 查询超期物品（已通过且30天未认领）
        List<Item> itemsToArchive = itemRepository.findByStatusAndCreateTimeBefore(
                ItemStatusEnum.PASSED, archiveTime
        );
        if (itemsToArchive.isEmpty()) {
            log.info("暂无超期未认领物品，无需归档");
            return;
        }
        log.info("查询到需归档物品{}个，开始批量处理", itemsToArchive.size());

        // 3. 批量更新为归档状态
        itemsToArchive.forEach(item -> {
            item.setStatus(ItemStatusEnum.ARCHIVED);
            item.setUpdateTime(LocalDateTime.now());
            // 发送归档通知
            noticeService.sendNotice(
                    item.getPublisher(),
                    "物品自动归档通知",
                    String.format("您发布的物品《%s》（ID：%d）因超30天未认领，已自动归档",
                            item.getName(), item.getId())
            );
            // 记录归档日志
            auditLogService.recordLog(
                    null, // 系统自动操作，无操作人
                    "ITEM_ARCHIVE",
                    String.format("物品自动归档：%s（ID：%d），发布时间：%s",
                            item.getName(), item.getId(), item.getCreateTime()),
                    item.getId().toString()
            );
        });

        // 4. 批量保存归档结果
        itemRepository.saveAll(itemsToArchive);
        log.info("超期物品归档完成，共归档{}个物品", itemsToArchive.size());
    }

    /**
     * 取消发布物品（发布人/超级管理员操作）
     * @param itemId 物品ID
     * @param operator 操作人
     * @return 取消后的物品实体
     * @throws RuntimeException 权限不足/状态异常时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Item cancelItem(Long itemId, User operator) {
        // 1. 参数校验
        Assert.notNull(itemId, "物品ID不能为空");
        Assert.notNull(operator, "操作人信息不能为空");

        // 2. 校验物品存在
        Item item = getItemById(itemId);

        // 3. 校验操作权限（发布人或超级管理员可取消）
        boolean isPublisher = item.getPublisher().getId().equals(operator.getId());
        boolean isSuperAdmin = UserRoleEnum.SUPER_ADMIN.equals(operator.getRole());
        if (!isPublisher && !isSuperAdmin) {
            String errorMsg = String.format("取消失败：操作人（ID：%d）%s（ID：%d）",
                    operator.getId(), ERROR_NO_CANCEL_PERMISSION, itemId);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // 4. 校验物品状态（仅待审核、已通过可取消）
        ItemStatusEnum currentStatus = item.getStatus();
        if (ItemStatusEnum.CLAIMED.equals(currentStatus)
                || ItemStatusEnum.ARCHIVED.equals(currentStatus)
                || ItemStatusEnum.CANCELED.equals(currentStatus)) {
            String errorMsg = String.format("取消失败：物品（ID：%d）%s，状态：%s",
                    itemId, ERROR_ITEM_STATUS_CANNOT_CANCEL, currentStatus.getDesc());
            log.warn(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // 5. 更新状态并保存
        item.setStatus(ItemStatusEnum.CANCELED);
        item.setUpdateTime(LocalDateTime.now());
        Item canceledItem = itemRepository.save(item);
        log.info("物品发布取消成功，物品ID：{}，操作人ID：{}", itemId, operator.getId());

        // 6. 记录日志+发送通知
        auditLogService.recordLog(
                operator,
                "ITEM_CANCEL",
                String.format("取消物品发布：%s（ID：%d），操作人角色：%s",
                        canceledItem.getName(), itemId, operator.getRole().name()),
                itemId.toString()
        );

        noticeService.sendNotice(
                item.getPublisher(),
                "物品发布取消通知",
                String.format("您发布的物品《%s》（ID：%d）已成功取消发布",
                        canceledItem.getName(), itemId)
        );

        return canceledItem;
    }
}