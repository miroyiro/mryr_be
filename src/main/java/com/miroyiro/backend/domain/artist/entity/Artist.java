package com.miroyiro.backend.domain.artist.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** miroyiro_main.artist — 지금은 이름 목록(/internal/catalog/names)에 필요한 컬럼만. 나머지는 팀원(artist 담당)이 추가. */
@Entity
@Table(name = "artist")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "name_ja", length = 100)
    private String nameJa;

    /** jpop | anime */
    @Column(name = "category", length = 10, nullable = false)
    private String category;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Artist() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public String getNameJa() { return nameJa; }
    public String getCategory() { return category; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
