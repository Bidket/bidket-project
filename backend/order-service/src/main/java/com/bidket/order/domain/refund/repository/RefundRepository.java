package com.bidket.order.domain.refund.repository;

import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.model.RefundStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RefundRepository {

    Refund save(Refund refund);

    Page<Refund> findByUserId(UUID userId, Pageable pageable);

    Optional<Refund> findById(UUID refundId);

    boolean existsByPaymentIdAndStatusIn(UUID paymentId, List<RefundStatus> statuses);
}