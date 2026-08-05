package kr.co.firstdayproject.repository.coverletter;

import java.util.List;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {

    List<CoverLetter> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
}