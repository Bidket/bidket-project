package com.bidket.auction.application.bid.service;

import com.bidket.auction.application.bid.dto.request.CreateBidRequest;
import com.bidket.auction.application.bid.dto.response.BidListResponse;
import com.bidket.auction.application.bid.dto.response.BidResponse;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.global.exception.BidDomainException;
import com.bidket.auction.global.exception.BidErrorCode;
import com.bidket.auction.infrastructure.notification.NotificationEventProducer;
import com.bidket.auction.infrastructure.retry.RetryOnOptimisticLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * [Senior's Guide: 역할]
 * 이 클래스는 사용자의 입찰(Bid) 행위를 처리하는 핵심 서비스입니다.
 * [입찰 등록 → 최고가 갱신 → 이전 입찰자 알림 → 경매 가격 업데이트] 흐름을 조율합니다.
 *
 * [도메인 흐름에서의 위치]
 * 경매 라이프사이클의 [진행 단계]에 속하며, 사용자가 경매에 참여하는 모든 행위를 담당합니다.
 * - 입찰 등록: 사용자가 금액을 제시 → 최고가 검증 → Bid 엔티티 생성
 * - 즉시 구매: 사용자가 Buy Now 클릭 → 경매 즉시 종료 → AuctionEndSaga 트리거
 * - 입찰 취소: 밀린 입찰(OUTBID)을 사용자가 취소
 *
 * [왜 이렇게 설계했을까?]
 *
 * 1. @Transactional(readOnly = true) (클래스 레벨)
 *    - 대부분의 메서드가 조회(getXxx)이므로, 기본값을 readOnly=true로 설정했습니다
 *    - 조회 전용 트랜잭션은 Flush를 하지 않아 성능이 향상됩니다
 *    - 쓰기가 필요한 메서드(placeBid, buyNow 등)만 @Transactional을 오버라이드합니다
 *
 * 2. @RetryOnOptimisticLock
 *    - placeBid(), buyNow() 메서드에 적용되어 동시성 충돌 시 자동으로 재시도합니다
 *    - 예: User A와 User B가 동시에 입찰 → Auction.version 충돌 → OptimisticLockException → 자동 재시도
 *    - AOP 방식으로 구현되어 비즈니스 로직에 재시도 코드가 섞이지 않습니다
 *    - 최대 3회 재시도, 실패 시 BidDomainException(CONCURRENT_BID_CONFLICT) 발생
 *
 * 3. NotificationEventProducer 의존성
 *    - 입찰이 밀렸을 때(OUTBID) 이전 최고 입찰자에게 실시간 알림을 보냅니다
 *    - Kafka를 통해 비동기로 발행하여, 알림 전송 실패가 입찰 트랜잭션에 영향을 주지 않습니다
 *    - "내 입찰이 밀렸습니다!" 알림으로 사용자 경험(UX)을 향상시킵니다
 *
 * [핵심 메서드 설명]
 *
 * 1. placeBid(auctionId, bidderId, amount)
 *    - 입찰의 모든 검증과 처리를 담당하는 핵심 메서드입니다
 *    - 검증 순서:
 *      1) 경매 존재 확인
 *      2) 경매가 ACTIVE 상태인지 확인
 *      3) 판매자 본인 입찰 차단 (SELF_BID_NOT_ALLOWED)
 *      4) 최소 입찰가 검증 (currentPrice + bidIncrement 이상)
 *    - 처리 흐름:
 *      1) 이전 최고가 입찰을 OUTBID 상태로 변경 (markAsOutbid)
 *      2) 이전 입찰자에게 알림 발행 (publishOutbidNotification)
 *      3) 새 입찰 생성 및 ACTIVE 상태로 설정 (markAsHighest)
 *      4) 경매의 currentPrice 업데이트
 *
 * 2. buyNow(auctionId, bidderId)
 *    - 즉시 구매 기능: 사용자가 buyNowPrice로 경매를 즉시 낙찰받습니다
 *    - 처리 흐름:
 *      1) 경매의 buyNowPrice 확인 (null이면 예외)
 *      2) currentPrice < buyNowPrice 검증 (이미 초과했으면 사용 불가)
 *      3) buyNowPrice로 입찰 생성 (isHighest=true)
 *      4) 경매 즉시 종료 (auction.end(true))
 *    - 주의: 경매가 SUCCESS 상태가 되면 AuctionEndSaga가 트리거됩니다
 *
 * 3. createBid(bidderId, request, queueVerified)
 *    - **Queue 검증**이 포함된 입찰 등록 메서드입니다 (Gateway 연동)
 *    - queueVerified=false면 QUEUE_ACCESS_DENIED 예외 발생 (Gateway 우회 접근 차단)
 *    - queueVerified=true면 placeBid() 호출하여 정상 처리
 *    - Gateway가 Queue-Service와 통신하여 사용자가 대기열을 통과했는지 검증합니다
 *
 * 4. cancelBid(bidId, userId)
 *    - 사용자가 자신의 입찰을 취소하는 메서드입니다
 *    - 조건:
 *      1) 본인의 입찰이어야 함 (NOT_BID_OWNER)
 *      2) 최고가 입찰이 아니어야 함 (CANNOT_CANCEL_HIGHEST_BID)
 *    - 최고가 입찰은 취소할 수 없는 이유: 경매 진행에 혼란을 주기 때문
 *
 * [어디로 이어지나요?]
 * - placeBid() → Bid 생성 → (경매 종료 시) AuctionScheduler가 AuctionEndSaga 시작
 * - buyNow() → 경매 즉시 종료 → AuctionEndSaga 시작 → 주문 생성 (Order Service)
 * - publishOutbidNotification() → Kafka → Notification Service → 사용자에게 푸시 알림
 *
 * [신입 개발자 주의사항]
 * - placeBid()는 Optimistic Lock으로 동시성을 제어하므로, 재시도가 발생할 수 있습니다
 * - 입찰 금액 검증은 도메인 로직이므로, 컨트롤러에서 중복 검증하지 마세요
 * - 이전 최고가 입찰을 OUTBID로 변경하는 것을 잊으면 안 됩니다 (경매당 최고가는 1개만)
 * - 알림 발행 실패는 입찰 성공에 영향을 주지 않습니다 (try-catch로 격리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final NotificationEventProducer notificationEventProducer;

    @Transactional
    @RetryOnOptimisticLock
    public Bid placeBid(UUID auctionId, UUID bidderId, Long amount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_ACTIVE);
        }

        if (auction.getSellerId().equals(bidderId)) {
            throw new BidDomainException(BidErrorCode.SELF_BID_NOT_ALLOWED);
        }

        Long minimumBid = auction.getPriceInfo().getCurrentPrice() + auction.getPriceInfo().getBidIncrement();
        if (amount < minimumBid) {
            throw new BidDomainException(BidErrorCode.BID_AMOUNT_TOO_LOW);
        }

        Optional<Bid> previousHighestBid = bidRepository.findHighestBidByAuctionId(auctionId);
        if (previousHighestBid.isPresent()) {
            Bid prevBid = previousHighestBid.get();
            prevBid.markAsOutbid();
            bidRepository.save(prevBid);

            // 이전 최고 입찰자에게 상회 입찰 알림 발행
            notificationEventProducer.publishOutbidNotification(
                    prevBid.getBidderId(),
                    auctionId,
                    amount,
                    LocalDateTime.now(),
                    auctionId
            );
            log.info("상회 입찰 알림 발행: previousBidderId={}, auctionId={}, newAmount={}",
                    prevBid.getBidderId(), auctionId, amount);
        }

        Bid newBid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(amount)
                .build();
        newBid.markAsHighest();
        
        Bid savedBid = bidRepository.save(newBid);

        auction.updateCurrentPrice(amount);
        auctionRepository.save(auction);

        log.info("입찰 등록 완료 - 경매 ID: {}, 입찰자: {}, 금액: {}", auctionId, bidderId, amount);
        
        return savedBid;
    }

    @Transactional
    public BidResponse createBid(UUID bidderId, CreateBidRequest request) {
        Bid bid = placeBid(request.auctionId(), bidderId, request.amount());
        return BidResponse.from(bid);
    }

    /**
     * Gateway에서 Queue 검증 완료 후 입찰 생성
     * Gateway가 Queue-Service와 연동하여 검증 후 X-Queue-Verified 헤더 전달
     *
     * @param bidderId 입찰자 ID
     * @param request 입찰 요청 데이터
     * @param queueVerified Gateway에서 Queue 검증 완료 여부 (true/false)
     * @return 생성된 입찰 응답
     * @throws BidDomainException Queue 검증 실패 시 (Gateway 우회 접근 차단)
     */
    @Transactional
    public BidResponse createBid(UUID bidderId, CreateBidRequest request, boolean queueVerified) {
        log.info("[BidService] 입찰 생성: bidderId={}, auctionId={}, queueVerified={}",
                bidderId, request.auctionId(), queueVerified);

        // Gateway에서 Queue 검증을 통과하지 못한 요청 차단
        if (!queueVerified) {
            log.warn("[BidService] Queue 검증 실패 (Gateway 우회 접근): bidderId={}, auctionId={}",
                    bidderId, request.auctionId());
            throw new BidDomainException(BidErrorCode.QUEUE_ACCESS_DENIED);
        }

        // 입찰 처리
        Bid bid = placeBid(request.auctionId(), bidderId, request.amount());

        log.info("[BidService] 입찰 완료: bidId={}, auctionId={}, amount={}",
                bid.getId(), request.auctionId(), request.amount());

        return BidResponse.from(bid);
    }

    public BidListResponse getBidsByAuction(UUID auctionId) {
        List<Bid> bids = bidRepository.findByAuctionId(auctionId);
        List<BidResponse> bidResponses = bids.stream()
                .map(BidResponse::from)
                .toList();
        
        return BidListResponse.of(bidResponses, bids.size());
    }

    public BidListResponse getMyBids(UUID bidderId) {
        List<Bid> bids = bidRepository.findByBidderId(bidderId);
        List<BidResponse> bidResponses = bids.stream()
                .map(BidResponse::from)
                .toList();
        
        return BidListResponse.of(bidResponses, bids.size());
    }

    public BidResponse getBidById(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidDomainException(BidErrorCode.BID_NOT_FOUND));
        return BidResponse.from(bid);
    }

    @Transactional
    public void cancelBid(UUID bidId, UUID bidderId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BidDomainException(BidErrorCode.BID_NOT_FOUND));

        if (!bid.getBidderId().equals(bidderId)) {
            throw new BidDomainException(BidErrorCode.NOT_BID_OWNER);
        }

        if (bid.isHighest()) {
            throw new BidDomainException(BidErrorCode.CANNOT_CANCEL_HIGHEST_BID);
        }
        bid.cancel();
        bidRepository.save(bid);

        log.info("입찰 취소 완료 - 입찰 ID: {}, 입찰자: {}", bidId, bidderId);
    }

    @Transactional
    @RetryOnOptimisticLock
    public Bid buyNow(UUID auctionId, UUID bidderId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_ACTIVE);
        }

        if (auction.getSellerId().equals(bidderId)) {
            throw new BidDomainException(BidErrorCode.SELF_BID_NOT_ALLOWED);
        }

        Long buyNowPrice = auction.getPriceInfo().getBuyNowPrice();
        if (buyNowPrice == null) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_BUY_NOW_PRICE);
        }

        Long currentPrice = auction.getPriceInfo().getCurrentPrice();
        if (currentPrice >= buyNowPrice) {
            throw new AuctionDomainException(AuctionErrorCode.BUY_NOW_NOT_AVAILABLE);
        }

        Optional<Bid> previousHighestBid = bidRepository.findHighestBidByAuctionId(auctionId);
        if (previousHighestBid.isPresent()) {
            Bid prevBid = previousHighestBid.get();
            prevBid.markAsOutbid();
            bidRepository.save(prevBid);

            // 이전 최고 입찰자에게 상회 입찰 알림 발행 (즉시 구매로 인한 패찰)
            notificationEventProducer.publishOutbidNotification(
                    prevBid.getBidderId(),
                    auctionId,
                    buyNowPrice,
                    LocalDateTime.now(),
                    auctionId
            );
            log.info("즉시 구매로 인한 상회 입찰 알림 발행: previousBidderId={}, auctionId={}, buyNowPrice={}",
                    prevBid.getBidderId(), auctionId, buyNowPrice);
        }

        Bid buyNowBid = Bid.builder()
                .auctionId(auctionId)
                .bidderId(bidderId)
                .amount(buyNowPrice)
                .build();
        buyNowBid.markAsHighest();

        Bid savedBid = bidRepository.save(buyNowBid);

        auction.updateCurrentPrice(buyNowPrice);
        auction.end(true);
        auctionRepository.save(auction);

        log.info("즉시 구매 완료 - 경매 ID: {}, 구매자: {}, 금액: {}", auctionId, bidderId, buyNowPrice);

        return savedBid;
    }

    /**
     * Gateway에서 Queue 검증 완료 후 즉시 구매
     * Gateway가 Queue-Service와 연동하여 검증 후 X-Queue-Verified 헤더 전달
     *
     * @param auctionId 경매 ID
     * @param bidderId 구매자 ID
     * @param queueVerified Gateway에서 Queue 검증 완료 여부 (true/false)
     * @return 생성된 입찰 (즉시 구매)
     * @throws BidDomainException Queue 검증 실패 시 (Gateway 우회 접근 차단)
     */
    @Transactional
    @RetryOnOptimisticLock
    public Bid buyNow(UUID auctionId, UUID bidderId, boolean queueVerified) {
        log.info("[BidService] 즉시 구매: bidderId={}, auctionId={}, queueVerified={}",
                bidderId, auctionId, queueVerified);

        // Gateway에서 Queue 검증을 통과하지 못한 요청 차단
        if (!queueVerified) {
            log.warn("[BidService] Queue 검증 실패 (Gateway 우회 접근): bidderId={}, auctionId={}",
                    bidderId, auctionId);
            throw new BidDomainException(BidErrorCode.QUEUE_ACCESS_DENIED);
        }

        // 즉시 구매 처리
        Bid bid = buyNow(auctionId, bidderId);

        log.info("[BidService] 즉시 구매 완료: bidId={}, auctionId={}",
                bid.getId(), auctionId);

        return bid;
    }
}


