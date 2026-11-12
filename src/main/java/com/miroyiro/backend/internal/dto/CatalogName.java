package com.miroyiro.backend.internal.dto;

/** name_matcher 용 한 줄: 작품/아티스트 하나의 표기 하나. kind = artist | anime */
public record CatalogName(String kind, Long id, String name) {
}
