package com.zjut.lost_found.dto;

import com.zjut.lost_found.entity.Item;
import com.zjut.lost_found.enums.ItemStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 物品响应DTO（0基础必懂）
 * 作用：格式化返回数据，隐藏敏感信息，适配前端展示
 */
@Data
public class ItemResponseDTO {

    private Long id;          // 物品ID
    private String name;      // 物品名称
    private String type;      // 物品类型
    private String time;      // 丢失/拾取时间（格式化后）
    private String description; // 物品描述
    private String photoUrl;  // 图片URL
    private String isReward;  // 是否悬赏（转中文：是/否）
    private String rewardDesc; // 悬赏说明
    private String status;    // 物品状态（转中文：待审核/已通过）
    private String publisherName; // 发布人姓名（隐藏账号/密码）
    private String createTime; // 发布时间（格式化后）

    /**
     * 实体转DTO（核心映射逻辑）
     * 把数据库查询的Item实体，转换成前端需要的格式
     */
    public static ItemResponseDTO fromEntity(Item item) {
        ItemResponseDTO dto = new ItemResponseDTO();
        // 基础字段映射
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setType(item.getType());
        dto.setDescription(item.getDescription());
        dto.setPhotoUrl(item.getPhotoUrl());
        dto.setRewardDesc(item.getRewardDesc());

        // 时间格式化：LocalDateTime→字符串（前端无法直接解析）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dto.setTime(item.getTime().format(formatter));
        dto.setCreateTime(item.getCreateTime().format(formatter));

        // 布尔值转中文：提升用户体验
        dto.setIsReward(item.getIsReward() ? "是" : "否");

        // 枚举转中文：调用枚举的getDesc()方法
        dto.setStatus(item.getStatus().getDesc());

        // 敏感信息脱敏：仅返回发布人姓名，不返回账号/密码
        dto.setPublisherName(item.getPublisher().getName());

        return dto;
    }
}