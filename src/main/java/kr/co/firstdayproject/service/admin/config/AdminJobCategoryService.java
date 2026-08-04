package kr.co.firstdayproject.service.admin.config;

import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.UserDesiredJobRepository;
import java.util.Locale;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminJobCategoryService {

    private static final Sort CATEGORY_TREE_SORT = Sort.by(
        "depth",
        "parentId",
        "displayOrder",
        "jobCategoryId"
    );

    private static final Sort DISPLAY_ORDER_SORT = Sort.by(
        "displayOrder",
        "jobCategoryId"
    );

    private final JobCategoryRepository jobCategoryRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserDesiredJobRepository userDesiredJobRepository;

    public List<JobCategory> getJobCategories(boolean activeOnly) {
        if (activeOnly) {
            return jobCategoryRepository.findByIsActiveTrue(
                CATEGORY_TREE_SORT
            );
        }

        return jobCategoryRepository.findAll(CATEGORY_TREE_SORT);
    }

    public List<JobCategory> getRootJobCategories(boolean activeOnly) {
        if (activeOnly) {
            return jobCategoryRepository
                .findByParentIdIsNullAndIsActiveTrue(
                    DISPLAY_ORDER_SORT
                );
        }

        return jobCategoryRepository.findByParentIdIsNull(
            DISPLAY_ORDER_SORT
        );
    }

    public List<JobCategory> getChildJobCategories(
        Long parentId,
        boolean activeOnly
    ) {
        if (activeOnly) {
            return jobCategoryRepository.findByParentIdAndIsActiveTrue(
                parentId,
                DISPLAY_ORDER_SORT
            );
        }

        return jobCategoryRepository.findByParentId(
            parentId,
            DISPLAY_ORDER_SORT
        );
    }

    public long getJobCategoryUsageCount(Long jobCategoryId) {
        return jobPostingRepository.countByJobCategoryId(jobCategoryId)
            + userDesiredJobRepository.countByIdJobCategoryId(jobCategoryId);
    }

    public long getTotalJobCategoryUsageCount() {
        return jobPostingRepository.count()
            + userDesiredJobRepository.count();
    }

    @Transactional
    public JobCategory createJobCategory(
        String categoryName,
        Long parentId
    ) {
        String normalizedName = normalizeCategoryName(categoryName);
        JobCategory parent = validateParent(parentId);
        Optional<JobCategory> existingCategory = findExistingCategory(
            normalizedName,
            parentId
        );

        if (existingCategory.isPresent()) {
            throw new IllegalArgumentException(
                Boolean.TRUE.equals(existingCategory.get().getIsActive())
                    ? "이미 등록된 카테고리입니다."
                    : "삭제된 카테고리입니다. 복구 여부를 확인해 주세요."
            );
        }

        JobCategory category = JobCategory.builder()
            .parentId(parentId)
            .categoryName(normalizedName)
            .slug(createUniqueSlug(normalizedName, parentId, null))
            .depth(parent == null ? 1 : 2)
            .displayOrder(getNextDisplayOrder(parentId))
            .isActive(true)
            .build();

        return jobCategoryRepository.save(category);
    }

    public Optional<JobCategory> findRestorableJobCategory(
        String categoryName,
        Long parentId
    ) {
        String normalizedName = normalizeCategoryName(categoryName);

        return findExistingCategory(normalizedName, parentId)
            .filter(category -> !Boolean.TRUE.equals(category.getIsActive()));
    }

    @Transactional
    public JobCategory restoreJobCategory(Long jobCategoryId) {
        JobCategory category = jobCategoryRepository.findById(jobCategoryId)
            .orElseThrow(() -> new IllegalArgumentException(
                "복구할 카테고리를 찾을 수 없습니다."
            ));

        if (Boolean.TRUE.equals(category.getIsActive())) {
            throw new IllegalArgumentException(
                "이미 복구된 카테고리입니다."
            );
        }

        if (category.getDepth() == 2) {
            validateParent(category.getParentId());
        }

        category.setDisplayOrder(getNextDisplayOrder(category.getParentId()));
        category.setIsActive(true);
        return category;
    }

    @Transactional
    public JobCategory updateJobCategory(
        Long jobCategoryId,
        String categoryName,
        Long parentId
    ) {
        JobCategory category = jobCategoryRepository.findById(jobCategoryId)
            .orElseThrow(() -> new IllegalArgumentException(
                "수정할 카테고리를 찾을 수 없습니다."
            ));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new IllegalArgumentException(
                "비활성화된 카테고리는 수정할 수 없습니다."
            );
        }

        Long resolvedParentId = resolveUpdateParent(category, parentId);
        String normalizedName = normalizeCategoryName(categoryName);
        validateDuplicateName(
            category.getJobCategoryId(),
            normalizedName,
            resolvedParentId
        );

        boolean parentChanged = category.getDepth() == 2
            && !category.getParentId().equals(resolvedParentId);

        if (parentChanged
            && getJobCategoryUsageCount(category.getJobCategoryId()) > 0) {
            throw new IllegalArgumentException(
                "사용 중인 직무는 상위 카테고리를 변경할 수 없습니다."
            );
        }

        category.setCategoryName(normalizedName);
        category.setSlug(createUniqueSlug(
            normalizedName,
            resolvedParentId,
            category.getJobCategoryId()
        ));

        if (parentChanged) {
            category.setParentId(resolvedParentId);
            category.setDisplayOrder(getNextDisplayOrder(resolvedParentId));
        }

        return category;
    }

    @Transactional
    public JobCategory deleteJobCategory(Long jobCategoryId) {
        JobCategory category = jobCategoryRepository.findById(jobCategoryId)
            .orElseThrow(() -> new IllegalArgumentException(
                "삭제할 카테고리를 찾을 수 없습니다."
            ));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new IllegalArgumentException(
                "이미 삭제된 카테고리입니다."
            );
        }

        category.setIsActive(false);
        return category;
    }

    @Transactional
    public void reorderJobCategories(
        List<Long> orderedIds,
        Long parentId
    ) {
        if (parentId != null) {
            validateParent(parentId);
        }

        List<JobCategory> categories = parentId == null
            ? getRootJobCategories(true)
            : getChildJobCategories(parentId, true);

        validateReorderIds(
            orderedIds,
            categories.stream()
                .map(JobCategory::getJobCategoryId)
                .toList()
        );

        for (int index = 0; index < orderedIds.size(); index++) {
            Long categoryId = orderedIds.get(index);
            JobCategory category = categories.stream()
                .filter(item -> item.getJobCategoryId().equals(categoryId))
                .findFirst()
                .orElseThrow();

            category.setDisplayOrder(index + 1);
        }
    }

    private void validateReorderIds(
        List<Long> orderedIds,
        List<Long> expectedIds
    ) {
        if (orderedIds == null
            || orderedIds.size() != expectedIds.size()
            || new HashSet<>(orderedIds).size() != orderedIds.size()
            || !new HashSet<>(orderedIds).equals(new HashSet<>(expectedIds))) {
            throw new IllegalArgumentException(
                "정렬할 카테고리 목록이 올바르지 않습니다."
            );
        }
    }

    private Long resolveUpdateParent(
        JobCategory category,
        Long parentId
    ) {
        if (category.getDepth() == 1) {
            if (parentId != null) {
                throw new IllegalArgumentException(
                    "1차 카테고리에는 상위 카테고리를 지정할 수 없습니다."
                );
            }

            return null;
        }

        if (parentId == null) {
            throw new IllegalArgumentException(
                "상위 카테고리를 선택해주세요."
            );
        }

        validateParent(parentId);
        return parentId;
    }

    private void validateDuplicateName(
        Long jobCategoryId,
        String categoryName,
        Long parentId
    ) {
        Optional<JobCategory> duplicate = findExistingCategory(
            categoryName,
            parentId
        );

        if (duplicate.isPresent()
            && !duplicate.get().getJobCategoryId().equals(jobCategoryId)) {
            throw new IllegalArgumentException(
                "이미 등록된 카테고리입니다."
            );
        }
    }

    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("카테고리명을 입력해주세요.");
        }

        String normalizedName = categoryName.strip().replaceAll("\\s+", " ");

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                "카테고리명은 100자 이하로 입력해주세요."
            );
        }

        return normalizedName;
    }

    private JobCategory validateParent(Long parentId) {
        if (parentId == null) {
            return null;
        }

        JobCategory parent = jobCategoryRepository.findById(parentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "선택한 1차 카테고리를 찾을 수 없습니다."
            ));

        if (parent.getDepth() != 1 || !Boolean.TRUE.equals(parent.getIsActive())) {
            throw new IllegalArgumentException(
                "활성화된 1차 카테고리만 선택할 수 있습니다."
            );
        }

        return parent;
    }

    private Optional<JobCategory> findExistingCategory(
        String categoryName,
        Long parentId
    ) {
        if (parentId == null) {
            return jobCategoryRepository
                .findByParentIdIsNullAndCategoryName(categoryName);
        }

        return jobCategoryRepository.findByParentIdAndCategoryName(
            parentId,
            categoryName
        );
    }

    private int getNextDisplayOrder(Long parentId) {
        Optional<JobCategory> lastCategory;

        if (parentId == null) {
            lastCategory = jobCategoryRepository
                .findFirstByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderDescJobCategoryIdDesc();
        } else {
            lastCategory = jobCategoryRepository
                .findFirstByParentIdAndIsActiveTrueOrderByDisplayOrderDescJobCategoryIdDesc(
                    parentId
                );
        }

        return lastCategory
            .map(JobCategory::getDisplayOrder)
            .orElse(0) + 1;
    }

    private String createUniqueSlug(
        String categoryName,
        Long parentId,
        Long excludedCategoryId
    ) {
        String slug = categoryName
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", "-")
            .replaceAll("(^-+|-+$)", "");

        if (slug.isBlank()) {
            slug = "category";
        }

        String candidate = parentId == null
            ? slug
            : slug + "-" + parentId;

        boolean slugExists = excludedCategoryId == null
            ? jobCategoryRepository.existsBySlug(candidate)
            : jobCategoryRepository.existsBySlugAndJobCategoryIdNot(
                candidate,
                excludedCategoryId
            );

        if (!slugExists) {
            return candidate;
        }

        return candidate + "-" + UUID.randomUUID()
            .toString()
            .substring(0, 8);
    }
}
