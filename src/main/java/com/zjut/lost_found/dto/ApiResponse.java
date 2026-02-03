package com.zjut.lost_found.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局统一响应DTO（0基础必懂）
 * 作用：所有接口返回格式标准化，前端无需适配不同结构
 * 成功示例：{"code":200,"message":"操作成功","data":{"id":1,"name":"手机"}}
 * 失败示例：{"code":400,"message":"物品名称不能为空","data":null}
 * @param <T> 泛型，data字段可以是任意类型（如User、Item、List等）
 */
@Data // 自动生成get/set方法
@NoArgsConstructor // 无参构造器（JSON解析需要）
@AllArgsConstructor // 全参构造器（快速创建对象）
public class ApiResponse<T> {
    private Integer code;    // 状态码：200=成功，400=参数错误，401=未授权，403=权限不足，404=资源不存在
    private String message;  // 提示信息（前端展示给用户，如“操作成功”“账号已存在”）
    private T data;          // 响应数据（成功时返回，失败时为null

// 静态工具方法：简化成功响应创建（无需手动new