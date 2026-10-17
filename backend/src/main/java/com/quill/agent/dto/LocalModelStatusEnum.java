package com.quill.agent.dto;

/**
 * 本地Ollama健康状态枚举，对齐接口文档§2.1三状态约定
 */
public enum LocalModelStatusEnum {
    /** 无法连接Ollama服务（不区分未安装/已安装未启动） */
    UNAVAILABLE,
    /** 服务可达，但目标模型未下载 */
    MODEL_MISSING,
    /** 服务正常，模型就绪 */
    READY
}
