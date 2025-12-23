package com.bidket.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상품 서비스 공통 페이징 요청")
public class PageRequestDto {

    @Schema(description = "페이지 번호(0부터 시작)", example = "0")
    private int page = 0;

    @Schema(description = "페이지 크기(최대 100)", example = "20")
    private int size = 20;

    @Schema(description = "정렬 조건(property, direction", example = "createdAt, desc")
    private String sort;

    /** 기본 Pageable 생성 (정렬 없음) */
    public Pageable toPageable() {
        return PageRequest.of(
                getSafePage(),
                getSafeSize()
        );
    }

    /** 기본 정렬을 포함한 Pageable 생성
     *   - sort 파라미터가 있으면 요청 값 우선
     *   - 없으면 defaultSort 사용
     */
    public Pageable toPageable(Sort defaultSort) {

        Sort finalSort = hasSort()
                ? parseSort()
                : defaultSort;

        return PageRequest.of(
                getSafePage(),
                getSafeSize(),
                finalSort
        );
    }

    private int getSafePage() {
        return Math.max(page, 0);
    }

    private int getSafeSize() {
        return (size <= 0 || size > 100) ? 20 : size;
    }

    private boolean hasSort() {
        return StringUtils.hasText(this.sort);
    }

    // sort 문자열을 Sort 객체로 파싱
    private Sort parseSort() {
        // this.sort는 항상 null이 아님 (호출하는 곳에서 확인)
        String[] parts = this.sort.split(",");
        String property = parts[0].trim();

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            try {
                // 대소문자 무시하고 파싱 시도
                direction = Sort.Direction.fromString(parts[1].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // 잘못된 파라미터가 오면 기본값(ASC) 사용
                direction = Sort.Direction.ASC;
            }
        }

        return Sort.by(direction, property);
    }
}
