package com.bidket.auction.infrastructure.retry;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class OptimisticLockRetryAspect {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 50;
    @Around("@annotation(com.bidket.auction.infrastructure.retry.RetryOnOptimisticLock)")
    public Object retryOnOptimisticLock(ProceedingJoinPoint joinPoint) throws Throwable {

        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                return joinPoint.proceed();

            } catch (OptimisticLockingFailureException e) {
                attempt++;

                if (attempt >= MAX_RETRIES) {
                    log.error("Optimistic Lock 재시도 실패 - 최대 시도 횟수 초과: {} (시도 횟수: {})",
                            joinPoint.getSignature().getName(),
                            attempt);
                    throw e;
                }

                log.warn("Optimistic Lock 충돌 발생 - 재시도 중: {} (시도 {}/{})",
                        joinPoint.getSignature().getName(),
                        attempt,
                        MAX_RETRIES);

                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IllegalStateException("재시도 로직 실행 중 예상치 못한 오류 발생");
    }
}

