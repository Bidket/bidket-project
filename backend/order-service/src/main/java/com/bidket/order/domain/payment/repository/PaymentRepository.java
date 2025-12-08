package com.bidket.order.domain.payment.repository;

import com.bidket.order.domain.payment.model.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);
}