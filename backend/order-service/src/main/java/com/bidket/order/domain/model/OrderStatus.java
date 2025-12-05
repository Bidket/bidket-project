package com.bidket.order.domain.model;

public enum OrderStatus {
    PAYMENT,    // 결제 대기
    PAID,       // 결제 완료
    CANCELED,   // 주문 취소
    EXPIRED     // 결제 만료
}
