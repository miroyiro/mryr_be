package com.miroyiro.backend.domain.artist.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ArtistCreateRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 100)
        String nameEn,

        @Size(max = 100)
        String nameJa,

        @NotBlank
        @Pattern(regexp = "jpop|anime")
        String category,

        @Size(max = 500)
        String profileImage,

        String description,

        LocalDate debutDate,

        @Size(max = 500)
        String officialUrl

) {
}
