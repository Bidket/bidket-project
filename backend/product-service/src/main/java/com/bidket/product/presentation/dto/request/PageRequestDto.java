package com.bidket.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상품 서비스 페이징 요청")
public class PageRequestDto {

    @Schema(description = "페이지 번호(0부터 시작)", example = "0")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "20")
    private int size = 20;

    @Schema(description = "정렬 조건", example = "createdAt, desc")
    private String sort;

    public Pageable toPageable() {

        // 정렬 파라미터 없음
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }

        // 정렬 문자열 Sort.Order 리스트로 변환
        Sort sortObj = Sort.by(
                Arrays.stream(sort.split(","))
                        .map(String::trim)
                        .map(orderStr -> {
                            if (orderStr.contains("desc")) {
                                return Sort.Order.desc(orderStr.replace(
                                        ",desc",
                                        "").trim());
                            }

                            if (orderStr.contains("asc")) {
                                return Sort.Order.asc(orderStr.replace(
                                        ",asc",
                                        "").trim());
                            }
                            return Sort.Order.asc(orderStr);
                        })
                        .toList()
        );

        return PageRequest.of(page, size, sortObj);
    }

}
