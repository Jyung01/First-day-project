package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    // 관리자 목록 (카테고리 전체 + 검색어 없음)
    Page<Faq> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 관리자 목록 (카테고리 필터만)
    Page<Faq> findByFaqCategoryIdOrderByCreatedAtDesc(Long faqCategoryId, Pageable pageable);

    // 관리자 목록 (검색어만)
    Page<Faq> findByQuestionContainingOrderByCreatedAtDesc(String keyword, Pageable pageable);

    // 관리자 목록 (카테고리 필터 + 검색어)
    Page<Faq> findByFaqCategoryIdAndQuestionContainingOrderByCreatedAtDesc(Long faqCategoryId, String keyword, Pageable pageable);

    // 사용자 노출용 (공개 상태만)
    List<Faq> findByStatusOrderByCreatedAtDesc(String status);

    List<Faq> findByStatusAndFaqCategoryIdOrderByCreatedAtDesc(String status, Long faqCategoryId);
}