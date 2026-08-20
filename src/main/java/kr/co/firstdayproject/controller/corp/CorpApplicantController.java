package kr.co.firstdayproject.controller.corp;

import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.corp.CorpApplicantQueryService;
import kr.co.firstdayproject.service.corp.CorpApplicantManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

@Controller
@RequestMapping("/corp/applicant")
@RequiredArgsConstructor
public class CorpApplicantController {

    private final CorpApplicantQueryService corpApplicantQueryService;
    private final CorpApplicantManagementService corpApplicantManagementService;

    @GetMapping({"", "/list"})
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long jobPostingId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        var result = corpApplicantQueryService.getApplicants(
                userDetails.getCompanyId(),
                jobPostingId,
                status,
                keyword,
                page
        );

        prepare(model);
        model.addAttribute("applicants", result.applicants());
        model.addAttribute("jobOptions", result.jobOptions());
        model.addAttribute("applicantSummary", result.summary());
        model.addAttribute("jobPostingId", result.jobPostingId());
        model.addAttribute("status", result.status());
        model.addAttribute("keyword", result.keyword());
        model.addAttribute("pageHandler", result.pageHandler());
        return "corp/applicants";
    }

    @GetMapping("/detail")
    public String detail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long id,
            Model model
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        prepare(model);
        model.addAttribute(
                "applicant",
                corpApplicantQueryService.getApplicantDetail(
                        userDetails.getCompanyId(),
                        id
                )
        );
        return "corp/applicant-detail";
    }

    @PostMapping("/detail/memo")
    public String saveMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long id,
            @RequestParam(required = false) String memo,
            RedirectAttributes redirectAttributes
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        try {
            corpApplicantManagementService.saveMemo(
                    userDetails.getCompanyId(),
                    userDetails.getUserId(),
                    id,
                    memo
            );
            redirectAttributes.addFlashAttribute(
                    "memoSuccessMessage",
                    StringUtils.hasText(memo)
                            ? "담당자 메모를 저장했습니다."
                            : "담당자 메모를 삭제했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "memoErrorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/corp/applicant/detail?id=" + id;
    }

    @PostMapping("/detail/status")
    public String changeStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long id,
            @RequestParam String nextStatus,
            RedirectAttributes redirectAttributes
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return "redirect:/auth/login";
        }

        try {
            corpApplicantManagementService.changeStatus(
                    userDetails.getCompanyId(),
                    userDetails.getUserId(),
                    id,
                    nextStatus
            );
            redirectAttributes.addFlashAttribute(
                    "statusSuccessMessage",
                    "지원 상태를 " + nextStatus + "(으)로 변경했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "statusErrorMessage",
                    exception.getMessage()
            );
        }

        return "redirect:/corp/applicant/detail?id=" + id;
    }

    private void prepare(Model model) {
        model.addAttribute("activeMenu", "applicants");
        model.addAttribute("companyStatus", "APPROVED");
    }
}
