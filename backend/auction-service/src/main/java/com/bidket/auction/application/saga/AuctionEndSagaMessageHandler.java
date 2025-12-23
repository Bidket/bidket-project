package com.bidket.auction.application.saga;

import com.bidket.auction.application.order.OrderSagaMessageHandler;
import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnBean(AuctionEndSagaOrchestrator.class)
@RequiredArgsConstructor
public class AuctionEndSagaMessageHandler implements OrderSagaMessageHandler {

    private final AuctionEndSagaOrchestrator orchestrator;
    private final AuctionEndSagaContextRepository sagaRepository;

    @Override
    @Transactional
    public void handleOrderCreated(Map<String, Object> payload) {
         
        Map<String, Object> data = getDataMap(payload);

        UUID sagaId = parseUuid(data, "sagaId");
        UUID orderId = parseUuid(data, "orderId");
        UUID auctionId = parseUuid(data, "auctionId");

        log.info("[AuctionEndSagaMessageHandler] ORDER_CREATED 수신: sagaId={}, orderId={}, auctionId={}",
                sagaId, orderId, auctionId);

        AuctionEndSagaContext sagaContext = findSagaContext(sagaId);

        sagaContext.recordOrderId(orderId);
        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[AuctionEndSagaMessageHandler] orderId 저장 완료: sagaId={}, orderId={}, nextStep={}",
                sagaId, orderId, sagaContext.getCurrentStep());

        orchestrator.executeMarkWinningBidStep(sagaContext);
    }

    @Override
    @Transactional
    public void handleOrderCreationFailed(Map<String, Object> payload) {
         
        Map<String, Object> data = getDataMap(payload);

        UUID sagaId = parseUuid(data, "sagaId");
        UUID auctionId = parseUuid(data, "auctionId");
        String failureReason = (String) data.get("failureReason");

        log.warn("[AuctionEndSagaMessageHandler] ORDER_CREATION_FAILED 수신: sagaId={}, auctionId={}, reason={}",
                sagaId, auctionId, failureReason);

        orchestrator.compensate(sagaId, failureReason);

        log.info("[AuctionEndSagaMessageHandler] 보상 트랜잭션 완료: sagaId={}, auctionId={}",
                sagaId, auctionId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDataMap(Map<String, Object> payload) {
        Object dataObj = payload.get("data");
        if (dataObj instanceof Map) {
            return (Map<String, Object>) dataObj;
        }
        throw new IllegalArgumentException("Invalid event structure: 'data' field is missing or not a Map");
    }

    private UUID parseUuid(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        return UUID.fromString(value.toString());
    }

    private AuctionEndSagaContext findSagaContext(UUID sagaId) {
        return sagaRepository.findById(sagaId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.SAGA_NOT_FOUND));
    }
}
