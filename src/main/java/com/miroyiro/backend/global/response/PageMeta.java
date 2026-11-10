package com.miroyiro.backend.global.response;

import org.springframework.data.domain.Page;

/** 목록 meta (06: 목록 = items + meta). page 는 1부터. */
public record PageMeta(int page, int limit, long total, boolean hasNext) {

    public static PageMeta of(Page<?> p) {
        return new PageMeta(p.getNumber() + 1, p.getSize(), p.getTotalElements(), p.hasNext());
    }
}
