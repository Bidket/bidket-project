package com.bidket.order.infrastructure.refund.impl;

import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.repository.RefundRepository;
import com.bidket.order.infrastructure.refund.entity.RefundEntity;
import com.bidket.order.infrastructure.refund.repository.RefundJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefundRepositoryImpl implements RefundRepository {

    private final RefundJpaRepository jpaRepository;

    @Override
    public Refund save(Refund refund) {
        RefundEntity saved = jpaRepository.save(RefundEntity.from(refund));
        return saved.toModel();
    }

    @Override
    public Page<Refund> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository
                .findByUserId(userId, pageable)
                .map(RefundEntity::toModel);
    }
}