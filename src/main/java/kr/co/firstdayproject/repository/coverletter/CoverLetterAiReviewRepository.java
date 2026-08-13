package kr.co.firstdayproject.repository.coverletter;

import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.entity.coverletter.CoverLetterAiReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverLetterAiReviewRepository extends JpaRepository<CoverLetterAiReview, Long> {

    boolean existsByCoverLetterId(Long coverLetterId);

    Optional<CoverLetterAiReview> findFirstByCoverLetterIdOrderByCreatedAtDesc(
        Long coverLetterId
    );

    List<CoverLetterAiReview> findByCoverLetterIdOrderByCreatedAtDesc(
        Long coverLetterId
    );
}
