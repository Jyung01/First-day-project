package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
}
