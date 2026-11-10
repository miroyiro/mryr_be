package com.miroyiro.backend.internal.dto;

/** { "id": 12, "created": true } — scrap backend_client 는 id 만 읽음. */
public record IdResponse(Long id, boolean created) {
}
