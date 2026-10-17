package com.quill.agent.config;

import com.quill.agent.config.ApiKeyMaskingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyMaskingFilterTest {

    @Test
    void 请求体apiKey被掩码() throws Exception {
        String payload = "{\"apiKey\":\"sk-realkey123\",\"prompt\":\"写一段\"}";
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/ai/continue");
        req.setContent(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ApiKeyMaskingFilter filter = new ApiKeyMaskingFilter();
        filter.doFilter(req, new MockHttpServletResponse(), (s, r) -> {
            ContentCachingRequestWrapper wrapped = (ContentCachingRequestWrapper) s;
            String body = new String(wrapped.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(body).doesNotContain("sk-realkey123");
        });
    }
}