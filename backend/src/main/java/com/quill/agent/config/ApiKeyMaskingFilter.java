package com.quill.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 全局日志脱敏兜底：对请求体中的 apiKey 做掩码。
 * 仅作最后防线，业务代码仍须遵守"apiKey 不入日志"约束。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyMaskingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyMaskingFilter.class);

    /** 匹配 "apiKey":"<value>"，无论大小写与空格 */
    private static final String MASK_PATTERN = "(?i)(\"apiKey\"\\s*:\\s*\")([^\"]*)(\")";
    private static final String MASK_REPLACEMENT = "$1***$3";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest
                && !"GET".equalsIgnoreCase(httpRequest.getMethod())) {
            ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(httpRequest);
            try {
                chain.doFilter(wrapped, response);
            } finally {
                byte[] body = wrapped.getContentAsByteArray();
                if (body.length > 0) {
                    String json = new String(body, wrapped.getCharacterEncoding() == null
                            ? StandardCharsets.UTF_8 : StandardCharsets.UTF_8);
                    String masked = json.replaceAll(MASK_PATTERN, MASK_REPLACEMENT);
                    String sanitized = masked.replaceAll("\\s+", " ").trim();
                    if (sanitized.length() > 2000) {
                        sanitized = sanitized.substring(0, 2000) + "...(truncated)";
                    }
                    log.info("[REQ] {} {} body={}", httpRequest.getMethod(),
                            httpRequest.getRequestURI(), sanitized);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}