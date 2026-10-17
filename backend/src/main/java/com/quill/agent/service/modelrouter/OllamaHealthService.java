package com.quill.agent.service.modelrouter;

import com.quill.agent.dto.LocalModelStatusResponse;
import com.quill.agent.dto.LocalModelStatusResponse.ModelSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Ollama 健康探测（v2.0 收敛为 3 状态）：
 * UNAVAILABLE（无法连接）/ MODEL_MISSING（连通但模型未下载）/ READY（就绪）。
 * 仅 HTTP 探测，无法可靠区分"未安装/未启动"，故两者统一归 UNAVAILABLE。
 */
@Service
public class OllamaHealthService {

    private static final Logger log = LoggerFactory.getLogger(OllamaHealthService.class);

    private final RestClient restClient;
    private final String chatModel;

    public OllamaHealthService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.chat-model}") String chatModel) {
        this.chatModel = chatModel;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public LocalModelStatusResponse detect() {
        long ramGB = totalMemoryGB();
        List<ModelSuggestion> suggestions = List.of(
                new ModelSuggestion("qwen2.5:1.5b", 4, "低配笔记本 CPU 运行"),
                new ModelSuggestion("qwen2.5:3b", 8, "推荐平衡，8G 内存以上"),
                new ModelSuggestion("qwen2.5:7b", 16, "16G+ 内存，独立显卡"));

        // ① 服务不可达：统一 UNAVAILABLE（不区分未安装/未启动）
        if (!isOllamaReachable()) {
            return LocalModelStatusResponse.unavailable(chatModel, ramGB, suggestions);
        }
        // ② 服务可达但模型未下载：MODEL_MISSING + pull 命令
        if (!modelListContains(chatModel)) {
            return LocalModelStatusResponse.modelMissing(chatModel, ramGB, suggestions,
                    "ollama pull " + chatModel);
        }
        // ③ 就绪
        return LocalModelStatusResponse.ready(chatModel, ramGB, suggestions);
    }

    private boolean isOllamaReachable() {
        try {
            restClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.debug("Ollama 未响应：{}", ex.getMessage());
            return false;
        }
    }

    private boolean modelListContains(String model) {
        try {
            String body = restClient.get().uri("/api/tags")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().body(String.class);
            if (body == null) return false;
            String normalized = model.replace(":latest", "");
            return body.contains("\"name\":\"" + model + "\"")
                    || body.contains("\"name\":\"" + normalized + "\"");
        } catch (Exception ex) {
            log.warn("请求 Ollama /api/tags 失败：{}", ex.getMessage());
            return false;
        }
    }

    private long totalMemoryGB() {
        var os = (com.sun.management.OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
        return os.getTotalMemorySize() / (1024 * 1024 * 1024);
    }
}