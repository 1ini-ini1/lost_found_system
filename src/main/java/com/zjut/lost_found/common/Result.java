package com.zjut.lost_found.common; // 包路径需和测试类导入的一致

import lombok.Data;

/**
 * 全局统一响应结果类
 * 所有接口返回数据都通过此类封装，包含状态码、提示信息、数据体
 */
@Data // 使用Lombok自动生成getter/setter/toString等方法
public class Result<T> {
    // 响应状态码（200成功，400参数错误，401未授权，500服务器错误等）
    private Integer code;
    // 响应提示信息
    private String message;
    // 响应数据体（泛型支持任意数据类型）
    private T data;

    // 私有构造方法，禁止外部直接实例化
    private Result() {}

    // ========== 静态构造方法（简化调用） ==========
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 失败响应（自定义状态码和提示信息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 快捷失败响应（默认500状态码）
     */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}