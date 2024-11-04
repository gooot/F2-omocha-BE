package org.omocha.infra.repository;

import java.util.List;

import org.omocha.domain.auction.chat.ChatCommand;
import org.omocha.domain.auction.chat.ChatInfo;
import org.omocha.domain.auction.chat.QChat;
import org.omocha.domain.auction.chat.QChatInfo_ChatMessage;
import org.omocha.domain.member.QMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

public class ChatRepositoryImpl implements ChatRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public ChatRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}

	@Override
	public Slice<ChatInfo.ChatMessage> findChatMessagesByRoomId(
		ChatCommand.RetrieveChatRoomMessage retrieveMessage,
		Pageable pageable
	) {
		QChat chat = QChat.chat;
		QMember member = QMember.member;

		JPAQuery<ChatInfo.ChatMessage> query = queryFactory
			.select(new QChatInfo_ChatMessage(
				chat.messageType,
				chat.senderId,
				chat.roomId,
				member.nickname,
				member.profileImageUrl,
				chat.message,
				chat.createdAt
			))
			.from(chat)
			.leftJoin(member).on(member.memberId.eq(chat.senderId))
			.where(chat.roomId.eq(retrieveMessage.roomId())
				.and(retrieveMessage.cursor() != null ? chat.createdAt.lt(retrieveMessage.cursor()) : null));

		applySorting(pageable, chat, query);

		query.orderBy(chat.createdAt.desc());

		List<ChatInfo.ChatMessage> messages = query
			.limit(pageable.getPageSize() + 1)
			.fetch();

		boolean hasNext = messages.size() > pageable.getPageSize();

		if (hasNext) {
			messages.remove(pageable.getPageSize());
		}

		return new SliceImpl<>(messages, pageable, hasNext);
	}

	private static <T> void applySorting(
		Pageable pageable,
		QChat chat,
		JPAQuery<T> query
	) {
		for (Sort.Order o : pageable.getSort()) {
			PathBuilder<?> pathBuilder = new PathBuilder<>(
				chat.getType(),
				chat.getMetadata()
			);
			query.orderBy(new OrderSpecifier(
				o.isAscending() ? Order.ASC : Order.DESC,
				pathBuilder.get(o.getProperty())
			));
		}
	}
}
