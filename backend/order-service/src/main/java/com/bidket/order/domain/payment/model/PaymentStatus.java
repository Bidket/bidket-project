package com.bidket.order.domain.payment.model;

public enum PaymentStatus {
    PENDING,   // 결제 요청 생성됨
    SUCCESS,   // 결제 성공
    FAILED     // 결제 실패
}