package org.omocha.domain.auction.bid;

import java.time.LocalDateTime;
import java.util.List;

import org.omocha.domain.auction.Auction;
import org.omocha.domain.auction.AuctionReader;
import org.omocha.domain.auction.chat.ChatCommand;
import org.omocha.domain.auction.chat.ChatService;
import org.omocha.domain.auction.conclude.ConcludeStore;
import org.omocha.domain.member.Member;
import org.omocha.domain.member.MemberReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

	private final AuctionReader auctionReader;
	private final BidReader bidReader;
	private final BidStore bidStore;
	private final BidValidator bidValidator;
	private final MemberReader memberReader;
	private final ConcludeStore concludeStore;
	private final ChatService chatService;

	// TODO : 동시성 해결 해결 해야 함

	@Override
	@Transactional(readOnly = true)
	public List<BidInfo.BidList> retrieveBids(Long auctionId) {
		return bidReader.getBidList(auctionId)
			.stream()
			.map(BidInfo.BidList::toInfo)
			.toList();
	}

	// TODO : 최고 입찰가 관련 논의 후 수정 필요
	@Override
	@Transactional
	public BidInfo.AddBid addBid(BidCommand.AddBid addBid) {

		Long buyerId = addBid.buyerId();
		Long auctionId = addBid.auctionId();
		Long bidPrice = addBid.bidPrice();

		Auction auction = auctionReader.getAuction(auctionId);
		auction.validateAuctionStatus();

		bidValidator.bidValidate(auction, buyerId, bidPrice);

		Member member = memberReader.getMember(buyerId);

		Bid bid = bidStore.store(auction, member, bidPrice);

		HighestBidManager.setHighestBid(auctionId, bid);

		return BidInfo.AddBid.toInfo(bid);
	}

	@Override
	@Transactional(readOnly = true)
	public BidInfo.NowPrice retrieveNowPrice(Long auctionId) {
		return HighestBidManager.getCurrentHighestBid(auctionId, bidReader)
			.map(BidInfo.NowPrice::toInfo)
			.orElseGet(() -> new BidInfo.NowPrice(0L, null, LocalDateTime.now()));
	}

	@Override
	@Transactional
	public void buyNow(BidCommand.BuyNow buyNowCommand) {
		Long buyerId = buyNowCommand.memberId();

		Auction auction = auctionReader.getAuction(buyNowCommand.auctionId());
		auction.validateAuctionStatus();
		bidValidator.instantBuyValidate(auction, buyerId);

		Member buyer = memberReader.getMember(buyerId);
		Bid bid = bidStore.store(auction, buyer, auction.getInstantBuyPrice());
		concludeStore.store(auction, bid);

		var chatRoomCommand = new ChatCommand.AddChatRoom(auction.getAuctionId(), buyerId);
		chatService.addChatRoom(chatRoomCommand);

		auction.statusConcluded();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<BidInfo.RetrieveMyBids> retrieveMyBids(BidCommand.RetrieveMyBids retrieveMyBidsCommand,
		Pageable sortPage) {

		return bidReader.getMyBidList(retrieveMyBidsCommand.memberId(), retrieveMyBidsCommand.auctionId(), sortPage);

	}

}