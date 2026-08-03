package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Long> {
}
