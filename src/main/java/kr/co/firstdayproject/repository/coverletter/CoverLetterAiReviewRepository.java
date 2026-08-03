package kr.co.firstdayproject.repository.coverletter;

import kr.co.firstdayproject.entity.coverletter.CoverLetterAiReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverLetterAiReviewRepository extends JpaRepository<CoverLetterAiReview, Long> {
}
