package com.miroyiro.backend.global.security.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.miroyiro.backend.global.config.MiroyiroProperties;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** DB·스프링 컨텍스트 없이 도는 단위 테스트. */
class InternalKeyFilterTest {

    private static InternalKeyFilter filter(String key) {
        return new InternalKeyFilter(new MiroyiroProperties(
                new MiroyiroProperties.Internal(key), null, null, new MiroyiroProperties.Cors(List.of())));
    }

    private static int run(InternalKeyFilter f, String uri, String header) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        if (header != null) {
            req.addHeader(InternalKeyFilter.HEADER, header);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void rightKeyPasses() throws Exception {
        assertThat(run(filter("k1"), "/internal/pipeline/events", "k1")).isEqualTo(200);
    }

    @Test
    void wrongOrMissingKeyIs401() throws Exception {
        assertThat(run(filter("k1"), "/internal/pipeline/events", "nope")).isEqualTo(401);
        assertThat(run(filter("k1"), "/internal/pipeline/events", null)).isEqualTo(401);
    }

    @Test
    void emptyConfiguredKeyRejectsAll() throws Exception {
        assertThat(run(filter(""), "/internal/catalog/names", "")).isEqualTo(401);
    }

    @Test
    void nonInternalPathIsIgnored() throws Exception {
        assertThat(run(filter("k1"), "/api/health", null)).isEqualTo(200);
    }
}
