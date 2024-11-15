package org.omocha.api.interfaces.dto;

import org.omocha.domain.member.Role;

public class MemberDto {

	public record CurrentMemberInfoResponse(
		// TODO : 회원 가입 정보 추가 후 변경
		Long memberId,
		String email,
		String userName,
		String nickName,
		String phoneNumber,
		String birth,
		Role role,
		String profileImageUrl
	) {
	}

	public record MemberModifyRequest(
		String nickName,
		String phoneNumber
	) {
	}

	public record MemberModifyResponse(
		// TODO : 회원 가입 정보 추가 후 변경
		Long memberId,
		String email,
		String userName,
		String nickName,
		String phoneNumber,
		String birth,
		Role role,
		String profileImageUrl
	) {
	}

	public record PasswordModifyRequest(
		String currentPassword,
		String newPassword
	) {
	}

	public record ProfileImageModifyResponse(
		String imageUrl
	) {

	}

}
