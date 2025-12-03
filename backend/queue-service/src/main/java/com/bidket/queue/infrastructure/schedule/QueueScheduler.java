package com.bidket.queue.infrastructure.schedule;

import com.bidket.queue.domain.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {
    private final RedisRepository redisRepository;

    @Scheduled(fixedDelay = 1000)
    public void entranceSchedule() {
        redisRepository.getAllActiveAuctions()
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(this::processAuction)
                .subscribe(
                        null,
                        error -> log.error("대기열 입장 스케줄러 에러")
                );
    }

    private Mono<Void> processAuction(UUID auctionId) {
        String configKey = "queue:auction:" + auctionId + ":config";
        String activeKey = "queue:auction:" + auctionId + ":active";
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return redisRepository.getConfig(configKey)
                .flatMap(config ->
                        redisRepository.activeUserCount(activeKey)
                                .flatMap(currentActive -> {
                                    long maxUser = config.getMaxActive();
                                    long availableSlots = maxUser - currentActive;
                                    long limit = Math.min(availableSlots, config.getPermitsPerSec());

                                    if (limit <= 0)
                                        return Mono.empty();

                                    return redisRepository.popUserIdWaitingQueue(waitingKey, limit)
                                            .flatMap(users -> {
                                                if (users.isEmpty())
                                                    return Mono.empty();

                                                log.info("경매[{}] {} 명 입장", auctionId, users.size());

                                                return redisRepository.addAllActiveUser(waitingKey, users);
                                            });

                                })

                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("경매[{}] 설정 만료", auctionId);
                    log.debug("경매[{}] 관리 목록에서 제거", auctionId);

                    return redisRepository.removeActiveAuction(auctionId);
                }))
                .then();
    }
}
