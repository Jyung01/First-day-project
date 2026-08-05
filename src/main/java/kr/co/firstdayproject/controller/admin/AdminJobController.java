package kr.co.firstdayproject.controller.admin;

import java.util.Locale;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.AdminJobService;
import kr.co.firstdayproject.service.admin.AdminJobModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/job")
public class AdminJobController {

    private final AdminJobService adminJobService;
    private final AdminJobModerationService adminJobModerationService;

    @GetMapping({"", "/list"})
    public String list(
        @RequestParam(defaultValue = "ALL") String status,
        @RequestParam(required = false) Long parentCategoryId,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        String selectedStatus = status.toUpperCase(Locale.ROOT);

        model.addAttribute("activeMenu", "job");
        model.addAttribute(
            "jobPage",
            adminJobService.getJobPostings(
                selectedStatus,
                parentCategoryId,
                categoryId,
                keyword,
                page
            )
        );
        model.addAttribute(
            "jobCategoryGroups",
            adminJobService.getJobCategoryGroups()
        );
        model.addAttribute("statistics", adminJobService.getStatistics());
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("selectedParentCategoryId", parentCategoryId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        return "admin/job/index";
    }

    @GetMapping("/{jobId}")
    public String detail(
        @PathVariable Long jobId,
        Model model
    ) {
        model.addAttribute("activeMenu", "job");
        model.addAttribute("job", adminJobService.getJobPosting(jobId));

        return "admin/job/detail";
    }

    @PostMapping("/{jobId}/hide")
    public String hideJobPosting(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long jobId,
        @RequestParam String reason,
        RedirectAttributes redirectAttributes
    ) {
        try {
            adminJobModerationService.hideJobPosting(
                userDetails == null ? null : userDetails.getUserId(),
                jobId,
                reason
            );
            redirectAttributes.addFlashAttribute(
                "operationMessage",
                "채용공고를 숨김 처리했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "operationError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/job/" + jobId;
    }

    @PostMapping("/{jobId}/keep-hidden")
    public String keepJobPostingHidden(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long jobId,
        @RequestParam String reason,
        RedirectAttributes redirectAttributes
    ) {
        try {
            adminJobModerationService.keepJobPostingHidden(
                userDetails == null ? null : userDetails.getUserId(),
                jobId,
                reason
            );
            redirectAttributes.addFlashAttribute(
                "operationMessage",
                "채용공고의 숨김 상태를 유지했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "operationError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/job/" + jobId;
    }

    @PostMapping("/{jobId}/release-hidden")
    public String releaseJobPosting(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long jobId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            String status = adminJobModerationService.releaseJobPosting(
                userDetails == null ? null : userDetails.getUserId(),
                jobId
            );
            redirectAttributes.addFlashAttribute(
                "operationMessage",
                "마감".equals(status)
                    ? "숨김은 해제했지만 마감일이 지나 공고를 마감했습니다."
                    : "채용공고의 숨김을 해제했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "operationError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/job/" + jobId;
    }
}
