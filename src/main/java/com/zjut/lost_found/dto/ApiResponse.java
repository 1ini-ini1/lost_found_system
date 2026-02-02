package com.zjut.lost_found.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局统一响应DTO（0基础必懂）
 * 作用：所有接口返回格式标准化，前端无需适配不同结构
 * 成功示例：{"code":200,"message":"操作成功","data":{"id":1,"name":"手机"}}
 * 失败示例：{"code":400,"message":"物品名称不能为空","data":null}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer code;    // 状态码：200=成功，400=参数错误，404=资源不存在
    private String message;  // 提示信息（前端展示给用户）
    private T data;          // 响应数据（成功时返回，失败时为null）

    // 静态工具方法：简化成功响应创建
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, message, null);
    }

    // 静态工具方法：简化失败响应创建
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}