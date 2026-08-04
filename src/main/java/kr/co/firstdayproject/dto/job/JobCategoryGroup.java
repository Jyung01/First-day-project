package kr.co.firstdayproject.dto.job;

import java.util.List;

public record JobCategoryGroup(
        Long jobCategoryId,
        String categoryName,
        List<JobCategoryOption> children
) {

    public JobCategoryGroup {
        children = List.copyOf(children);
    }
}
