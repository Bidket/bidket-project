package com.bidket.auction.application.auction.dto.response;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 경매 응답 DTO
 *
 * [핵심 어노테이션]
 *
 * @JsonTypeInfo (Jackson)
 * - Redis 캐시에서 역직렬화 시 정확한 타입을 복원하기 위해 사용
 * - JSON에 "@class" 필드 추가하여 클래스 정보 포함
 * - 예: {"@class": "...AuctionResponse", "id": "...", ...}
 * - 주의: 클래스명/패키지 변경 시 기존 캐시 데이터 읽기 실패
 *  *                                                                                                             
       17 -   * 1. @JsonTypeInfo (Jackson 라이브러리)                                                                       
       18 -   *    [무엇인지]                                                                                               
       19 -   *    - JSON 직렬화/역직렬화 시 타입 정보를 포함시키는 어노테이션                                              
       20 -   *    - 다형성(Polymorphism)을 지원하여 객체의 실제 클래스 정보를 JSON에 포함                                  
       21 -   *                                                                                                             
       22 -   *    [왜 사용했는지]                                                                                          
       23 -   *    - Redis에 캐싱할 때 역직렬화 시 정확한 타입을 복원하기 위해                                              
       24 -   *    - 예: AuctionResponse를 Redis에 저장 후 꺼낼 때, 정확히 AuctionResponse 타입으로 복원                    
       25 -   *    - 타입 정보가 없으면 LinkedHashMap 같은 일반 타입으로 복원되어 타입 안전성 상실                          
       26 -   *                                                                                                             
       27 -   *    [어디에 사용하는지]                                                                                      
       28 -   *    - Redis 캐시를 사용하는 모든 DTO 클래스                                                                  
       29 -   *    - 상속 구조가 있는 DTO (부모-자식 클래스)                                                                
       30 -   *    - Kafka 메시지로 전송되는 이벤트 객체                                                                    
       31 -   *                                                                                                             
       32 -   *    [파라미터 설명]                                                                                          
       33 -   *    - use = JsonTypeInfo.Id.CLASS                                                                            
       34 -   *      * 클래스의 전체 이름(FQCN: Fully Qualified Class Name)을 타입 정보로 사용                              
       35 -   *      * 예: "com.bidket.auction.application.auction.dto.response.AuctionResponse"                            
       36 -   *      * 대안: Id.NAME (사용자 정의 이름), Id.MINIMAL_CLASS (짧은 클래스명)                                   
       37 -   *                                                                                                             
       38 -   *    - include = JsonTypeInfo.As.PROPERTY                                                                     
       39 -   *      * 타입 정보를 JSON의 일반 필드처럼 포함                                                                
       40 -   *      * 기본 필드명: "@class"                                                                                
       41 -   *      * JSON 예시:                                                                                           
       42 -   *        {                                                                                                    
       43 -   *          "@class": "com.bidket.auction.application.auction.dto.response.AuctionResponse",                   
       44 -   *          "id": "123e4567-e89b-12d3-a456-426614174000",                                                      
       45 -   *          "auctionTitle": "나이키 에어조던 1",                                                               
       46 -   *          ...                                                                                                
       47 -   *        }                                                                                                    
       48 -   *      * 대안: As.WRAPPER_OBJECT (타입을 래퍼로 감쌈), As.WRAPPER_ARRAY (배열로 감쌈)                         
       49 -   *                                                                                                             
       50 -   *    [주의사항]                                                                                               
       51 -   *    - 클래스 이름이 변경되면 기존 캐시 데이터를 읽을 수 없음 (역직렬화 실패)                                 
       52 -   *    - 패키지 구조 변경 시에도 동일한 문제 발생                                                               
       53 -   *    - 운영 환경에서 클래스 리팩토링 시 캐시 무효화 필요                                                      
       54 -   *                                                                                                             
       55 -   * 2. record (Java 14+)                                                                                        
       56 -   *    [무엇인지]                                                                                               
       57 -   *    - 불변(immutable) 데이터를 간결하게 표현하는 Java 키워드                                                 
       58 -   *    - 자동으로 생성자, getter, equals(), hashCode(), toString() 생성                                         
       59 -   *                                                                                                             
       60 -   *    [왜 사용했는지]                                                                                          
       61 -   *    - DTO는 불변 객체여야 스레드 안전성 보장                                                                 
       62 -   *    - Lombok @Value보다 간결하고 Java 표준 문법                                                              
       63 -   *    - 보일러플레이트 코드 제거 (getter 메서드 자동 생성)                                                     
       64 -   *                                                                                                             
       65 -   *    [어디에 사용하는지]                                                                                      
       66 -   *    - 모든 Request/Response DTO                                                                              
       67 -   *    - 도메인 이벤트 객체                                                                                     
       68 -   *    - Value Object (VO)                                                                                      
       69 -   *                                                                                                             
       70 -   *    [주의사항]                                                                                               
       71 -   *    - record는 상속 불가 (final class)                                                                       
       72 -   *    - setter 없음 (불변 객체이므로 수정 불가)                                                                
       73 -   *    - 필드 수정이 필요하면 withXxx() 메서드로 새 객체 생성 (line 95-120)     
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public record AuctionResponse(
        UUID id,
        UUID productSizeId,
        UUID sellerId,
        String auctionTitle,
        String description,
        AuctionCondition condition,
        Long startPrice,
        Long currentPrice,
        Long bidIncrement,
        Long buyNowPrice,
        AuctionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime originalEndTime,
        Integer extensionCount,
        UUID winnerId,
        UUID winningBidId,
        Long finalPrice,
        Integer totalBidsCount,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AuctionResponse from(Auction auction) {
        WinnerInfo winnerInfo = auction.getWinnerInfo() != null ? auction.getWinnerInfo() : WinnerInfo.empty();

        return new AuctionResponse(
                auction.getId(),
                auction.getProductSizeId(),
                auction.getSellerId(),
                auction.getAuctionTitle(),
                auction.getDescription(),
                auction.getCondition(),
                auction.getPriceInfo().getStartPrice(),
                auction.getPriceInfo().getCurrentPrice(),
                auction.getPriceInfo().getBidIncrement(),
                auction.getPriceInfo().getBuyNowPrice(),
                auction.getStatus(),
                auction.getPeriod().getStartTime(),
                auction.getPeriod().getEndTime(),
                auction.getPeriod().getOriginalEndTime(),
                auction.getPeriod().getExtensionCount(),
                winnerInfo.getWinnerId(),
                winnerInfo.getWinningBidId(),
                winnerInfo.getFinalPrice(),
                auction.getStats().getTotalBidsCount(),
                auction.getStats().getViewCount(),
                auction.getCreatedAt(),
                auction.getUpdatedAt()
        );
    }

    public static AuctionResponse fromSummary(Auction auction) {
        WinnerInfo winnerInfo = auction.getWinnerInfo() != null ? auction.getWinnerInfo() : WinnerInfo.empty();
        
        return new AuctionResponse(
                auction.getId(),
                auction.getProductSizeId(),
                auction.getSellerId(),
                auction.getAuctionTitle(),
                null,
                auction.getCondition(),
                auction.getPriceInfo().getStartPrice(),
                auction.getPriceInfo().getCurrentPrice(),
                auction.getPriceInfo().getBidIncrement(),
                auction.getPriceInfo().getBuyNowPrice(),
                auction.getStatus(),
                auction.getPeriod().getStartTime(),
                auction.getPeriod().getEndTime(),
                auction.getPeriod().getOriginalEndTime(),
                auction.getPeriod().getExtensionCount(),
                null,
                null,
                winnerInfo.getFinalPrice(),
                auction.getStats().getTotalBidsCount(),
                auction.getStats().getViewCount(),
                auction.getCreatedAt(),
                auction.getUpdatedAt()
        );
    }

    public AuctionResponse withViewCount(Integer viewCount) {
        return new AuctionResponse(
                this.id,
                this.productSizeId,
                this.sellerId,
                this.auctionTitle,
                this.description,
                this.condition,
                this.startPrice,
                this.currentPrice,
                this.bidIncrement,
                this.buyNowPrice,
                this.status,
                this.startTime,
                this.endTime,
                this.originalEndTime,
                this.extensionCount,
                this.winnerId,
                this.winningBidId,
                this.finalPrice,
                this.totalBidsCount,
                viewCount,
                this.createdAt,
                this.updatedAt
        );
    }
}


