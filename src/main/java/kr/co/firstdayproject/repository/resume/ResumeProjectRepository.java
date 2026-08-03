package kr.co.firstdayproject.repository.resume;

import kr.co.firstdayproject.entity.resume.ResumeProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeProjectRepository extends JpaRepository<ResumeProject, Long> {
}
