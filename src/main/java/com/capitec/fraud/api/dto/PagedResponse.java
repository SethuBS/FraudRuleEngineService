package com.capitec.fraud.api.dto;

import com.capitec.fraud.application.PageResult;

import java.util.List;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated API response.")
public record PagedResponse<T>(
        @Schema(description = "Current page content.")
        List<T> content,
        @Schema(description = "Current zero-based page.")
        int page,
        @Schema(description = "Requested page size.")
        int size,
        @Schema(description = "Total matching records.")
        long totalElements,
        @Schema(description = "Total available pages.")
        int totalPages)
{

    public PagedResponse
    {
        content = List.copyOf(content);
    }

    public static <T, U> PagedResponse<T> from(PageResult<U> pageResult, Function<U, T> mapper)
    {
        return new PagedResponse<>(
                pageResult.content().stream().map(mapper).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages());
    }
}
