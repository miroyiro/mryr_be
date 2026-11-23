package com.miroyiro.backend.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LogoutResponse(

        @JsonProperty("logged_out")
        boolean loggedOut

) {}
