package kr.co.firstdayproject.repository.resume;

import java.util.List;
import kr.co.firstdayproject.entity.resume.ResumeEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeEducationRepository extends JpaRepository<ResumeEducation, Long> {

    List<ResumeEducation> findByResumeIdOrderByDisplayOrderAsc(Long resumeId);
}
