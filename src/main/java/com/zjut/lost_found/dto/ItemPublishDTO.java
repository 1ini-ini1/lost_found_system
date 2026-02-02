package com.zjut.lost_found.dto;

import jakarta.validation.constraints.Length;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物品发布请求DTO（0基础必懂）
 * 作用：接收前端发布物品的参数，校验合法性
 */
@Data
public class ItemPublishDTO {

    @NotBlank(message = "物品名称不能为空")
    @Length(min = 1, max = 50, message = "物品名称长度需在1-50字符之间")
    private String name;  // 物品名称

    @NotBlank(message = "物品类型不能为空")
    private String type;  // 物品类型（如"电子设备""证件"）

    @NotNull(message = "丢失/拾取时间不能为空")
    private LocalDateTime time;  // 丢失/拾取时间

    @NotBlank(message = "物品描述不能为空")
    @Length(min = 10, max = 500, message = "物品描述长度需在10-500字符之间")
    private String description;  // 物品特征描述

    private String photoUrl;  // 物品图片URL（可选）

    private Boolean isReward = false;  // 是否悬赏（默认不悬赏）

    private String rewardDesc;  // 悬赏说明（可选）
}