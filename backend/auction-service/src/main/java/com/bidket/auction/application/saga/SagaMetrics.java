package com.bidket.auction.application.saga;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Saga 실행 메트릭 수집기
 * SAGA-005: Saga Orchestration Enhancements
 *
 * 역할:
 * - Saga 실행 시간 및 성공/실패 추적
 * - Circuit Breaker 상태 모니터링
 * - Retry 횟수 및 Timeout 발생 추적
 * - Prometheus와 연동하여 메트릭 노출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Saga 실행 시간 및 상태 기록
     *
     * @param sagaType Saga 타입 (AUCTION_END, PAYMENT_TIMEOUT)
     * @param status   실행 상태 (COMPLETED, FAILED, COMPENSATED)
     * @param duration 실행 시간
     */
    public void recordSagaExecution(String sagaType, String status, Duration duration) {
        try {
            Timer.builder("saga.execution")
                    .description("Saga execution time")
                    .tag("saga_type", sagaType)
                    .tag("status", status)
                    .register(meterRegistry)
                    .record(duration);

            log.debug("[SagaMetrics] Saga 실행 기록: type={}, status={}, duration={}ms",
                    sagaType, status, duration.toMillis());

        } catch (Exception e) {
            log.error("[SagaMetrics] 메트릭 기록 실패: saga={}, status={}", sagaType, status, e);
        }
    }

    /**
     * Saga Step 실행 시간 기록
     *
     * @param sagaType Saga 타입
     * @param step     Saga 단계명
     * @param duration 실행 시간
     */
    public void recordStepExecution(String sagaType, String step, Duration duration) {
        try {
            Timer.builder("saga.step.execution")
                    .description("Saga step execution time")
                    .tag("saga_type", sagaType)
                    .tag("step", step)
                    .register(meterRegistry)
                    .record(duration);

            log.debug("[SagaMetrics] Step 실행 기록: type={}, step={}, duration={}ms",
                    sagaType, step, duration.toMillis());

        } catch (Exception e) {
            log.error("[SagaMetrics] Step 메트릭 기록 실패: saga={}, step={}", sagaType, step, e);
        }
    }

    /**
     * Circuit Breaker 상태 전환 기록
     *
     * @param serviceName Circuit Breaker 이름 (orderService, productService)
     * @param state       Circuit Breaker 상태 (OPEN, CLOSED, HALF_OPEN)
     */
    public void recordCircuitBreakerStateTransition(String serviceName, String state) {
        try {
            Counter.builder("saga.circuit_breaker.state_transitions")
                    .description("Circuit Breaker state transition count")
                    .tag("service", serviceName)
                    .tag("state", state)
                    .register(meterRegistry)
                    .increment();

            log.info("[SagaMetrics] Circuit Breaker 상태 전환: service={}, state={}", serviceName, state);

        } catch (Exception e) {
            log.error("[SagaMetrics] Circuit Breaker 메트릭 기록 실패: service={}, state={}", serviceName, state, e);
        }
    }

    /**
     * Retry 발생 횟수 기록
     *
     * @param sagaType    Saga 타입
     * @param step        Saga 단계명
     * @param retryCount  재시도 횟수
     */
    public void recordRetry(String sagaType, String step, int retryCount) {
        try {
            Counter.builder("saga.retry")
                    .description("Saga step retry count")
                    .tag("saga_type", sagaType)
                    .tag("step", step)
                    .register(meterRegistry)
                    .increment(retryCount);

            log.debug("[SagaMetrics] Retry 기록: type={}, step={}, count={}", sagaType, step, retryCount);

        } catch (Exception e) {
            log.error("[SagaMetrics] Retry 메트릭 기록 실패: saga={}, step={}", sagaType, step, e);
        }
    }

    /**
     * Timeout 발생 횟수 기록
     *
     * @param sagaType Saga 타입
     * @param step     Saga 단계명
     */
    public void recordTimeout(String sagaType, String step) {
        try {
            Counter.builder("saga.timeout")
                    .description("Saga step timeout count")
                    .tag("saga_type", sagaType)
                    .tag("step", step)
                    .register(meterRegistry)
                    .increment();

            log.warn("[SagaMetrics] Timeout 발생: type={}, step={}", sagaType, step);

        } catch (Exception e) {
            log.error("[SagaMetrics] Timeout 메트릭 기록 실패: saga={}, step={}", sagaType, step, e);
        }
    }

    /**
     * 보상 트랜잭션 실행 기록
     *
     * @param sagaType Saga 타입
     * @param success  보상 성공 여부
     */
    public void recordCompensation(String sagaType, boolean success) {
        try {
            Counter.builder("saga.compensation")
                    .description("Saga compensation execution count")
                    .tag("saga_type", sagaType)
                    .tag("result", success ? "success" : "failure")
                    .register(meterRegistry)
                    .increment();

            log.info("[SagaMetrics] 보상 트랜잭션 기록: type={}, success={}", sagaType, success);

        } catch (Exception e) {
            log.error("[SagaMetrics] 보상 메트릭 기록 실패: saga={}, success={}", sagaType, success, e);
        }
    }

    /**
     * Bulkhead 거부 횟수 기록
     *
     * @param bulkheadName Bulkhead 이름
     */
    public void recordBulkheadRejection(String bulkheadName) {
        try {
            Counter.builder("saga.bulkhead.rejection")
                    .description("Bulkhead rejection count")
                    .tag("bulkhead", bulkheadName)
                    .register(meterRegistry)
                    .increment();

            log.warn("[SagaMetrics] Bulkhead 거부 발생: bulkhead={}", bulkheadName);

        } catch (Exception e) {
            log.error("[SagaMetrics] Bulkhead 메트릭 기록 실패: bulkhead={}", bulkheadName, e);
        }
    }

    /**
     * 현재 진행 중인 Saga 개수 기록 (Gauge)
     *
     * @param sagaType Saga 타입
     * @param count    진행 중인 Saga 수
     */
    public void recordInProgressSagaCount(String sagaType, int count) {
        try {
            meterRegistry.gauge("saga.in_progress",
                    io.micrometer.core.instrument.Tags.of("saga_type", sagaType),
                    count);

            log.debug("[SagaMetrics] 진행 중인 Saga 개수: type={}, count={}", sagaType, count);

        } catch (Exception e) {
            log.error("[SagaMetrics] Saga 개수 메트릭 기록 실패: saga={}", sagaType, e);
        }
    }

    /**
     * Idempotency Key 중복 감지 횟수 기록
     *
     * @param sagaType Saga 타입
     * @param step     Saga 단계명
     */
    public void recordIdempotencyKeyDuplication(String sagaType, String step) {
        try {
            Counter.builder("saga.idempotency.duplication")
                    .description("Idempotency key duplication detection count")
                    .tag("saga_type", sagaType)
                    .tag("step", step)
                    .register(meterRegistry)
                    .increment();

            log.info("[SagaMetrics] Idempotency Key 중복 감지: type={}, step={}", sagaType, step);

        } catch (Exception e) {
            log.error("[SagaMetrics] Idempotency 메트릭 기록 실패: saga={}, step={}", sagaType, step, e);
        }
    }

    /**
     * Saga 실행 시간 측정을 위한 Timer Sample 시작
     *
     * @return Timer.Sample 객체
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Timer Sample 종료 및 기록
     *
     * @param sample   Timer.Sample 객체
     * @param sagaType Saga 타입
     * @param status   실행 상태
     */
    public void stopTimer(Timer.Sample sample, String sagaType, String status) {
        try {
            sample.stop(Timer.builder("saga.execution")
                    .tag("saga_type", sagaType)
                    .tag("status", status)
                    .register(meterRegistry));

        } catch (Exception e) {
            log.error("[SagaMetrics] Timer 종료 실패: saga={}, status={}", sagaType, status, e);
        }
    }
}
