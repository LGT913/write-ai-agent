package com.quill.agent.exception;

import org.springframework.http.HttpStatus;

/**
 * 全局错误码（与 02-接口文档.md §1.4 一一对应，v2.0 收敛为 11 项）。
 * 禁止新增未在接口文档登记的对外错误码。
 */
public enum ErrorCode {

    SUCCESS("0", HttpStatus.OK, "成功"),

    ERR_PARAM_001("ERR-PARAM-001", HttpStatus.BAD_REQUEST, "请求参数非法，缺少必填字段"),
    ERR_AUTH_001("ERR-AUTH-001", HttpStatus.UNAUTHORIZED, "缺少或错误的本机令牌（X-Local-Token 校验失败）"),
    ERR_MODEL_001("ERR-MODEL-001", HttpStatus.BAD_REQUEST, "Ollama 服务不可达；本地模式不可用"),
    ERR_MODEL_002("ERR-MODEL-002", HttpStatus.BAD_REQUEST, "请在设置页填入您自己的 API 密钥后重试。"),
    ERR_MODEL_003("ERR-MODEL-003", HttpStatus.BAD_REQUEST, "调用第三方模型 API 失败（密钥错误或配额耗尽）"),
    ERR_SEARCH_001("ERR-SEARCH-001", HttpStatus.BAD_REQUEST, "联网素材开关开启但搜索服务未配置/密钥失效/调用失败"),
    ERR_DOC_001("ERR-DOC-001", HttpStatus.NOT_FOUND, "文档不存在"),
    ERR_VERSION_001("ERR-VERSION-001", HttpStatus.NOT_FOUND, "文档版本不存在"),
    ERR_CONFIG_001("ERR-CONFIG-001", HttpStatus.BAD_REQUEST, "配置文件解析失败，已回滚旧配置"),
    ERR_INTERNAL_001("ERR-INTERNAL-001", HttpStatus.INTERNAL_SERVER_ERROR, "服务内部异常，请稍后重试");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String code() { return code; }
    public HttpStatus httpStatus() { return httpStatus; }
    public String message() { return message; }
}