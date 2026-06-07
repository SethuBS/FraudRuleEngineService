package com.capitec.fraud.api.dto;

import com.capitec.fraud.application.PageResult;

import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
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
