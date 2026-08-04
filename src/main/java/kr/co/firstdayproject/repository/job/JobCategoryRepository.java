package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.JobCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {

    List<JobCategory> findByIsActiveTrue(Sort sort);

    List<JobCategory> findByParentIdIsNull(Sort sort);

    List<JobCategory> findByParentIdIsNullAndIsActiveTrue(Sort sort);

    List<JobCategory> findByParentId(Long parentId, Sort sort);

    List<JobCategory> findByParentIdAndIsActiveTrue(
        Long parentId,
        Sort sort
    );

    Optional<JobCategory> findByParentIdIsNullAndCategoryName(
        String categoryName
    );

    Optional<JobCategory> findByParentIdAndCategoryName(
        Long parentId,
        String categoryName
    );

    Optional<JobCategory>
        findFirstByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderDescJobCategoryIdDesc();

    Optional<JobCategory>
        findFirstByParentIdAndIsActiveTrueOrderByDisplayOrderDescJobCategoryIdDesc(
            Long parentId
        );

    boolean existsBySlug(String slug);

    boolean existsBySlugAndJobCategoryIdNot(
        String slug,
        Long jobCategoryId
    );

    boolean existsByParentIdAndCategoryName(
        Long parentId,
        String categoryName
    );
}
