package com.bidket.auction.application.auction.dto.request;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 경매 생성 요청 DTO
 *
 * [어노테이션 설명]
 *
 * 이 클래스는 Bean Validation API (Jakarta Validation)를 사용하여 입력값을 검증합니다.
 * 검증은 Controller에서 @Valid 또는 @Validated 어노테이션을 통해 자동으로 수행됩니다.
 *
 * 1. @NotNull (jakarta.validation.constraints.NotNull)
 *    [무엇인지]
 *    - 필드 값이 null이 아니어야 함을 검증하는 어노테이션
 *    - 빈 문자열("")이나 빈 컬렉션은 허용 (null만 체크)
 *
 *    [왜 사용했는지]
 *    - 필수 입력값을 명시적으로 표현
 *    - null로 인한 NullPointerException 사전 방지
 *    - API 문서 자동 생성 시 "required: true" 표시
 *
 *    [어디에 사용하는지]
 *    - 필수 입력 필드 (productSizeId, sellerId, startPrice 등)
 *    - 객체 타입 필드 (UUID, Enum, LocalDateTime 등)
 *    - 기본값이 없는 모든 필수 필드
 *
 *    [검증 시점]
 *    - Controller 메서드 실행 전 (Spring MVC가 자동 검증)
 *    - 검증 실패 시 HTTP 400 Bad Request 응답
 *    - message 속성의 메시지가 에러 응답에 포함됨
 *
 * 2. @NotBlank (jakarta.validation.constraints.NotBlank)
 *    [무엇인지]
 *    - 문자열이 null도 아니고, 빈 문자열("")도 아니고, 공백만 있지도 않아야 함
 *    - @NotNull + @Size(min=1) + trim() 검증을 합친 것
 *
 *    [왜 사용했는지]
 *    - 사용자가 공백만 입력하는 것을 방지
 *    - 예: "   " (공백 3개)는 NotNull은 통과하지만 NotBlank는 실패
 *    - 의미 있는 데이터만 DB에 저장하기 위해
 *
 *    [어디에 사용하는지]
 *    - String 타입의 필수 입력 필드
 *    - 제목, 이름, 설명 등 텍스트 입력 필드
 *
 *    [NotNull vs NotBlank 차이]
 *    - @NotNull: null만 체크, ""는 허용
 *    - @NotBlank: null, "", "  " 모두 불허
 *    - String 필드는 대부분 @NotBlank를 사용해야 함
 *
 * 3. @Size (jakarta.validation.constraints.Size)
 *    [무엇인지]
 *    - 문자열 길이 또는 컬렉션 크기를 검증하는 어노테이션
 *    - min, max 속성으로 최소/최대 크기 지정
 *
 *    [왜 사용했는지]
 *    - DB 컬럼 길이 제약 사전 검증 (예: VARCHAR(200))
 *    - 너무 짧거나 긴 입력 방지 (UX 개선)
 *    - 예: 제목이 1글자면 의미 없고, 1000글자면 UI가 깨짐
 *
 *    [어디에 사용하는지]
 *    - 문자열 필드: auctionTitle (5~200자), description (0~2000자)
 *    - 컬렉션 필드: List, Set, Map 등의 요소 개수 제한
 *
 *    [파라미터]
 *    - min: 최소 길이 (기본값 0)
 *    - max: 최대 길이 (기본값 Integer.MAX_VALUE)
 *    - message: 검증 실패 시 메시지
 *
 * 4. @Min (jakarta.validation.constraints.Min)
 *    [무엇인지]
 *    - 숫자 값이 지정된 최솟값 이상이어야 함을 검증
 *    - Long, Integer, BigDecimal 등 숫자 타입에 사용
 *
 *    [왜 사용했는지]
 *    - 비즈니스 규칙 강제 (예: 시작가 최소 10,000원)
 *    - 음수나 0원 입력 방지
 *    - DB에 무효한 데이터 저장 방지
 *
 *    [어디에 사용하는지]
 *    - 금액 필드: startPrice (최소 10,000원), bidIncrement (최소 1,000원)
 *    - 수량, 개수 등 양수만 허용되는 필드
 *
 *    [대안]
 *    - @Max: 최댓값 검증
 *    - @Positive: 0보다 큰 값만 허용
 *    - @PositiveOrZero: 0 이상만 허용
 *    - @Range: min, max를 동시에 지정
 *
 * 5. @Future (jakarta.validation.constraints.Future)
 *    [무엇인지]
 *    - 날짜/시간 값이 현재보다 미래여야 함을 검증하는 어노테이션
 *    - LocalDateTime, LocalDate, Instant, Date 등 시간 타입에 사용
 *
 *    [왜 사용했는지]
 *    - 경매 시작 시간은 반드시 미래여야 함 (과거 시간으로 경매 시작 불가)
 *    - 비즈니스 로직 검증을 선언적으로 표현
 *    - 잘못된 시간 입력으로 인한 스케줄링 오류 방지
 *
 *    [어디에 사용하는지]
 *    - 예약/예정 시간 필드 (경매 시작 시간, 이벤트 시작 시간 등)
 *    - 만료 시간 필드
 *
 *    [검증 시점]
 *    - Controller 메서드 실행 시점의 현재 시간과 비교
 *    - 예: 2025-01-01 10:00에 요청하면, startTime은 2025-01-01 10:01 이후여야 함
 *
 *    [대안]
 *    - @Past: 과거 시간만 허용 (예: 생년월일)
 *    - @FutureOrPresent: 현재 또는 미래 허용
 *    - @PastOrPresent: 현재 또는 과거 허용
 *
 *    [주의사항]
 *    - 서버 시간 기준으로 검증되므로 클라이언트-서버 시간 차이 고려 필요
 *    - 타임존 설정이 중요 (UTC vs KST)
 *
 * [검증 어노테이션 실행 흐름]
 *
 * 1. 클라이언트가 POST /api/auctions 요청
 * 2. Spring MVC가 JSON을 CreateAuctionRequest로 역직렬화
 * 3. @Valid 또는 @Validated 어노테이션이 있으면 자동 검증
 * 4. 검증 실패 시:
 *    - MethodArgumentNotValidException 발생
 *    - GlobalExceptionHandler가 잡아서 400 Bad Request 응답
 *    - 응답 body에 검증 실패 메시지 포함
 * 5. 검증 성공 시:
 *    - Controller 메서드 실행
 *    - Service 계층으로 전달
 *
 * [사용 예시]
 *
 * // Controller에서 사용
 * @PostMapping
 * public ResponseEntity<AuctionResponse> createAuction(
 *     @Valid @RequestBody CreateAuctionRequest request) {
 *     // 여기 도달했다면 모든 검증이 통과한 상태
 * }
 *
 * // 검증 실패 시 응답 예시
 * {
 *   "timestamp": "2025-12-19T10:00:00",
 *   "status": 400,
 *   "errors": [
 *     "시작가는 최소 10,000원 이상이어야 합니다",
 *     "시작 시간은 현재 시간 이후여야 합니다"
 *   ]
 * }
 */
