package com.quill.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quill.agent.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LocalTokenFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-Local-Token";
    /** 免鉴权接口（精确 + 尾部斜杠兼容），按需扩展 */
    private static final String[] EXCLUDE = {"/api/status/local-model", "/api/system/info"};

    private final LocalTokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LocalTokenFilter(LocalTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /** 免鉴权判定放 shouldNotFilter，比手写排除更贴合框架语义 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : EXCLUDE) {
            if (uri.equals(prefix) || uri.equals(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // ① 来源 IP 必须为回环（防外部机器直连；自动兼容 IPv4/IPv6）
        if (!isLoopback(request.getRemoteAddr())) {
            reject(response);
            return;
        }
        // ② Host 白名单（防 DNS rebinding；伪造 Host 的外部直连已被 ① 拦截）
        if (!isAllowedHost(request.getServerName())) {
            reject(response);
            return;
        }
        // ③ 业务接口令牌校验
        if (!tokenService.matches(request.getHeader(TOKEN_HEADER))) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isLoopback(String remoteAddr) {
        if (remoteAddr == null) return false;
        try {
            return InetAddress.getByName(remoteAddr).isLoopbackAddress();
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isAllowedHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        return h.equals("localhost") || h.equals("127.0.0.1")
                || h.equals("::1") || h.equals("[::1]")
                || h.equals("0:0:0:0:0:0:0:1");
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.ERR_AUTH_001.httpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "code", ErrorCode.ERR_AUTH_001.code(),
                "message", ErrorCode.ERR_AUTH_001.message(),
                "requestId", UUID.randomUUID().toString())));
    }
}