package com.bidket.queue.application.service;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.model.QueueMetrics;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.annotation.CheckQueueConfig;
import com.bidket.queue.presentation.dto.request.QueueConfigUpdateRequest;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueConfigUpdateResponse;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueManagementService {
    private final QueueManagementRepository managementRepository;
    private final QueueTrafficRepository trafficRepository;

    public Mono<QueueCreateResponse> createConfigQueue(QueueCreateRequest request) {
        String configKey = "queue:auction:" + request.auctionId() + ":config";
        QueueConfigModel queueConfig = QueueConfigModel.from(request);
        return managementRepository.saveConfig(request.auctionId(), queueConfig)
                .flatMap(isSuccess -> {
                    if (!isSuccess)
                        return Mono.error(new QueueException(QueueErrorCode.REDIS_SAVE_FAILED));

                    return managementRepository.setExpiration(configKey, request.closeAt().plus(1, ChronoUnit.DAYS));
                })
                .onErrorResume(e -> {
                    if (e instanceof QueueException)
                        return Mono.error(e);

                    return managementRepository.deleteConfig(request.auctionId())
                            .then(Mono.error(new QueueException(QueueErrorCode.REDIS_EXPIRE_SET_FAILED)));
                })
                .flatMap(isExpireSuccess -> {
                    if (isExpireSuccess) {
                        return managementRepository.registerActiveAuction(request.auctionId())
                                .onErrorMap(e -> new QueueException(QueueErrorCode.AUCTION_REGISTER_FAIL))
                                .map(addedCount -> queueConfig.toCreateResponse());
                    }
                    return Mono.error(new QueueException(QueueErrorCode.REDIS_EXPIRE_SET_FAILED));
                })
                .onErrorMap(e -> {
                    if (e instanceof QueueException)
                        return e;

                    return new QueueException(QueueErrorCode.REDIS_CONNECTION_ERROR);
                });
    }

    @CheckQueueConfig
    public Mono<QueueConfigUpdateResponse> updateConfig(UUID auctionId, QueueConfigUpdateRequest request) {
        Map<String, String> updateFields = new HashMap<>();
        if (request.maxActive() != null)
            updateFields.put("maxActive", request.maxActive().toString());
        if (request.permitsPerSec() != null)
            updateFields.put("permitsPerSec", request.permitsPerSec().toString());
        if (request.status() != null)
            updateFields.put("status", request.status().toString());

        if (!updateFields.isEmpty()) {
            return managementRepository.updateConfig(auctionId, updateFields)
                    .flatMap(isSuccess -> managementRepository.getConfig(auctionId)
                            .map(QueueConfigModel::toUpdateResponse));
        }

        return Mono.error(new QueueException(QueueErrorCode.INVALID_UPDATE_FIELD));
    }

    @CheckQueueConfig
    public Mono<QueueMetricsResponse> getQueueMetrics(UUID auctionId, UUID userId) {
        return Mono.zip(
                        managementRepository.getConfig(auctionId),
                        trafficRepository.getWaitingUserCount(auctionId).defaultIfEmpty(0L),
                        trafficRepository.getActiveUserCount(auctionId).defaultIfEmpty(0L))
                .map(tuple -> {
                    QueueConfigModel config = tuple.getT1();

                    QueueMetrics metrics = QueueMetrics.builder()
                            .totalWaiting(tuple.getT2())
                            .currentActive(tuple.getT3())
                            .totalTraffic(tuple.getT2() + tuple.getT3())
                            .maxActive(config.maxActive())
                            .build();

                    return QueueMetricsResponse.builder()
                            .auctionId(auctionId)
                            .status(config.status())
                            .metrics(metrics)
                            .lastUpdated(config.lastUpdatedAt())
                            .lastUpdatedBy(config.lastUpdatedBy())
                            .build();
                });
    }
}
