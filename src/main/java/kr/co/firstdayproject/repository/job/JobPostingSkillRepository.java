package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.JobPostingSkill;
import kr.co.firstdayproject.entity.job.JobPostingSkillId;
import java.util.List;
import kr.co.firstdayproject.dto.job.JobListSkillItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingSkillRepository extends JpaRepository<JobPostingSkill, JobPostingSkillId> {

    long countByIdSkillId(Long skillId);

    List<JobPostingSkill> findAllByIdJobPostingId(Long jobPostingId);

    List<JobPostingSkill> findAllByIdJobPostingIdIn(List<Long> jobPostingIds);

    @Query("""
            select new kr.co.firstdayproject.dto.job.JobListSkillItem(
                    postingSkill.id.jobPostingId,
                    skill.skillName
            )
              from JobPostingSkill postingSkill
              join Skill skill on skill.skillId = postingSkill.id.skillId
             where postingSkill.id.jobPostingId in :jobPostingIds
             order by postingSkill.id.jobPostingId,
                      skill.displayOrder,
                      skill.skillId
            """)
    List<JobListSkillItem> findListSkillsByJobPostingIds(
            @Param("jobPostingIds") List<Long> jobPostingIds
    );

    void deleteByIdJobPostingId(Long jobPostingId);
}
