package kr.co.firstdayproject.repository.coverletter;

import kr.co.firstdayproject.entity.coverletter.CoverLetterItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverLetterItemRepository extends JpaRepository<CoverLetterItem, Long> {
}
