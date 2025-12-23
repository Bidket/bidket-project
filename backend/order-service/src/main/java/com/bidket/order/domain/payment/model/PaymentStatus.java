package com.bidket.order.domain.payment.model;

public enum PaymentStatus {
    PENDING,           // 결제 요청 생성
    SUCCESS,           // 결제 성공
    FAILED,            // 결제 실패

    REFUND_REQUESTED,  // 환불 요청 접수
    REFUNDED           // 환불 완료
}