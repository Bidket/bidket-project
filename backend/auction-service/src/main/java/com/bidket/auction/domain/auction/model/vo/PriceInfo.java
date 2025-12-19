package com.bidket.auction.domain.auction.model.vo;

import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.global.exception.BidDomainException;
import com.bidket.auction.global.exception.BidErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경매 가격 정보 Value Object
 *
 * [어노테이션 설명]
 *
 * 1. @Embeddable (jakarta.persistence.Embeddable)
 *    [무엇인지]
 *    - JPA에서 다른 엔티티에 포함(embed)될 수 있는 값 객체(Value Object)를 표시하는 어노테이션
 *    - 독립적인 테이블을 가지지 않고, 부모 엔티티의 테이블에 컬럼으로 포함됨
 *
 *    [왜 사용했는지]
 *    - 도메인 주도 설계(DDD)의 Value Object 패턴 구현
 *    - 응집력 있는 데이터를 하나의 개념으로 묶어 관리
 *    - 예: startPrice, currentPrice, bidIncrement, buyNowPrice는 모두 "가격 정보"라는 하나의 개념
 *    - 관련 데이터를 객체로 캡슐화하여 비즈니스 로직(validate, withUpdatedCurrentPrice)을 함께 관리
 *
 *    [어디에 사용하는지]
 *    - Auction 엔티티에서 @Embedded로 포함됨
 *      @Embedded
 *      private PriceInfo priceInfo;
 *    - DB에는 별도 테이블 없이 auction 테이블에 컬럼으로 저장됨
 *      auction 테이블: id, start_price, current_price, bid_increment, buy_now_price, ...
 *
 *    [Value Object 패턴의 장점]
 *    - 관심사 분리: Auction 클래스가 가격 관련 로직을 모두 알 필요 없음
 *    - 재사용성: 다른 엔티티(예: BidHistory)에서도 동일한 PriceInfo 사용 가능
 *    - 테스트 용이성: PriceInfo만 독립적으로 테스트 가능
 *    - 불변성 보장: withXxx() 메서드로 새 객체를 생성하여 기존 객체는 변경 안 됨
 *
 * 2. @Column (jakarta.persistence.Column)
 *    [무엇인지]
 *    - JPA 엔티티의 필드를 DB 테이블의 컬럼에 매핑하는 어노테이션
 *
 *    [왜 사용했는지]
 *    - 필드명과 컬럼명이 다를 때 명시적 매핑 (startPrice → start_price)
 *    - nullable, unique, length 등 컬럼 제약 조건 정의
 *
 *    [파라미터 설명]
 *    - name: DB 컬럼명 (Java는 camelCase, DB는 snake_case 컨벤션)
 *      * startPrice → start_price
 *      * currentPrice → current_price
 *
 *    - nullable: NOT NULL 제약 조건
 *      * nullable = false: 필수 값 (NULL 허용 안 함)
 *      * nullable = true (기본값): 선택적 값 (NULL 허용)
 *      * buyNowPrice는 즉시 구매가가 없을 수 있으므로 nullable (기본값)
 *
 *    [주의사항]
 *    - @Column을 생략하면 필드명을 그대로 컬럼명으로 사용
 *    - Spring Boot의 spring.jpa.hibernate.naming.physical-strategy 설정에 따라 자동 변환
 *      * SpringPhysicalNamingStrategy: camelCase → snake_case 자동 변환
 *      * 명시적으로 name을 지정하면 더 명확하고 안전함
 *
 * 3. @Getter (Lombok)
 *    [무엇인지]
 *    - 모든 필드에 대해 getter 메서드를 자동 생성
 *    - 예: getStartPrice(), getCurrentPrice(), getBidIncrement(), getBuyNowPrice()
 *
 *    [왜 사용했는지]
 *    - 보일러플레이트 코드 제거 (각 필드마다 getter 작성 불필요)
 *    - Value Object는 불변 객체이므로 getter만 있고 setter는 없음
 *
 * 4. @NoArgsConstructor(access = AccessLevel.PROTECTED) (Lombok)
 *    [무엇인지]
 *    - 파라미터가 없는 기본 생성자를 자동 생성
 *    - PROTECTED 접근 제어자로 생성
 *
 *    [왜 사용했는지]
 *    - JPA/Hibernate 요구사항: 엔티티와 @Embeddable 객체는 기본 생성자가 필요
 *      * JPA가 DB에서 데이터를 읽어올 때 리플렉션으로 기본 생성자 호출
 *    - PROTECTED 이유:
 *      * PUBLIC으로 하면 외부에서 new PriceInfo() 호출 가능 → 불완전한 객체 생성 위험
 *      * PROTECTED로 하면 JPA는 접근 가능하지만, 도메인 외부에서는 사용 불가
 *      * 객체 생성은 반드시 Builder나 정적 팩토리 메서드(createDefault) 사용 강제
 *
 * 5. @AllArgsConstructor (Lombok)
 *    [무엇인지]
 *    - 모든 필드를 파라미터로 받는 생성자 자동 생성
 *    - private PriceInfo(Long startPrice, Long currentPrice, Long bidIncrement, Long buyNowPrice)
 *
 *    [왜 사용했는지]
 *    - @Builder와 함께 사용하여 Builder 패턴 구현
 *    - 외부에서 직접 호출하지 않고, Builder가 내부적으로 사용
 *
 * 6. @Builder (Lombok)
 *    [무엇인지]
 *    - Builder 패턴을 자동 생성하는 어노테이션
 *    - 유창한 인터페이스(Fluent Interface)로 객체 생성
 *
 *    [왜 사용했는지]
 *    - 가독성: 어떤 값이 어떤 필드에 할당되는지 명확
 *      PriceInfo.builder()
 *          .startPrice(100000L)
 *          .currentPrice(100000L)
 *          .bidIncrement(10000L)
 *          .buyNowPrice(500000L)
 *          .build();
 *
 *    - 불변성: 생성 후 수정 불가 (setter 없음)
 *    - 선택적 파라미터: 일부 필드만 설정 가능 (buyNowPrice는 선택적)
 *
 *    [사용 예시]
 *    - createDefault(): 초기 가격 정보 생성 (currentPrice = startPrice)
 *    - withUpdatedCurrentPrice(): 입찰로 currentPrice 업데이트 (새 객체 반환)
 *    - withBuyNowPrice(): 즉시 구매가 설정 (새 객체 반환)
 *
 * [Value Object의 불변성 패턴]
 *
 * PriceInfo는 불변 객체(Immutable Object)입니다:
 * - setter 메서드 없음
 * - 모든 필드가 private final (Lombok이 자동으로 final 처리)
 * - 수정이 필요하면 새 객체를 생성하여 반환 (withXxx 메서드)
 *
 * 예시:
 * PriceInfo original = PriceInfo.createDefault(100000L, 10000L, 500000L);
 * PriceInfo updated = original.withUpdatedCurrentPrice(110000L);
 * // original은 그대로 유지, updated는 새 객체
 *
 * 불변성의 장점:
 * - 스레드 안전성 (Thread-Safe)
 * - 예측 가능성 (객체 상태가 변하지 않음)
 * - 버그 방지 (의도치 않은 수정 불가)
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PriceInfo {

    @Column(nullable = false, name = "start_price")
    private Long startPrice;

    @Column(nullable = false, name = "current_price")
    private Long currentPrice;

    @Column(nullable = false, name = "bid_increment")
    private Long bidIncrement;

    @Column(name = "buy_now_price")
    private Long buyNowPrice;

    public static PriceInfo createDefault(Long startPrice, Long bidIncrement, Long buyNowPrice) {
        return PriceInfo.builder()
                .startPrice(startPrice)
                .currentPrice(startPrice)
                .bidIncrement(bidIncrement != null ? bidIncrement : 10000L)
                .buyNowPrice(buyNowPrice)
                .build();
    }

    public void validate() {
        if (startPrice == null || startPrice <= 0) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_START_PRICE);
        }
        if (bidIncrement == null || bidIncrement <= 0) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_BID_INCREMENT);
        }
        if (buyNowPrice != null && buyNowPrice <= startPrice) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_BUY_NOW_PRICE);
        }
    }

    public PriceInfo withUpdatedCurrentPrice(Long newPrice) {
        if (newPrice <= this.currentPrice) {
            throw new BidDomainException(BidErrorCode.BID_AMOUNT_TOO_LOW);
        }
        return PriceInfo.builder()
                .startPrice(this.startPrice)
                .currentPrice(newPrice)
                .bidIncrement(this.bidIncrement)
                .buyNowPrice(this.buyNowPrice)
                .build();
    }

    public PriceInfo withBuyNowPrice(Long buyNowPrice) {
        return PriceInfo.builder()
                .startPrice(this.startPrice)
                .currentPrice(this.currentPrice)
                .bidIncrement(this.bidIncrement)
                .buyNowPrice(buyNowPrice)
                .build();
    }
}


