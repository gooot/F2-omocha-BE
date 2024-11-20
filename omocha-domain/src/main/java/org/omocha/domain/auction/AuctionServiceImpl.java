package org.omocha.domain.auction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.omocha.domain.auction.exception.AuctionHasBidException;
import org.omocha.domain.auction.exception.AuctionImageNotFoundException;
import org.omocha.domain.auction.exception.AuctionOwnerMismatchException;
import org.omocha.domain.category.CategoryInfo;
import org.omocha.domain.category.CategoryReader;
import org.omocha.domain.category.CategoryStore;
import org.omocha.domain.category.exception.CategoryNotFoundException;
import org.omocha.domain.image.Image;
import org.omocha.domain.likes.LikeReader;
import org.omocha.domain.likes.LikeStore;
import org.omocha.domain.member.MemberReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

	private final AuctionStore auctionStore;
	private final AuctionImagesFactory auctionImagesFactory;
	private final AuctionReader auctionReader;
	private final CategoryReader categoryReader;
	private final CategoryStore categoryStore;
	private final LikeReader likeReader;
	private final LikeStore likeStore;
	private final MemberReader memberReader;

	@Override
	@Transactional
	public Long addAuction(AuctionCommand.AddAuction addCommand) {
		if (addCommand.images() == null || addCommand.images().isEmpty()) {
			throw new AuctionImageNotFoundException(addCommand.memberId());
		}

		Auction auction = auctionStore.store(addCommand.toEntity());
		auctionImagesFactory.store(auction, addCommand);
		categoryStore.auctionCategoryStore(auction, addCommand);

		return auction.getAuctionId();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AuctionInfo.SearchAuction> searchAuction(
		AuctionCommand.SearchAuction searchAuction,
		Pageable pageable
	) {

		List<Long> subCategoryIds = categoryReader.getSubCategoryIds(searchAuction.categoryId());

		Page<AuctionInfo.SearchAuction> auctionList = auctionReader.getAuctionList(searchAuction, subCategoryIds,
			pageable);

		List<AuctionInfo.SearchAuction> categoryAuctions = auctionList.getContent().stream()
			.map(auctionInfo -> {
				List<CategoryInfo.CategoryResponse> categoryHierarchy =
					categoryReader.getCategoryHierarchyUpwards(searchAuction.categoryId());
				return categoryHierarchy != null ? auctionInfo.withCategoryHierarchy(categoryHierarchy) : auctionInfo;
			})
			.collect(Collectors.toList());

		return PageableExecutionUtils.getPage(
			categoryAuctions,
			pageable,
			auctionList::getTotalElements
		);

	}

	@Override
	@Transactional(readOnly = true)
	public AuctionInfo.RetrieveAuction retrieveAuction(AuctionCommand.RetrieveAuction retrieveCommand) {
		Auction auction = auctionReader.getAuction(retrieveCommand.auctionId());

		List<String> imagePaths = auction.getImages().stream()
			.map(Image::getImagePath)
			.collect(Collectors.toList());

		List<Long> categoryIds = auction.getAuctionCategories()
			.stream()
			.map(auctionCategory -> auctionCategory.getCategory().getCategoryId())
			.toList();

		if (categoryIds.isEmpty()) {
			throw new CategoryNotFoundException(CategoryNotFoundException.Type.AUCTION_ID, auction.getAuctionId());
		}

		Long selectedCategoryId = categoryIds.get(0);

		List<CategoryInfo.CategoryResponse> categoryHierarchy =
			categoryReader.getCategoryHierarchyUpwards(selectedCategoryId);

		return new AuctionInfo.RetrieveAuction(auction, imagePaths, categoryHierarchy);
	}

	@Override
	@Transactional
	public void removeAuction(AuctionCommand.RemoveAuction removeCommand) {
		Auction auction = auctionReader.getAuction(removeCommand.auctionId());

		Long memberId = removeCommand.memberId();

		if (!auction.getMemberId().equals(memberId)) {
			throw new AuctionOwnerMismatchException(memberId);
		}

		if (auction.getBidCount() != null && auction.getBidCount() != 0) {
			throw new AuctionHasBidException(auction.getAuctionId());
		}

		auctionStore.removeAuction(auction);
	}

	@Override
	@Transactional
	public AuctionInfo.LikeAuction likeAuction(AuctionCommand.LikeAuction likeCommand) {

		Long auctionId = likeCommand.auctionId();
		Long memberId = likeCommand.memberId();

		Auction auction = auctionReader.getAuction(auctionId);

		boolean likeStatus = likeReader.getAuctionLikeStatus(likeCommand);

		if (!likeStatus) {
			likeStore.clickLike(likeCommand, LocalDateTime.now());
			auction.increaseLikeCount();
			return AuctionInfo.LikeAuction.toResponse(auctionId, memberId, "LIKE");
		} else {
			likeStore.unClickLike(likeCommand);
			auction.decreaseLikeCount();
			return AuctionInfo.LikeAuction.toResponse(auctionId, memberId, "UNLIKE");
		}
	}

	@Override
	public Page<AuctionInfo.RetrieveMyAuctionLikes> retrieveMyAuctionLikes(Long memberId, Pageable pageable) {
		return likeReader.getMyAuctionLikes(memberId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AuctionInfo.RetrieveMyAuctions> retrieveMyAuctions(
		AuctionCommand.RetrieveMyAuctions retrieveMyAuctionsCommand,
		Pageable pageable
	) {
		return auctionReader.getMyAuctionList(retrieveMyAuctionsCommand, pageable);
	}

	@Override
	public Page<AuctionInfo.RetrieveMemberAuctions> retrieveMemberAuctions(
		AuctionCommand.RetrieveMemberAuctions retrieveMemberAuctions,
		Pageable pageable
	) {
		memberReader.getMember(retrieveMemberAuctions.memberId());

		return auctionReader.getMemberAuctionList(retrieveMemberAuctions, pageable);
	}

	@Override
	public Page<AuctionInfo.RetrieveMyBidAuctions> retrieveMyBidAuctions(
		AuctionCommand.RetrieveMyBidAuctions retrieveMyBidAuctionsCommand,
		Pageable pageable
	) {
		return auctionReader.getMyBidAuctionList(retrieveMyBidAuctionsCommand.memberId(), pageable);
	}

}
