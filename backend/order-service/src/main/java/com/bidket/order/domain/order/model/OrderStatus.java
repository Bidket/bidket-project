package com.bidket.order.domain.order.model;

public enum OrderStatus {
    PAYMENT,            // 결제 대기
    PAID,               // 결제 완료
    CANCELED,           // 주문 취소
    EXPIRED,            // 결제 만료

    REFUND_REQUESTED,   // 환불 요청 접수
    REFUNDED            // 환불 완료
}