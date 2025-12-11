package com.bidket.order.domain.refund.repository;

import com.bidket.order.domain.refund.model.Refund;

public interface RefundRepository {

    Refund save(Refund refund);
}