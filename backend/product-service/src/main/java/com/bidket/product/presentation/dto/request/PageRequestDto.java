package com.bidket.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상품 서비스 공통 페이징 요청")
public class PageRequestDto {

    @Schema(description = "페이지 번호(0부터 시작)", example = "0")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "20")
    private int size = 20;

    @Schema(description = "정렬 조건", example = "createdAt, desc")
    private String sort;

    public Pageable toPageable() {

        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > 100) ? 20 : size;

        // 정렬 파라미터 없음
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }

        // 정렬 파라미터 Sort 객체로 파싱
        String[] parts = sort.split(",");
        String property = parts[0].trim();

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException e) {
                direction = Sort.Direction.ASC;
            }
        }

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(direction, property)
        );
    }
}
