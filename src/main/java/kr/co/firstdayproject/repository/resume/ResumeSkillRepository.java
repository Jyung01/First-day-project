package kr.co.firstdayproject.repository.resume;

import kr.co.firstdayproject.entity.resume.ResumeSkill;
import kr.co.firstdayproject.entity.resume.ResumeSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, ResumeSkillId> {

    long countByIdSkillId(Long skillId);
}
