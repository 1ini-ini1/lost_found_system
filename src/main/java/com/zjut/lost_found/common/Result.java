package com.zjut.lost_found.common;

import lombok.Data;

/**
 * 全局统一返回结果类
 * @param <T> 数据泛型
 */
@Data // 自动生成get/set/toString/equals，解决setCode/setMessage/setData报错
public class Result<T> {
    /**
     * 响应码：200成功，500失败，其他自定义
     */
    private Integer code;
    /**
     * 响应消息
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;

    // 私有构造，仅通过静态方法创建
    private Result() {}

    /**
     * 成功：无返回数据
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /**
     * 成功：带返回数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 失败：自定义码+消息
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败：默认500+操作失败
     */
    public static <T> Result<T> fail() {
        return error(500, "操作失败");
    }

    /**
     * 失败：默认500+自定义消息
     */
    public static <T> Result<T> fail(String message) {
        return error(500, message);
    }
}