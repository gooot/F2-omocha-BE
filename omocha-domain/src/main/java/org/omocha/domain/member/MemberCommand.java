package org.omocha.domain.member;

import org.omocha.domain.common.Role;
import org.omocha.domain.review.rating.Rating;
import org.springframework.web.multipart.MultipartFile;

import lombok.Builder;

public class MemberCommand {

	public record AddMember(
		String email,
		String password
	) {
		// TODO : 사용자 정보 확정 후 추가 수정 필요
		public Member toEntity(RandomNickNameGenerator randomNickNameGenerator) {
			return Member.builder()
				.email(email)
				.password(password)
				.averageRating(new Rating(0.0))
				.nickname(randomNickNameGenerator.generateRandomNickname())
				.role(Role.ROLE_USER)
				.memberStatus(Member.MemberStatus.ACTIVATE)
				.build();
		}
	}

	public record MemberLogin(
		String email,
		String password
	) {

	}

	@Builder
	public record OAuthProvider(
		String provider,
		String providerId
	) {

	}

	public record ModifyBasicInfo(
		Long memberId,
		String nickName,
		String phoneNumber
	) {
	}

	public record ModifyPassword(
		Long memberId,
		String currentPassword,
		String newPassword
	) {
	}

	public record ModifyProfileImage(
		Long memberId,
		MultipartFile profileImage
	) {

	}
}
