package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.InquiryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryCategoryRepository extends JpaRepository<InquiryCategory, Long> {
}
