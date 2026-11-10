package com.miroyiro.backend.internal;

import java.time.OffsetDateTime;

import com.miroyiro.backend.global.response.ListResponse;
import com.miroyiro.backend.internal.dto.AnimeUpsertRequest;
import com.miroyiro.backend.internal.dto.CatalogName;
import com.miroyiro.backend.internal.dto.CountResponse;
import com.miroyiro.backend.internal.dto.IdResponse;
import com.miroyiro.backend.internal.dto.StreamingUpsertRequest;
import com.miroyiro.backend.internal.service.InternalCatalogService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** scrap → backend : 카탈로그 보강 + 이름 목록 (X-Internal-Key 필요). */
@RestController
@RequestMapping("/internal/catalog")
public class InternalCatalogController {

    private final InternalCatalogService service;

    public InternalCatalogController(InternalCatalogService service) {
        this.service = service;
    }

    /** AniList / 애니시아 결과 upsert → { "id": n } */
    @PostMapping("/anime")
    public ResponseEntity<IdResponse> upsertAnime(@Valid @RequestBody AnimeUpsertRequest body) {
        IdResponse r = service.upsertAnime(body);
        return ResponseEntity.status(r.created() ? HttpStatus.CREATED : HttpStatus.OK).body(r);
    }

    /** 라프텔 · 애니플러스 동시방영 '어디서 보나' upsert → { "count": n } */
    @PostMapping("/streaming")
    public CountResponse upsertStreaming(@Valid @RequestBody StreamingUpsertRequest body) {
        return new CountResponse(service.upsertStreaming(body));
    }

    /** GET /internal/catalog/names?since=2026-11-10T00:00:00+09:00 → { "items": [{kind,id,name}] } */
    @GetMapping("/names")
    public ListResponse<CatalogName> names(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
        return ListResponse.of(service.names(since));
    }
}
