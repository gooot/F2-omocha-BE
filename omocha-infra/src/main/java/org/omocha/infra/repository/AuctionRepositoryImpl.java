package org.omocha.infra.repository;

import static org.omocha.domain.auction.QAuction.*;
import static org.omocha.domain.auction.QAuctionCategory.*;
import static org.omocha.domain.auction.QCategory.*;
import static org.omocha.domain.auction.QLikes.*;
import static org.omocha.domain.auction.bid.QBid.*;
import static org.omocha.domain.auction.conclude.QConclude.*;
import static org.springframework.util.ObjectUtils.*;

import java.util.List;

import org.omocha.domain.auction.Auction;
import org.omocha.domain.auction.AuctionCommand;
import org.omocha.domain.auction.AuctionInfo;
import org.omocha.domain.auction.QAuction;
import org.omocha.domain.auction.QAuctionCategory;
import org.omocha.domain.auction.QAuctionInfo_RetrieveMyAuctions;
import org.omocha.domain.auction.QAuctionInfo_RetrieveMyBidAuctions;
import org.omocha.domain.auction.QAuctionInfo_SearchAuction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

@Repository
public class AuctionRepositoryImpl implements AuctionRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public AuctionRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}

	public Page<AuctionInfo.SearchAuction> getAuctionList(
		AuctionCommand.SearchAuction searchAuction,
		List<Long> subCategoryIds,
		Pageable pageable
	) {
		Long memberId = searchAuction.memberId();

		JPAQuery<AuctionInfo.SearchAuction> query = queryFactory
			.select(new QAuctionInfo_SearchAuction(
				auction.auctionId,
				auction.memberId,
				auction.title,
				auction.content,
				auction.startPrice,
				auction.bidUnit,
				auction.instantBuyPrice,
				auction.auctionStatus,
				auction.thumbnailPath,
				auction.nowPrice,
				conclude.concludePrice,
				auction.bidCount,
				isLiked(memberId),
				auction.startDate,
				auction.endDate,
				auction.createdAt
			))
			.from(auction)
			.leftJoin(conclude).on(conclude.auction.eq(auction))
			.leftJoin(auctionCategory).on(auctionCategory.auction.eq(auction))
			.leftJoin(category).on(auctionCategory.category.eq(category));

		if (memberId != null) {
			query.leftJoin(likes).on(likes.auction.eq(auction)
				.and(likes.member.memberId.eq(memberId)));
		}

		query.where(
			titleContains(searchAuction.title()),
			statusEquals(searchAuction.auctionStatus()),
			categoryContains(subCategoryIds)
		);

		applySorting(pageable, auction, query);

		List<AuctionInfo.SearchAuction> results = query
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = getCountQuery(searchAuction, auction, subCategoryIds);

		return PageableExecutionUtils.getPage(results, pageable, countQuery::fetchOne);
	}

	@Override
	public Page<AuctionInfo.RetrieveMyAuctions> getMyAuctionList(
		Long memberId,
		Auction.AuctionStatus auctionStatus,
		Pageable pageable
	) {
		JPAQuery<AuctionInfo.RetrieveMyAuctions> query = queryFactory
			.select(new QAuctionInfo_RetrieveMyAuctions(
				auction.auctionId,
				auction.title,
				auction.auctionStatus,
				auction.nowPrice,
				auction.endDate,
				auction.thumbnailPath
			))
			.from(auction) // from 절 추가
			.where(auction.memberId.eq(memberId)
				.and(statusEquals(auctionStatus))); // statusEquals 메서드 확인

		applySorting(pageable, auction, query);

		// 페이징 적용
		List<AuctionInfo.RetrieveMyAuctions> auctions = query
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(auction.count())
			.from(auction) // from 절 추가
			.where(auction.memberId.eq(memberId) // countQuery에도 where 절 추가
				.and(statusEquals(auctionStatus))); // statusEquals 메서드 확인

		return PageableExecutionUtils.getPage(auctions, pageable, countQuery::fetchOne);
	}

	@Override
	public Page<AuctionInfo.RetrieveMyBidAuctions> getMyBidAuctionList(Long memberId, Pageable sortPage) {

		JPAQuery<Long> maxBidIds = queryFactory.select(bid.bidId.max())
			.from(bid)
			.where(bid.buyer.memberId.eq(memberId))
			.groupBy(bid.auction.auctionId);

		JPAQuery<AuctionInfo.RetrieveMyBidAuctions> query = queryFactory
			.select(new QAuctionInfo_RetrieveMyBidAuctions(
				auction.auctionId,
				auction.title,
				auction.auctionStatus,
				auction.thumbnailPath,
				Expressions.cases()
					.when(auction.auctionStatus.eq(Auction.AuctionStatus.BIDDING)).then("입찰중")
					.when(conclude.buyer.memberId.eq(memberId)
						.and(auction.auctionStatus.eq(Auction.AuctionStatus.CONCLUDED)
							.or(auction.auctionStatus.eq(Auction.AuctionStatus.COMPLETED)))).then("낙찰")
					.otherwise("패찰")
					.as("bidStatus")
			))
			.from(bid)
			.where(bid.bidId.in(maxBidIds))
			.leftJoin(auction).on(bid.auction.eq(auction))
			.leftJoin(conclude).on(auction.eq(conclude.auction));

		for (Sort.Order o : sortPage.getSort()) {
			PathBuilder<?> pathBuilder = new PathBuilder<>(bid.getType(), bid.getMetadata());
			query.orderBy(
				new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
		}

		// 페이징 적용
		List<AuctionInfo.RetrieveMyBidAuctions> bidAuctions = query.offset(sortPage.getOffset())
			.limit(sortPage.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(bid.auction.auctionId.countDistinct())
			.from(bid)
			.where(bid.buyer.memberId.eq(memberId));

		return PageableExecutionUtils.getPage(bidAuctions, sortPage, countQuery::fetchOne);
	}

	private static Expression<Boolean> isLiked(Long memberId) {
		return memberId != null ?
			new CaseBuilder()
				.when(likes.likesId.isNotNull()).then(true)
				.otherwise(false)
			: Expressions.constant(false);
	}

	private JPAQuery<Long> getCountQuery(
		AuctionCommand.SearchAuction searchAuction,
		QAuction auction,
		List<Long> subCategoryIds
	) {
		return queryFactory
			.select(auction.count())
			.from(auction)
			.where(
				titleContains(searchAuction.title()),
				statusEquals(searchAuction.auctionStatus()),
				categoryContains(subCategoryIds)
			);
	}

	private static <T> void applySorting(
		Pageable pageable,
		QAuction auction,
		JPAQuery<T> query
	) {

		// 우선적으로 auctionStatus 정렬 (BIDDING > NO_BIDS > CONCLUDED > COMPLETED)
		NumberExpression<Integer> statusOrder = new CaseBuilder()
			.when(auction.auctionStatus.eq(Auction.AuctionStatus.BIDDING)).then(1)
			.when(auction.auctionStatus.eq(Auction.AuctionStatus.NO_BIDS)).then(2)
			.when(auction.auctionStatus.eq(Auction.AuctionStatus.CONCLUDED)).then(3)
			.when(auction.auctionStatus.eq(Auction.AuctionStatus.COMPLETED)).then(4)
			.otherwise(5);

		query.orderBy(new OrderSpecifier<>(
			Order.ASC,
			statusOrder
		));

		for (Sort.Order o : pageable.getSort()) {
			if (o.getProperty().equalsIgnoreCase("auctionStatus")) {
				continue;
			}
			PathBuilder<?> pathBuilder = new PathBuilder<>(
				auction.getType(),
				auction.getMetadata()
			);
			query.orderBy(new OrderSpecifier(
				o.isAscending() ? Order.ASC : Order.DESC,
				pathBuilder.get(o.getProperty())
			));
		}
	}

	private BooleanExpression titleContains(String title) {
		return isEmpty(title) ? null : auction.title.containsIgnoreCase(title);
	}

	private BooleanExpression statusEquals(Auction.AuctionStatus auctionStatus) {
		return auctionStatus == null ? null : auction.auctionStatus.eq(auctionStatus);
	}

	private Predicate categoryContains(List<Long> categoryIds) {
		return categoryIds.isEmpty() ? null : category.categoryId.in(categoryIds);
	}

}
