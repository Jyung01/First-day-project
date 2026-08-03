package kr.co.firstdayproject.repository.coverletter;

import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {
}
