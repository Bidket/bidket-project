package com.bidket.order.domain.refund.model;

public enum RefundStatus {
    REQUESTED,  // 환불 요청 접수
    APPROVED,   // 환불 승인
    REJECTED    // 환불 거절
}