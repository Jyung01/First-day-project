package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.JobPostingSkill;
import kr.co.firstdayproject.entity.job.JobPostingSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingSkillRepository extends JpaRepository<JobPostingSkill, JobPostingSkillId> {
}
