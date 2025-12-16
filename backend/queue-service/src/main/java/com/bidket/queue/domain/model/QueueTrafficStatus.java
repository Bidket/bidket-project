package com.bidket.queue.domain.model;
/**
 * 대기 인원
 * 2500 미만 원활
 * 2500 이상 혼잡
 *
 */
public enum QueueTrafficStatus {
    SMOOTH,
    CROWDED,
    FULL;

    public static QueueTrafficStatus checkStatus(long count, long maxUser) {
        if(count == maxUser)
            return FULL;
        if(count < maxUser / 2)
            return SMOOTH;
        else
            return CROWDED;
    }
}
