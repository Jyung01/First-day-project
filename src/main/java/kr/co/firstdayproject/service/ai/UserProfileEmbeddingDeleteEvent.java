package kr.co.firstdayproject.service.ai;

/**
 * 회원 탈퇴로 개인 프로필 임베딩을 pgvector에서 제거해야 할 때 발행한다.
 * 갱신용 {@link UserProfileEmbeddingSyncEvent}와 달리 재생성을 하지 않는다.
 */
public record UserProfileEmbeddingDeleteEvent(Long userId) {
}
