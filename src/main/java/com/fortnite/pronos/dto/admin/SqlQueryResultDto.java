package com.fortnite.pronos.dto.admin;

import java.util.List;
import java.util.Map;

public record SqlQueryResultDto(
    List<String> columns, List<Map<String, Object>> rows, int totalRows, boolean truncated) {}
