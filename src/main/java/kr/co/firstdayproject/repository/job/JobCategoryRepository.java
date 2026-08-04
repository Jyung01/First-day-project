package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.JobCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {

    List<JobCategory> findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(
            Integer depth
    );

    List<JobCategory> findAllByJobCategoryIdInAndIsActiveTrueAndDepth(
            Collection<Long> jobCategoryIds,
            Integer depth
    );
}
