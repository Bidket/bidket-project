package com.bidket.order.application.order.port;

import java.util.UUID;

public interface AuctionQueryPort {

    AuctionSnapshot getAuction(UUID auctionId);
}