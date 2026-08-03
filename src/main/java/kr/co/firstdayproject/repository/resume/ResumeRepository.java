package kr.co.firstdayproject.repository.resume;

import kr.co.firstdayproject.entity.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
}
