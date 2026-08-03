package kr.co.firstdayproject.repository.cs;

import kr.co.firstdayproject.entity.cs.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
