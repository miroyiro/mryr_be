package com.miroyiro.backend.domain.artist.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class ArtistUpdateRequest {

    private final Set<String> providedFields = new HashSet<>();

    @Size(max = 100)
    @Pattern(regexp = "(?s).*\\S.*")
    private String name;

    @Size(max = 100)
    private String nameEn;

    @Size(max = 100)
    private String nameJa;

    @Size(max = 500)
    private String profileImage;

    private String description;

    private LocalDate debutDate;

    @Size(max = 500)
    private String officialUrl;

    @JsonSetter(value = "name", nulls = Nulls.FAIL)
    public void setName(String name) {
        this.name = name;
        providedFields.add("name");
    }

    @JsonSetter(value = "name_en", nulls = Nulls.SET)
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
        providedFields.add("name_en");
    }

    @JsonSetter(value = "name_ja", nulls = Nulls.SET)
    public void setNameJa(String nameJa) {
        this.nameJa = nameJa;
        providedFields.add("name_ja");
    }

    @JsonSetter(value = "profile_image", nulls = Nulls.SET)
    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
        providedFields.add("profile_image");
    }

    @JsonSetter(value = "description", nulls = Nulls.SET)
    public void setDescription(String description) {
        this.description = description;
        providedFields.add("description");
    }

    @JsonSetter(value = "debut_date", nulls = Nulls.SET)
    public void setDebutDate(LocalDate debutDate) {
        this.debutDate = debutDate;
        providedFields.add("debut_date");
    }

    @JsonSetter(value = "official_url", nulls = Nulls.SET)
    public void setOfficialUrl(String officialUrl) {
        this.officialUrl = officialUrl;
        providedFields.add("official_url");
    }

    public boolean hasField(String field) {
        return providedFields.contains(field);
    }

    public String name() {
        return name;
    }

    public String nameEn() {
        return nameEn;
    }

    public String nameJa() {
        return nameJa;
    }

    public String profileImage() {
        return profileImage;
    }

    public String description() {
        return description;
    }

    public LocalDate debutDate() {
        return debutDate;
    }

    public String officialUrl() {
        return officialUrl;
    }
}
