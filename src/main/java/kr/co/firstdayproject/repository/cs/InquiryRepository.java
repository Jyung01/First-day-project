package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 기존 InquiryRepository에 동적 검색(JpaSpecificationExecutor)을 추가한 버전입니다.
 * 실제 프로젝트의 InquiryRepository.java를 이 내용으로 교체(또는 병합)해주세요.
 */
@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long>, JpaSpecificationExecutor<Inquiry> {
}