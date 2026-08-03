package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
