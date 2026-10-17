package com.quill.agent.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LocalTokenFilter 单元测试：
 * 覆盖免鉴权路径（含尾部斜杠）、回环 IP / Host 校验、令牌校验、响应 401 格式。
 * 构造对象而非装载 Spring 容器，保证纯 Servlet 层行为可测。
 */
class LocalTokenFilterTest {

    private static final String TOKEN = "good-token";

    private LocalTokenService tokenService;
    private LocalTokenFilter filter;

    @BeforeEach
    void setUp() {
        tokenService = mock(LocalTokenService.class);
        when(tokenService.matches(TOKEN)).thenReturn(true);
        when(tokenService.matches("bad-token")).thenReturn(false);
        filter = new LocalTokenFilter(tokenService);
    }

    /** 执行过滤器，返回是否到达业务链路（chain.doFilter 被调用） */
    private boolean run(MockHttpServletRequest req, MockHttpServletResponse resp) throws Exception {
        AtomicBoolean reached = new AtomicBoolean(false);
        FilterChain chain = (request, response) -> reached.set(true);
        filter.doFilter(req, resp, chain);
        return reached.get();
    }

    private MockHttpServletRequest baseRequest(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRemoteAddr("127.0.0.1");
        req.setServerName("localhost");
        return req;
    }

    // ---- 免鉴权路径 ----

    @Test
    void 免鉴权路径不带token放行() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(baseRequest("/api/status/local-model"), resp);
        assertThat(reached).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void 免鉴权路径带尾部斜杠放行() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(baseRequest("/api/system/info/"), resp);
        assertThat(reached).isTrue();
    }

    @Test
    void 免鉴权路径的前缀子路径不误放行_无token拒绝() throws Exception {
        // 精确匹配：/api/status/local-model/xxx 不属于免鉴权，应进入校验
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(baseRequest("/api/status/local-model/extra"), resp);
        assertThat(reached).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    // ---- 回环 IP 校验 ----

    @Test
    void 非回环IP拒绝() throws Exception {
        MockHttpServletRequest req = baseRequest("/api/chat");
        req.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(req, resp);
        assertThat(reached).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    @Test
    void IPv6回环地址放行() throws Exception {
        MockHttpServletRequest req = baseRequest("/api/chat");
        req.setRemoteAddr("0:0:0:0:0:0:0:1");
        req.addHeader("X-Local-Token", TOKEN);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(req, resp);
        assertThat(reached).isTrue();
    }

    @Test
    void null远程地址拒绝() throws Exception {
        MockHttpServletRequest req = baseRequest("/api/chat");
        req.setRemoteAddr(null);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(req, resp);
        assertThat(reached).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    // ---- Host 白名单校验 ----

    @Test
    void 非法Host拒绝() throws Exception {
        MockHttpServletRequest req = baseRequest("/api/chat");
        req.setServerName("evil.com");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(req, resp);
        assertThat(reached).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    @Test
    void 大小写混写Host放行() throws Exception {
        MockHttpServletRequest req = baseRequest("/api/chat");
        req.setServerName("LOCALHOST");
        req.addHeader("X-Local-Token", TOKEN);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(req, resp);
        assertThat(reached).isTrue();
    }

    // ---- 令牌校验 ----

    @Test
    void 合法令牌放行() throws Exception {
        MockHttpServletRequest req = baseRequest("/api/chat");
        req.addHeader("X-Local-Token", TOKEN);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(req, resp);
        assertThat(reached).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void 错误令牌拒绝_返回ERR_AUTH_001() throws Exception {
        MockHttpServletRequest req = baseRequest("/api/chat");
        req.addHeader("X-Local-Token", "bad-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(req, resp);
        assertThat(reached).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
        String body = resp.getContentAsString();
        assertThat(body).contains("ERR-AUTH-001");
        assertThat(body).contains("X-Local-Token");
    }

    @Test
    void 缺少令牌拒绝() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean reached = run(baseRequest("/api/chat"), resp);
        assertThat(reached).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }
}