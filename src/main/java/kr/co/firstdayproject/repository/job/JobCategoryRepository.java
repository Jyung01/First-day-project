package kr.co.firstdayproject.repository.job;

import kr.co.firstdayproject.entity.job.JobCategory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobCategoryRepository
        extends JpaRepository<JobCategory, Long> {

    // 활성화된 특정 depth 카테고리 조회
    List<JobCategory>
        findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(
            Integer depth
        );

    // 선택한 카테고리 ID 중 활성화된 특정 depth 카테고리 조회
    List<JobCategory>
        findAllByJobCategoryIdInAndIsActiveTrueAndDepth(
            Collection<Long> jobCategoryIds,
            Integer depth
        );

    Optional<JobCategory> findByJobCategoryIdAndDepthAndIsActiveTrue(
        Long jobCategoryId,
        Integer depth
    );

    // 활성화된 전체 카테고리 조회
    List<JobCategory> findByIsActiveTrue(Sort sort);

    // 최상위 카테고리 전체 조회
    List<JobCategory> findByParentIdIsNull(Sort sort);

    // 활성화된 최상위 카테고리 조회
    List<JobCategory> findByParentIdIsNullAndIsActiveTrue(Sort sort);

    // 특정 상위 카테고리의 하위 카테고리 조회
    List<JobCategory> findByParentId(
        Long parentId,
        Sort sort
    );

    // 특정 상위 카테고리의 활성화된 하위 카테고리 조회
    List<JobCategory> findByParentIdAndIsActiveTrue(
        Long parentId,
        Sort sort
    );

    // 최상위 카테고리 이름으로 조회
    Optional<JobCategory> findByParentIdIsNullAndCategoryName(
        String categoryName
    );

    // 특정 상위 카테고리 아래에서 이름으로 조회
    Optional<JobCategory> findByParentIdAndCategoryName(
        Long parentId,
        String categoryName
    );

    // 활성화된 최상위 카테고리 중 마지막 순서 조회
    Optional<JobCategory>
        findFirstByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderDescJobCategoryIdDesc();

    // 특정 상위 카테고리 아래 활성화된 마지막 순서 조회
    Optional<JobCategory>
        findFirstByParentIdAndIsActiveTrueOrderByDisplayOrderDescJobCategoryIdDesc(
            Long parentId
        );

    // slug 중복 검사
    boolean existsBySlug(String slug);

    // 수정 중인 카테고리를 제외한 slug 중복 검사
    boolean existsBySlugAndJobCategoryIdNot(
        String slug,
        Long jobCategoryId
    );

    // 같은 상위 카테고리 내 이름 중복 검사
    boolean existsByParentIdAndCategoryName(
        Long parentId,
        String categoryName
    );
}
