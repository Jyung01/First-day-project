package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.Skill;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByIsActiveTrue(Sort sort);

    List<Skill> findByDepth(Integer depth, Sort sort);

    List<Skill> findByDepthAndIsActiveTrue(
        Integer depth,
        Sort sort
    );

    List<Skill> findByParentId(Long parentId, Sort sort);

    List<Skill> findByParentIdAndIsActiveTrue(
        Long parentId,
        Sort sort
    );

    List<Skill> findAllBySkillIdInAndDepthAndIsActiveTrue(
        Collection<Long> skillIds,
        Integer depth
    );

    Optional<Skill> findByDepthAndSkillName(
        Integer depth,
        String skillName
    );

    Optional<Skill> findByParentIdAndSkillName(
        Long parentId,
        String skillName
    );

    Optional<Skill>
        findFirstByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderDescSkillIdDesc();

    Optional<Skill>
        findFirstByParentIdAndIsActiveTrueOrderByDisplayOrderDescSkillIdDesc(
            Long parentId
        );

    boolean existsBySlug(String slug);

    boolean existsBySlugAndSkillIdNot(String slug, Long skillId);
}
