package com.bidket.auction.application.service;

import com.bidket.auction.infrastructure.client.queue.QueueClient;
import com.bidket.auction.infrastructure.client.queue.dto.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueValidationService {

    private final QueueClient queueClient;

    public boolean isUserActive(UUID userId, UUID auctionId) {
        try {
            QueueStatusResponse response = queueClient.checkQueueStatus(auctionId, userId);

            log.debug("Queue 상태 확인: status={}, userId={}, auctionId={}",
                response.status(), userId, auctionId);

            return "ACTIVE".equals(response.status());
        } catch (Exception e) {
            log.error("Queue 상태 확인 실패: userId={}, auctionId={}", userId, auctionId, e);
            return false;
        }
    }

    public QueueStatusResponse getQueueStatus(UUID userId, UUID auctionId) {
        try {
            return queueClient.checkQueueStatus(auctionId, userId);
        } catch (Exception e) {
            log.error("Queue 상태 조회 실패: userId={}, auctionId={}", userId, auctionId, e);
            throw new RuntimeException("Queue 상태 조회 실패", e);
        }
    }
}
