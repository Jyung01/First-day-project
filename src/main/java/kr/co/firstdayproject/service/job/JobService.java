package kr.co.firstdayproject.service.job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private final JobCategoryRepository jobCategoryRepository;

    public List<JobCategoryGroup> getActiveJobCategoryGroups() {
        List<JobCategory> parents = jobCategoryRepository
                .findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(1);
        List<JobCategory> children = jobCategoryRepository
                .findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(2);

        Map<Long, List<JobCategoryOption>> childrenByParentId = new LinkedHashMap<>();
        for (JobCategory child : children) {
            if (child.getParentId() == null) {
                continue;
            }
            childrenByParentId
                    .computeIfAbsent(child.getParentId(), ignored -> new ArrayList<>())
                    .add(new JobCategoryOption(
                            child.getJobCategoryId(),
                            child.getCategoryName()
                    ));
        }

        return parents.stream()
                .map(parent -> new JobCategoryGroup(
                        parent.getJobCategoryId(),
                        parent.getCategoryName(),
                        childrenByParentId.getOrDefault(
                                parent.getJobCategoryId(),
                                List.of()
                        )
                ))
                .filter(group -> !group.children().isEmpty())
                .toList();
    }
}
