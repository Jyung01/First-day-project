package kr.co.firstdayproject.controller.admin.config;

import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.service.admin.config.AdminJobCategoryService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/config/job-category")
@RequiredArgsConstructor
public class AdminJobCategoryController {

    private final AdminJobCategoryService adminJobCategoryService;

    @GetMapping
    public String jobCategory(
        @RequestParam(required = false) Long categoryId,
        Model model
    ) {
        List<JobCategory> rootCategories = adminJobCategoryService
            .getRootJobCategories(true);
        JobCategory selectedRootCategory = findSelectedRootCategory(
            rootCategories,
            categoryId
        );
        List<JobCategory> childCategories = getSelectedChildCategories(
            selectedRootCategory
        );
        List<JobCategory> activeChildCategories = adminJobCategoryService
            .getJobCategories(true)
            .stream()
            .filter(category -> category.getDepth() == 2)
            .filter(category -> rootCategories.stream().anyMatch(root ->
                root.getJobCategoryId().equals(category.getParentId())
            ))
            .toList();

        model.addAttribute("activeMenu", "jobConfig");
        model.addAttribute("rootCategories", rootCategories);
        model.addAttribute("selectedRootCategory", selectedRootCategory);
        model.addAttribute("childCategories", childCategories);
        model.addAttribute(
            "childCountsByRoot",
            createChildCountsByRoot(activeChildCategories)
        );
        model.addAttribute(
            "jobCategoryUsageCounts",
            createJobCategoryUsageCounts(childCategories)
        );
        model.addAttribute(
            "activeChildCategoryCount",
            activeChildCategories.size()
        );
        model.addAttribute(
            "totalJobCategoryUsageCount",
            adminJobCategoryService.getTotalJobCategoryUsageCount()
        );

        return "admin/config/job-category";
    }

    @PostMapping
    public String createJobCategory(
        @RequestParam String categoryType,
        @RequestParam String categoryName,
        @RequestParam(required = false) Long parentId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            validateCategoryType(categoryType, parentId);
            Long resolvedParentId = "PRIMARY".equals(categoryType)
                ? null
                : parentId;
            Optional<JobCategory> restorableCategory = adminJobCategoryService
                .findRestorableJobCategory(
                    categoryName,
                    resolvedParentId
                );

            if (restorableCategory.isPresent()) {
                addRestoreModalAttributes(
                    redirectAttributes,
                    restorableCategory.get()
                );

                if (resolvedParentId != null) {
                    redirectAttributes.addAttribute(
                        "categoryId",
                        resolvedParentId
                    );
                }

                return "redirect:/admin/config/job-category";
            }

            JobCategory category = adminJobCategoryService
                .createJobCategory(categoryName, resolvedParentId);

            redirectAttributes.addFlashAttribute(
                "configMessage",
                "카테고리가 등록되었습니다."
            );

            if (category.getParentId() != null) {
                redirectAttributes.addAttribute(
                    "categoryId",
                    category.getParentId()
                );
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/job-category";
    }

    @PostMapping("/restore")
    public String restoreJobCategory(
        @RequestParam Long categoryId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            JobCategory category = adminJobCategoryService
                .restoreJobCategory(categoryId);

            redirectAttributes.addFlashAttribute(
                "configMessage",
                "카테고리가 복구되었습니다."
            );

            redirectAttributes.addAttribute(
                "categoryId",
                category.getDepth() == 1
                    ? category.getJobCategoryId()
                    : category.getParentId()
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/job-category";
    }

    @PostMapping("/edit")
    public String updateJobCategory(
        @RequestParam Long categoryId,
        @RequestParam String categoryName,
        @RequestParam(required = false) Long parentId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            JobCategory category = adminJobCategoryService
                .updateJobCategory(categoryId, categoryName, parentId);

            redirectAttributes.addFlashAttribute(
                "configMessage",
                "카테고리가 수정되었습니다."
            );

            redirectAttributes.addAttribute(
                "categoryId",
                category.getDepth() == 1
                    ? category.getJobCategoryId()
                    : category.getParentId()
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/job-category";
    }

    @PostMapping("/delete")
    public String deleteJobCategory(
        @RequestParam Long categoryId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            JobCategory category = adminJobCategoryService
                .deleteJobCategory(categoryId);

            redirectAttributes.addFlashAttribute(
                "configMessage",
                category.getDepth() == 1
                    ? "1차 카테고리가 삭제되었습니다."
                    : "하위 직무가 삭제되었습니다."
            );

            if (category.getParentId() != null) {
                redirectAttributes.addAttribute(
                    "categoryId",
                    category.getParentId()
                );
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/job-category";
    }

    @PostMapping("/reorder")
    public String reorderJobCategories(
        @RequestParam List<Long> orderedIds,
        @RequestParam(required = false) Long parentId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            adminJobCategoryService.reorderJobCategories(
                orderedIds,
                parentId
            );
            redirectAttributes.addFlashAttribute(
                "configMessage",
                "카테고리 순서가 저장되었습니다."
            );

            if (parentId != null) {
                redirectAttributes.addAttribute("categoryId", parentId);
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/job-category";
    }

    private void validateCategoryType(String categoryType, Long parentId) {
        if (!"PRIMARY".equals(categoryType)
            && !"SECONDARY".equals(categoryType)) {
            throw new IllegalArgumentException(
                "카테고리 구분을 확인해주세요."
            );
        }

        if ("SECONDARY".equals(categoryType) && parentId == null) {
            throw new IllegalArgumentException(
                "상위 카테고리를 선택해주세요."
            );
        }
    }

    private void addRestoreModalAttributes(
        RedirectAttributes redirectAttributes,
        JobCategory category
    ) {
        redirectAttributes.addFlashAttribute(
            "restoreCategoryId",
            category.getJobCategoryId()
        );
        redirectAttributes.addFlashAttribute(
            "restoreCategoryName",
            category.getCategoryName()
        );
        redirectAttributes.addFlashAttribute(
            "restoreCategoryDepth",
            category.getDepth()
        );
    }

    private JobCategory findSelectedRootCategory(
        List<JobCategory> rootCategories,
        Long categoryId
    ) {
        if (rootCategories.isEmpty()) {
            return null;
        }

        if (categoryId == null) {
            return rootCategories.getFirst();
        }

        return rootCategories.stream()
            .filter(category ->
                category.getJobCategoryId().equals(categoryId)
            )
            .findFirst()
            .orElse(rootCategories.getFirst());
    }

    private List<JobCategory> getSelectedChildCategories(
        JobCategory selectedRootCategory
    ) {
        if (selectedRootCategory == null) {
            return Collections.emptyList();
        }

        return adminJobCategoryService.getChildJobCategories(
            selectedRootCategory.getJobCategoryId(),
            true
        );
    }

    private Map<Long, Long> createChildCountsByRoot(
        List<JobCategory> childCategories
    ) {
        Map<Long, Long> childCounts = new LinkedHashMap<>();

        for (JobCategory category : childCategories) {
            childCounts.merge(category.getParentId(), 1L, Long::sum);
        }

        return childCounts;
    }

    private Map<Long, Long> createJobCategoryUsageCounts(
        List<JobCategory> categories
    ) {
        Map<Long, Long> usageCounts = new LinkedHashMap<>();

        for (JobCategory category : categories) {
            usageCounts.put(
                category.getJobCategoryId(),
                adminJobCategoryService.getJobCategoryUsageCount(
                    category.getJobCategoryId()
                )
            );
        }

        return usageCounts;
    }
}
