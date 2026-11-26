package com.miroyiro.backend.domain.admin.artist;

import com.miroyiro.backend.domain.artist.dto.request.ArtistCreateRequest;
import com.miroyiro.backend.domain.artist.dto.request.ArtistUpdateRequest;
import com.miroyiro.backend.domain.artist.dto.response.ArtistCreateResponse;
import com.miroyiro.backend.domain.artist.dto.response.ArtistUpdateResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/artists")
public class AdminArtistController {

    private final AdminArtistService adminArtistService;

    public AdminArtistController(
            AdminArtistService adminArtistService
    ) {
        this.adminArtistService = adminArtistService;
    }

    // 관리자 아티스트 등록
    @PostMapping
    public ResponseEntity<Map<String, ArtistCreateResponse>> create(
            @Valid @RequestBody ArtistCreateRequest request
    ) {
        ArtistCreateResponse response =
                adminArtistService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("data", response));
    }

    // 관리자 아티스트 수정
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, ArtistUpdateResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ArtistUpdateRequest request
    ) {
        ArtistUpdateResponse response =
                adminArtistService.update(id, request);

        return ResponseEntity.ok(
                Map.of("data", response)
        );
    }
}
