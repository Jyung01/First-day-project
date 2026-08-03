package kr.co.firstdayproject.repository.review;

import kr.co.firstdayproject.entity.review.CompanyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyReviewRepository extends JpaRepository<CompanyReview, Long> {
}
