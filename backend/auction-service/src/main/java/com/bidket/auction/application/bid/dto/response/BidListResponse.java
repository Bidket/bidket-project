package com.bidket.auction.application.bid.dto.response;

import java.util.List;

public record BidListResponse(
        List<BidResponse> bids,
        long totalCount,
        Integer currentPage,
        Integer totalPages
) {
    public static BidListResponse of(List<BidResponse> bids, long totalCount) {
        return new BidListResponse(bids, totalCount, null, null);
    }

    public static BidListResponse of(List<BidResponse> bids, long totalCount, Integer currentPage, Integer totalPages) {
        return new BidListResponse(bids, totalCount, currentPage, totalPages);
    }
}


