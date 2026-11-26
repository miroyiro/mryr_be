package com.miroyiro.backend.domain.artist.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "category", length = 10, nullable = false)
    private String category;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "debut_date")
    private LocalDate debutDate;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected Artist() {
    }

    public Artist(
            String name,
            String nameEn,
            String nameJa,
            String category,
            String profileImage,
            String description,
            LocalDate debutDate,
            String officialUrl
    ) {
        this.name = name;
        this.nameEn = nameEn;
        this.nameJa = nameJa;
        this.category = category;
        this.profileImage = profileImage;
        this.description = description;
        this.debutDate = debutDate;
        this.officialUrl = officialUrl;
    }

    // 기존 일괄 수정 메서드 유지
    public void update(
            String name,
            String nameEn,
            String nameJa,
            String profileImage,
            String description,
            LocalDate debutDate,
            String officialUrl
    ) {
        if (name != null) {
            updateName(name);
        }

        if (nameEn != null) {
            updateNameEn(nameEn);
        }

        if (nameJa != null) {
            updateNameJa(nameJa);
        }

        if (profileImage != null) {
            updateProfileImage(profileImage);
        }

        if (description != null) {
            updateDescription(description);
        }

        if (debutDate != null) {
            updateDebutDate(debutDate);
        }

        if (officialUrl != null) {
            updateOfficialUrl(officialUrl);
        }
    }

    // PATCH 개별 필드 수정
    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Artist name must not be blank"
            );
        }

        this.name = name;
    }

    public void updateNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public void updateNameJa(String nameJa) {
        this.nameJa = nameJa;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateDebutDate(LocalDate debutDate) {
        this.debutDate = debutDate;
    }

    public void updateOfficialUrl(String officialUrl) {
        this.officialUrl = officialUrl;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameJa() {
        return nameJa;
    }

    public String getCategory() {
        return category;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDebutDate() {
        return debutDate;
    }

    public String getOfficialUrl() {
        return officialUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
