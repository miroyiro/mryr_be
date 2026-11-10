package com.miroyiro.backend.global.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** { "items": [...], "meta": {...} }  — 빈 목록도 200 + [] */
public record ListResponse<T>(List<T> items, PageMeta meta) {

    public static <E, T> ListResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new ListResponse<>(page.getContent().stream().map(mapper).toList(), PageMeta.of(page));
    }

    public static <T> ListResponse<T> of(List<T> items) {
        return new ListResponse<>(items, null);
    }
}
