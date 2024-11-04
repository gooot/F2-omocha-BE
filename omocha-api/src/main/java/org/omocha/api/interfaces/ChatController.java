package org.omocha.api.interfaces;

import org.omocha.api.application.ChatFacade;
import org.omocha.api.interfaces.dto.ChatDto;
import org.omocha.api.interfaces.mapper.ChatDtoMapper;
import org.omocha.domain.auction.chat.ChatCommand;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ChatController {

	private final ChatFacade chatFacade;
	private final ChatDtoMapper chatDtoMapper;

	@MessageMapping("/{roomId}/messages")
	public void chat(
		@DestinationVariable Long roomId,
		@Valid ChatDto.ChatMessageRequest chatMessageRequest
	) {

		ChatCommand.AddChatMessage chatCommand =
			chatDtoMapper.toCommand(chatMessageRequest, roomId);

		// 메시지 처리
		chatFacade.processChatMessage(chatCommand);

	}
}
