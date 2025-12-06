package com.bidket.queue.application.aop;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QueueConfigAspect {
    private final QueueManagementRepository managementRepository;

    @Around("@annotation(com.bidket.queue.global.annotation.CheckQueueConfig)")
    public Object checkQueueConfig(ProceedingJoinPoint joinPoint) {
        UUID auctionId = findAuctionId(joinPoint);
        String configKey = "queue:auction:" + auctionId + ":config";

        return managementRepository.getConfig(configKey)
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.CONFIG_NOT_FOUND)))
                .flatMap(config -> {
                    config.checkOpenStatus(Instant.now());
                    log.info("경매[{}]: 경매 대기열 상태 확인", auctionId);
                    try {
                        return (Mono<?>) joinPoint.proceed();
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                });
    }

    private UUID findAuctionId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if ("auctionId".equals(paramNames[i])) {
                return (UUID) args[i];
            }
        }
        throw new IllegalArgumentException("메소드에 'auctionId' 파라미터가 없습니다.");
    }
}
