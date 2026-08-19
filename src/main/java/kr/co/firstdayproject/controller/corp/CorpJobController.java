package kr.co.firstdayproject.controller.corp;

import jakarta.validation.Valid;
import java.util.Locale;
import kr.co.firstdayproject.dto.corp.job.JobPostingCreateRequest;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.corp.CorpJobQueryService;
import kr.co.firstdayproject.service.corp.CorpJobService;
import kr.co.firstdayproject.service.corp.CorpJobManagementService;
import kr.co.firstdayproject.service.corp.CorpJobFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/corp/job")
public class CorpJobController {

    private final CorpJobService corpJobService;
    private final CorpJobQueryService corpJobQueryService;
    private final CorpJobManagementService corpJobManagementService;
    private final CorpJobFormService corpJobFormService;

    @GetMapping({"", "/list"})
    public String list(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(defaultValue = "ALL") String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        prepare(model);
        String selectedStatus = status.toUpperCase(Locale.ROOT);
        Long companyId = userDetails == null
            ? null
            : userDetails.getCompanyId();

        model.addAttribute(
            "jobPage",
            corpJobQueryService.getJobPostings(
                companyId,
                selectedStatus,
                keyword,
                page
            )
        );
        model.addAttribute(
            "statusCounts",
            corpJobQueryService.getJobStatusCounts(companyId)
        );
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("keyword", keyword);
        return "corp/job-list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        prepare(model);
        prepareCreateForm(model);
        model.addAttribute("isEdit", false);
        model.addAttribute("isHiddenEdit", false);
        model.addAttribute(
            "jobPostingCreateRequest",
            new JobPostingCreateRequest()
        );
        return "corp/job-create";
    }

    @GetMapping("/{jobPostingId}")
    public String closedJobDetail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long jobPostingId,
        Model model
    ) {
        prepare(model);
        Long companyId = userDetails == null
            ? null
            : userDetails.getCompanyId();

        var job = corpJobQueryService.getReadOnlyJobPosting(
            companyId,
            jobPostingId
        );
        model.addAttribute("job", job);
        model.addAttribute("isReadOnly", true);
        model.addAttribute("isHidden", false);
        model.addAttribute(
            "isReviewReadOnly",
            "재검토요청".equals(job.status())
        );
        return "corp/job-edit";
    }

    @PostMapping("/create")
    public String createJobPosting(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("jobPostingCreateRequest")
        JobPostingCreateRequest request,
        BindingResult bindingResult,
        Model model
    ) {
        prepare(model);
        prepareCreateForm(model);
        model.addAttribute("isEdit", false);
        model.addAttribute("isHiddenEdit", false);

        if (bindingResult.hasErrors()) {
            return "corp/job-create";
        }

        try {
            corpJobService.createJobPosting(
                userDetails == null ? null : userDetails.getCompanyId(),
                request
            );
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            return "corp/job-create";
        }

        return "redirect:/corp/job?created=true";
    }

    @GetMapping("/edit")
    public String edit(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam Long id,
        Model model
    ) {
        prepare(model);
        prepareCreateForm(model);
        Long companyId = userDetails == null
            ? null
            : userDetails.getCompanyId();

        JobPostingCreateRequest request = corpJobQueryService
            .getEditableJobPosting(companyId, id);
        model.addAttribute("jobPostingCreateRequest", request);
        prepareGeneralEditState(model, id, request, companyId);
        return "corp/job-create";
    }

    @PostMapping("/{jobPostingId}/edit")
    public String updateJobPosting(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long jobPostingId,
        @Valid @ModelAttribute("jobPostingCreateRequest")
        JobPostingCreateRequest request,
        BindingResult bindingResult,
        Model model
    ) {
        prepare(model);
        prepareCreateForm(model);
        Long companyId = userDetails == null
            ? null
            : userDetails.getCompanyId();
        prepareGeneralEditState(
            model,
            jobPostingId,
            request,
            companyId
        );

        if (bindingResult.hasErrors()) {
            return "corp/job-create";
        }

        try {
            corpJobService.updateJobPosting(
                companyId,
                jobPostingId,
                request
            );
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            return "corp/job-create";
        }

        return "redirect:/corp/job?updated=true";
    }

    @PostMapping("/{jobPostingId}/close")
    public String closeJobPosting(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long jobPostingId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            corpJobManagementService.closeJobPosting(
                userDetails == null ? null : userDetails.getCompanyId(),
                jobPostingId
            );
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "채용공고를 마감했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                exception.getMessage()
            );
        }

        return "redirect:/corp/job";
    }

    @PostMapping("/{jobPostingId}/delete")
    public String deleteJobPosting(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long jobPostingId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            corpJobManagementService.deleteJobPosting(
                userDetails == null ? null : userDetails.getCompanyId(),
                jobPostingId
            );
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "채용공고를 삭제했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                exception.getMessage()
            );
        }

        return "redirect:/corp/job";
    }

    @GetMapping("/hidden-edit")
    public String hiddenEdit(Model model) {
        prepare(model);
        prepareEditState(model, true);
        return "corp/job-edit";
    }

    private void prepare(Model model) {
        model.addAttribute("activeMenu", "jobs");
        model.addAttribute("companyStatus", "APPROVED");
    }

    private void prepareCreateForm(Model model) {
        model.addAttribute(
            "jobCategoryGroups",
            corpJobFormService.getActiveJobCategoryGroups()
        );
        model.addAttribute(
            "activeSkills",
            corpJobFormService.getActiveSkills()
        );
    }

    private void prepareEditState(Model model, boolean hidden) {
        model.addAttribute("isReadOnly", false);
        model.addAttribute("isHidden", hidden);
        if (hidden) {
            model.addAttribute("hiddenReason", "회사 정보와 공고 내용 불일치");
        }
    }

    private void prepareGeneralEditState(
        Model model,
        Long jobPostingId,
        JobPostingCreateRequest request,
        Long companyId
    ) {
        model.addAttribute("isEdit", true);
        model.addAttribute("jobPostingId", jobPostingId);
        model.addAttribute(
            "isDraftEdit",
            "DRAFT".equals(request.getSubmitType())
        );
        boolean hiddenEdit = "REVIEW".equals(request.getSubmitType());
        model.addAttribute("isHiddenEdit", hiddenEdit);
        model.addAttribute(
            "isRecruitingEdit",
            corpJobQueryService.isRecruitingJobPosting(
                companyId,
                jobPostingId
            )
        );
        model.addAttribute(
            "isScheduledEdit",
            corpJobQueryService.isScheduledJobPosting(
                companyId,
                jobPostingId
            )
        );
        model.addAttribute(
            "hiddenReason",
            hiddenEdit
                ? corpJobQueryService.getHiddenReason(
                    companyId,
                    jobPostingId
                )
                : null
        );
    }
}
