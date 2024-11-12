package org.omocha.domain.auction.review;

import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;

public class ReviewInfo {

	public record RetrieveReviews(
		Long memberId,
		String memberNickname,
		Long auctionId,
		String auctionTitle,
		String auctionThumbnailPath,
		Review.ReviewType reviewType,
		Rating rating,
		String content,
		LocalDateTime createAt
	) {
		@QueryProjection
		public RetrieveReviews(
			Long memberId,
			String memberNickname,
			Long auctionId,
			String auctionTitle,
			String auctionThumbnailPath,
			Review.ReviewType reviewType,
			Rating rating,
			String content,
			LocalDateTime createAt
		) {
			this.memberId = memberId;
			this.memberNickname = memberNickname;
			this.auctionId = auctionId;
			this.auctionTitle = auctionTitle;
			this.auctionThumbnailPath = auctionThumbnailPath;
			this.reviewType = reviewType;
			this.rating = rating;
			this.content = content;
			this.createAt = createAt;
		}
	}
}
