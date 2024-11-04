package org.omocha.api.application;

import org.omocha.domain.auction.AuctionCommand;
import org.omocha.domain.auction.AuctionInfo;
import org.omocha.domain.auction.AuctionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionFacade {

	private final AuctionService auctionService;

	public Long addAuction(AuctionCommand.AddAuction addCommand) {
		return auctionService.addAuction(addCommand);
	}

	public Page<AuctionInfo.SearchAuction> searchAuction(
		AuctionCommand.SearchAuction searchAuction,
		Pageable pageable
	) {
		return auctionService.searchAuction(searchAuction, pageable);
	}

	public AuctionInfo.RetrieveAuction retrieveAuction(AuctionCommand.RetrieveAuction retrieveCommand) {
		return auctionService.retrieveAuction(retrieveCommand);
	}

	public void removeAuction(AuctionCommand.RemoveAuction removeCommand) {
		auctionService.removeAuction(removeCommand);
	}

}
