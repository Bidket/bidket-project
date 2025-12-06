package com.bidket.queue.infrastructure.schedule;

import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.util.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {
    private final QueueManagementRepository managementRepository;
    private final QueueTrafficRepository trafficRepository;
    private final TokenProvider tokenProvider;

    @Scheduled(fixedDelay = 1000)
    public void entranceSchedule() {
        log.info("스케줄링 시작");
        managementRepository.getAllActiveAuctions()
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(this::processAuction)
                .subscribe(
                        null,
                        e -> log.error("대기열 입장 스케줄러 에러", e)
                );
    }

    private Mono<Void> processAuction(UUID auctionId) {
        String configKey = "queue:auction:" + auctionId + ":config";
        String activeKey = "queue:auction:" + auctionId + ":active";
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return managementRepository.getConfig(configKey)
                .flatMap(config ->
                        trafficRepository.getActiveUserCount(activeKey)
                                .flatMap(currentActive -> {
                                    long maxUser = config.getMaxActive();
                                    long availableSlots = maxUser - currentActive;
                                    long limit = Math.min(availableSlots, config.getPermitsPerSec());

                                    if (limit <= 0)
                                        return Mono.just(0L);

                                    return trafficRepository.popUserIdWaitingQueue(waitingKey, limit)
                                            .flatMap(userIds -> {
                                                if (userIds.isEmpty())
                                                    return Mono.just(0L);

                                                Map<UUID, String> userTokens = new HashMap<>();
                                                userIds.forEach(userId -> {
                                                    String token = tokenProvider.generateToken(userId, auctionId);
                                                    userTokens.put(userId, token);
                                                });

                                                log.info("경매[{}] {} 명 입장", auctionId, userIds.size());
                                                return trafficRepository.addAllActiveUser(activeKey, userIds)
                                                        .flatMap(added -> {
                                                            String tokenKey = "queue:token:" + auctionId;
                                                            return trafficRepository.saveToken(tokenKey, userTokens);
                                                        });
                                            });

                                })

                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("경매[{}] 설정 만료, 관리 목록에서 제거", auctionId);

                    return managementRepository.removeActiveAuction(auctionId)
                            .then(Mono.empty());
                }))
                .then();
    }
}
