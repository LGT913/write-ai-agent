package com.quill.agent.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 对话请求（阶段0/1 精简版）。
 * 阶段2 D18 会扩展 fileContext 与 modelConfig（apiKey 仅单次请求内存，禁止落库落日志）。
 */
public record ChatRequest(
        /** 为空表示新建会话，由后端生成 conversationId 并返回 */
        String conversationId,
        @NotBlank(message = "userMessage 不能为空")
        String userMessage
) {
}
