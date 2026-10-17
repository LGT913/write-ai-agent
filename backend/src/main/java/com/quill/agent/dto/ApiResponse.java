package com.quill.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应报文：{@code {code, message, requestId, data}}。
 * 业务错误结构见 02-接口文档.md §1.4：{@code {code, message, requestId}}。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, String requestId, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("0", null, null, data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>("0", null, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(code, message, requestId, null);
    }
}
