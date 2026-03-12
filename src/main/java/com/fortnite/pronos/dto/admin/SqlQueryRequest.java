package com.fortnite.pronos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SqlQueryRequest(
    @NotBlank(message = "Query must not be blank")
        @Size(max = 2000, message = "Query must be 2000 characters or fewer")
        String query) {}
