package kr.co.firstdayproject.service.corp;

import java.util.List;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.Skill;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpJobFormService {

    private static final int CHILD_DEPTH = 2;

    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;

    public List<JobCategoryGroup> getActiveJobCategoryGroups() {
        List<JobCategory> categories = jobCategoryRepository
            .findByIsActiveTrue(Sort.by(
                "depth",
                "parentId",
                "displayOrder",
                "jobCategoryId"
            ));

        return categories.stream()
            .filter(category -> category.getDepth() == 1)
            .map(parent -> new JobCategoryGroup(
                parent.getJobCategoryId(),
                parent.getCategoryName(),
                categories.stream()
                    .filter(child -> child.getDepth() == CHILD_DEPTH)
                    .filter(child -> parent.getJobCategoryId().equals(
                        child.getParentId()
                    ))
                    .map(child -> new JobCategoryOption(
                        child.getJobCategoryId(),
                        child.getCategoryName()
                    ))
                    .toList()
            ))
            .toList();
    }

    public List<Skill> getActiveSkills() {
        return skillRepository.findByIsActiveTrue(Sort.by(
            "depth",
            "parentId",
            "displayOrder",
            "skillId"
        ));
    }
}
