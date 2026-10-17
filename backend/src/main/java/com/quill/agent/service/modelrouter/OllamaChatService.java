package com.quill.agent.service.modelrouter;

import com.quill.agent.exception.BusinessException;
import com.quill.agent.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * LOCAL 模式下的 Ollama 对话调用（阶段0/1 最小实现）。
 * 阶段2 D18 会由 ModelRouter 统一收口 LOCAL / USER_PROVIDED_API 双模式。
 */
@Service
public class OllamaChatService {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatService.class);

    private final ChatClient chatClient;

    public OllamaChatService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String chat(String userMessage) {
        try {
            return chatClient.prompt().user(userMessage).call().content();
        } catch (Exception ex) {
            // Ollama 未启动 / 地址错误 / 模型缺失，统一翻译为业务错误码，不抛原始堆栈
            log.warn("调用本地 Ollama 失败：{}", ex.getMessage());
            throw new BusinessException(ErrorCode.ERR_MODEL_001);
        }
    }
}
