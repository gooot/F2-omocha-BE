package org.omocha.domain.exception;

import org.omocha.domain.exception.code.ErrorCode;

public class ChatRoomAlreadyExistException extends ChatException {
	public ChatRoomAlreadyExistException(Long auctionId) {
		super(
			ErrorCode.CHATROOM_ALREADY_EXISTS,
			"채팅방이 이미 존재 합니다. auctionId: " + auctionId
		);
	}
}
