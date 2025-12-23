package com.bidket.auction.application.compensation;

import java.util.UUID;

@FunctionalInterface
public interface CompensationAction {

    void execute(UUID sagaId, UUID aggregateId, String payload) throws Exception;
}
