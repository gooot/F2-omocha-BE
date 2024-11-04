package org.omocha.infra.repository;

import java.util.List;
import java.util.Optional;

import org.omocha.domain.auction.bid.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long>, BidRepositoryCustom {

	List<Bid> findAllByAuctionAuctionIdOrderByCreatedAtDesc(Long auctionId);

	Optional<Bid> findTopByAuctionAuctionIdOrderByBidPriceDesc(Long auctionId);

	Long countByAuctionAuctionId(Long auctionId);
}
