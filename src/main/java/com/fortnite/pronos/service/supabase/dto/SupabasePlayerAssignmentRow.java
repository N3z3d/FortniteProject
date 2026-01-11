package com.fortnite.pronos.service.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabasePlayerAssignmentRow(
    String pronostiqueur, String nickname, String region, Integer score, Integer rank) {}
