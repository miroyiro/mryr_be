package com.miroyiro.backend.internal;

import com.miroyiro.backend.internal.dto.EventPublishRequest;
import com.miroyiro.backend.internal.dto.IdResponse;
import com.miroyiro.backend.internal.dto.NewsPublishRequest;
import com.miroyiro.backend.internal.service.InternalPipelineService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * scrap → backend : 승인된 항목 게시 (X-Internal-Key 필요, InternalKeyFilter).
 * 새로 만들면 201, 이미 있던 것(재시도·재승인)이면 200. 둘 다 { "id": n } 포함.
 */
@RestController
@RequestMapping("/internal/pipeline")
public class InternalPipelineController {

    private final InternalPipelineService service;

    public InternalPipelineController(InternalPipelineService service) {
        this.service = service;
    }

    @PostMapping("/events")
    public ResponseEntity<IdResponse> publishEvent(@Valid @RequestBody EventPublishRequest body) {
        return respond(service.publishEvent(body));
    }

    @PostMapping("/news")
    public ResponseEntity<IdResponse> publishNews(@Valid @RequestBody NewsPublishRequest body) {
        return respond(service.publishNews(body));
    }

    private static ResponseEntity<IdResponse> respond(IdResponse r) {
        return ResponseEntity.status(r.created() ? HttpStatus.CREATED : HttpStatus.OK).body(r);
    }
}
