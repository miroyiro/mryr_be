package com.miroyiro.backend.domain.artist.controller;

import com.miroyiro.backend.domain.artist.dto.response.ArtistResponse;
import com.miroyiro.backend.domain.artist.dto.response.ArtistFollowResponse;
import com.miroyiro.backend.domain.artist.dto.response.ArtistFollowCreateResponse;
import com.miroyiro.backend.domain.artist.service.ArtistService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    // 아티스트 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, ArtistResponse>> getArtist(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ArtistResponse response = artistService.getArtist(
                id,
                getUserId(authentication)
        );

        return ResponseEntity.ok(
                Map.of("data", response)
        );
    }

    // 아티스트 팔로우
    @PostMapping("/{id}/follow")
    public ResponseEntity<Map<String, ArtistFollowCreateResponse>> follow(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ArtistService.FollowResult result =
                artistService.follow(
                        id,
                        getUserId(authentication)
                );

        ArtistFollowCreateResponse response =
                new ArtistFollowCreateResponse(
                        id,
                        result.followId(),
                        true
                );

        return ResponseEntity
                .status(result.created() ? 201 : 200)
                .body(Map.of("data", response));
    }

    // 아티스트 언팔로우
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Map<String, ArtistFollowResponse>> unfollow(
            @PathVariable Long id,
            Authentication authentication
    ) {
        artistService.unfollow(
                id,
                getUserId(authentication)
        );

        ArtistFollowResponse response =
                new ArtistFollowResponse(
                        id,
                        false
                );

        return ResponseEntity.ok(
                Map.of("data", response)
        );
    }

    // 인증 정보에서 사용자 ID 추출
    private Long getUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return Long.valueOf(authentication.getName());
    }
}
