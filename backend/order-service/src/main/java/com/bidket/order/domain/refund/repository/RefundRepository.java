package com.bidket.order.domain.refund.repository;

import com.bidket.order.domain.refund.model.Refund;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RefundRepository {

    Refund save(Refund refund);

    Page<Refund> findByUserId(UUID userId, Pageable pageable);
}