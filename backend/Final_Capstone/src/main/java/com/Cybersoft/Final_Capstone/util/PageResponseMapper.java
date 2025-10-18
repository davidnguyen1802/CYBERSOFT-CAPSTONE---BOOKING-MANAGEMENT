package com.Cybersoft.Final_Capstone.util;

import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for converting Spring Data Page to custom PageResponse.
 * Provides reusable methods for pagination response mapping.
 */
public class PageResponseMapper {

    /**
     * Convert Spring Data Page to PageResponse with entity-to-DTO mapping.
     * @param page Spring Data Page object
     * @param mapper Function to convert entity to DTO
     * @param <E> Entity type
     * @param <D> DTO type
     * @return PageResponse with mapped DTOs
     */
    public static <E, D> PageResponse<D> toPageResponse(Page<E> page, Function<E, D> mapper) {
        List<D> content = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    /**
     * Convert Spring Data Page to PageResponse (content already mapped).
     * @param page Spring Data Page with DTOs
     * @param <D> DTO type
     * @return PageResponse
     */
    public static <D> PageResponse<D> toPageResponse(Page<D> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}

