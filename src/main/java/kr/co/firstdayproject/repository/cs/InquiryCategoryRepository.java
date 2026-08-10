package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.InquiryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 기존 InquiryCategoryRepository에 노출용 카테고리 목록 조회 메서드를 추가한 버전입니다.
 */
@Repository
public interface InquiryCategoryRepository extends JpaRepository<InquiryCategory, Long> {

    List<InquiryCategory> findByIsActiveTrueOrderByDisplayOrderAsc();
}