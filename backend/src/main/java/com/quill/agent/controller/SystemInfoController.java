package com.quill.agent.controller;

import com.quill.agent.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 系统信息：版本 + 数据目录；不返回任何密钥（02-接口文档 §2 免鉴权接口） */
@RestController
public class SystemInfoController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${writer.data-dir}")
    private String dataDir;

    @GetMapping("/api/system/info")
    public ApiResponse<Map<String, String>> info() {
        return ApiResponse.ok(Map.of(
                "app", appName,
                "version", "1.0.0-SNAPSHOT",
                "dataDir", dataDir));
    }
}