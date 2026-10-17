package com.quill.agent.controller;

import com.quill.agent.dto.ApiResponse;
import com.quill.agent.dto.LocalModelStatusResponse;
import com.quill.agent.service.modelrouter.OllamaHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 本地模型健康探测，供前端「设置页」置灰 / 展示 FAQ 使用 */
@RestController
public class LocalModelStatusController {

    private final OllamaHealthService healthService;

    public LocalModelStatusController(OllamaHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/api/status/local-model")
    public ApiResponse<LocalModelStatusResponse> status() {
        return ApiResponse.ok(healthService.detect());
    }
}