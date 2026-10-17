package com.quill.agent.controller;

import com.quill.agent.dto.ApiResponse;
import com.quill.agent.dto.ChatRequest;
import com.quill.agent.dto.ChatResponse;
import com.quill.agent.service.modelrouter.OllamaChatService;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 对话控制器（阶段0/1 非流式版本，仅用于打通 Ollama 链路）。
 * 阶段1 D12 改造成 SSE 流式，事件白名单：session/token/citation/agent_status/approval/error/complete。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final OllamaChatService ollamaChatService;

    public ChatController(OllamaChatService ollamaChatService) {
        this.ollamaChatService = ollamaChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String conversationId = StringUtils.hasText(request.conversationId())
                ? request.conversationId()
                : UUID.randomUUID().toString();
        String content = ollamaChatService.chat(request.userMessage());
        return ApiResponse.ok(new ChatResponse(conversationId, content));
    }
}
