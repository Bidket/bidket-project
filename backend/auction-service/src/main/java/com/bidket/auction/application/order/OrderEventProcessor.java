package com.bidket.auction.application.order;

import java.util.Map;

/**
 * Order Service로부터 수신한 이벤트를 처리하는 인터페이스
 */
public interface OrderEventProcessor {

    /**
     * 이벤트 타입에 따라 적절한 핸들러로 라우팅
     *
     * @param eventType 이벤트 타입 (ORDER_CREATED, ORDER_CREATION_FAILED)
     * @param payload   이벤트 페이로드
     */
    void process(String eventType, Map<String, Object> payload);
}
