package kr.co.firstdayproject.repository.resume;

import java.util.List;
import kr.co.firstdayproject.entity.resume.ResumeCareer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeCareerRepository extends JpaRepository<ResumeCareer, Long> {

    List<ResumeCareer> findByResumeIdOrderByDisplayOrderAsc(Long resumeId);
}
