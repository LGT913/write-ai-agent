package com.quill.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 本机接口令牌（03-生产规范.md §2.5）：
 * 启动时生成一次性随机 token，写入应用数据目录 .local-token 文件，
 * 由 Electron 主进程读取经 preload 注入渲染进程；token 绝不通过 HTTP 下发。
 */
@Service
public class LocalTokenService {

    private static final Logger log = LoggerFactory.getLogger(LocalTokenService.class);

    private final String dataDir;
    private volatile String token;

    public LocalTokenService(@Value("${writer.data-dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    @PostConstruct
    public void init() {
        try {
            Path dir = Paths.get(dataDir);
            Files.createDirectories(dir);
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            token = hex(bytes);
            Path file = dir.resolve(".local-token");
            Files.write(file, token.getBytes(StandardCharsets.UTF_8));
            log.info("已生成本机令牌文件：{}", file);
        } catch (Exception ex) {
            throw new IllegalStateException("生成本机令牌失败", ex);
        }
    }

    /** 常量时间比较，防时序侧信道 */
    public boolean matches(String provided) {
        if (provided == null || token == null) return false;
        return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}