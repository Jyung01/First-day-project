package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
}