public record CreateAuctionRequest(
        @NotNull(message = "상품 사이즈 ID는 필수입니다")
        UUID productSizeId,

        @NotNull(message = "판매자 ID는 필수입니다")
        UUID sellerId,

        @NotBlank(message = "경매 제목은 필수입니다")
        @Size(min = 5, max = 200, message = "경매 제목은 5자 이상 200자 이하여야 합니다")
        String auctionTitle,

        @Size(max = 2000, message = "설명은 최대 2000자까지 입력 가능합니다")
        String description,

        @NotNull(message = "상품 상태는 필수입니다")
        AuctionCondition condition,

        @NotNull(message = "시작가는 필수입니다")
        @Min(value = 10000, message = "시작가는 최소 10,000원 이상이어야 합니다")
        Long startPrice,

        @Min(value = 1000, message = "입찰 단위는 최소 1,000원 이상이어야 합니다")
        Long bidIncrement,

        Long buyNowPrice,

        @NotNull(message = "시작 시간은 필수입니다")
        @Future(message = "시작 시간은 현재 시간 이후여야 합니다")
        LocalDateTime startTime,

        @NotNull(message = "종료 시간은 필수입니다")
        LocalDateTime endTime
) {
    public Auction toEntity() {
        return Auction.builder()
                .productSizeId(productSizeId)
                .sellerId(sellerId)
                .auctionTitle(auctionTitle)
                .description(description)
                .condition(condition)
                .startPrice(startPrice)
                .bidIncrement(bidIncrement)
                .buyNowPrice(buyNowPrice)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}


