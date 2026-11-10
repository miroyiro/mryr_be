package com.miroyiro.backend.domain.health;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/health — 배포 확인용 (Docker HEALTHCHECK, 로드밸런서). DB 까지 붙는지 같이 봄. */
@RestController
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        String db;
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            db = "ok";
        } catch (Exception e) {
            db = "down";
        }
        Map<String, Object> body = Map.of(
                "status", "ok".equals(db) ? "ok" : "degraded",
                "db", db,
                "time", OffsetDateTime.now(ZoneId.of("Asia/Seoul")).toString());
        return ResponseEntity.status("ok".equals(db) ? 200 : 503).body(body);
    }
}
