package com.quill.agent.controller;

import com.quill.agent.dto.ApiResponse;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优雅停机（03-生产规范.md R1）：
 * 仅 127.0.0.1 可访问（server.address 已限定），且经 LocalTokenFilter 校验。
 * Electron 主进程退出时先调本接口，失败再 tree-kill。
 */
@RestController
public class ShutdownController {

    private final ConfigurableApplicationContext context;

    public ShutdownController(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @PostMapping("/api/shutdown")
    public ApiResponse<Void> shutdown() {
        // 异步关闭，避免响应未写出即被销毁
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            context.close();
        }, "shutdown-thread").start();
        return ApiResponse.ok();
    }
}