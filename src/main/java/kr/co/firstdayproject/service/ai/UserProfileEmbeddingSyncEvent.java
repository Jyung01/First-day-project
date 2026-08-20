package kr.co.firstdayproject.service.ai;

/** 개인회원의 이력서 또는 자기소개서가 변경된 뒤 프로필 벡터를 갱신하는 이벤트다. */
public record UserProfileEmbeddingSyncEvent(Long userId) {
}
