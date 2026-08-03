package kr.co.firstdayproject.repository.review;

import kr.co.firstdayproject.entity.review.ReviewReaction;
import kr.co.firstdayproject.entity.review.ReviewReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, ReviewReactionId> {
}
