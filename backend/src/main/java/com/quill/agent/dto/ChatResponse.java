package com.quill.agent.dto;

/**
 * 对话响应（非流式，仅阶段0/1 调试用；D12 起 /api/chat 改为 SSE 流式）。
 */
public record ChatResponse(String conversationId, String content) {
}
