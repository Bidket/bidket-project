package com.bidket.queue.domain.model;
/**
 * 대기 인원
 * 2500 미만 원활
 * 2500 이상 혼잡
 *
 */
public enum QueueStatus {
    SMOOTH,
    CROWDED,
    FULL;

    public static QueueStatus checkStatus(Long count) {
        if(count == 5000)
            return FULL;
        if(count < 2500)
            return SMOOTH;
        else
            return CROWDED;
    }
}
