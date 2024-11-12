package org.omocha.infra.repository;

import static org.omocha.domain.auction.QAuction.*;
import static org.omocha.domain.auction.QCategory.*;
import static org.springframework.util.ObjectUtils.*;

import java.util.List;

import org.omocha.domain.auction.Auction;
import org.omocha.domain.auction.AuctionCommand;
import org.omocha.domain.auction.AuctionInfo;
import org.omocha.domain.auction.QAuction;
import org.omocha.domain.auction.QAuctionCategory;
import org.omocha.domain.auction.QAuctionInfo_SearchAuction;
import org.omocha.domain.auction.QCategory;
import org.omocha.domain.auction.conclude.QConclude;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
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

		QAuction auction = QAuction.auction;
		QConclude conclude = QConclude.conclude;
		QAuctionCategory auctionCategory = QAuctionCategory.auctionCategory;
		QCategory category = QCategory.category;

		JPAQuery<AuctionInfo.SearchAuction> query = queryFactory
			.select(new QAuctionInfo_SearchAuction(
				auction.auctionId,
				auction.memberId,
				auction.title,
				auction.content,
				auction.startPrice,
				auction.bidUnit,
				auction.auctionStatus,
				auction.thumbnailPath,
				auction.nowPrice,
				conclude.concludePrice,
				auction.bidCount,
				auction.startDate,
				auction.endDate,
				auction.createdAt
			))
			.from(auction)
			.leftJoin(conclude).on(conclude.auction.eq(auction))
			.leftJoin(auctionCategory).on(auctionCategory.auction.eq(auction))
			.leftJoin(category).on(auctionCategory.category.eq(category))
			.where(
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

	// public Page<Auction> searchMyAuctionList(Long memberId, AuctionStatus auctionStatus, Pageable pageable) {
	//
	// 	JPAQuery<Auction> query = queryFactory
	// 		.selectFrom(auction)
	// 		.leftJoin(auction.images, image)
	// 		.where(auction.member.memberId.eq(memberId)
	// 			.and(statusEquals(auctionStatus)));
	//
	// 	for (Sort.Order o : pageable.getSort()) {
	// 		PathBuilder<?> pathBuilder = new PathBuilder<>(
	// 			auction.getType(),
	// 			auction.getMetadata()
	// 		);
	// 		query.orderBy(new OrderSpecifier(
	// 			o.isAscending() ? Order.ASC : Order.DESC,
	// 			pathBuilder.get(o.getProperty())
	// 		));
	// 	}
	//
	// 	// 페이징 적용
	// 	List<Auction> auctions = query
	// 		.offset(pageable.getOffset())
	// 		.limit(pageable.getPageSize())
	// 		.fetch();
	//
	// 	JPAQuery<Long> countQuery = queryFactory
	// 		.select(auction.count())
	// 		.from(auction);
	//
	// 	return PageableExecutionUtils.getPage(auctions, pageable, countQuery::fetchOne);
	// }

}
