package kr.co.firstdayproject.repository.coverletter;

import java.util.List;
import kr.co.firstdayproject.entity.coverletter.CoverLetterItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverLetterItemRepository extends JpaRepository<CoverLetterItem, Long> {

    List<CoverLetterItem> findByCoverLetterIdOrderByDisplayOrderAsc(Long coverLetterId);
}