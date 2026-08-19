package kr.co.firstdayproject.controller.job;

import kr.co.firstdayproject.dto.job.JobApplicationConfirmView;
import kr.co.firstdayproject.exception.DuplicateApplicationException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.service.job.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/job/application")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @GetMapping("/confirm")
    public String confirm(
            @RequestParam Long jobPostingId,
            @RequestParam Long resumeId,
            @RequestParam(required = false) Long coverLetterId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            JobApplicationConfirmView application =
                    jobApplicationService.getConfirmView(
                            jobPostingId,
                            resumeId,
                            coverLetterId,
                            authentication
                    );
            model.addAttribute("applicationView", application);
            return "job/application-confirm";
        } catch (DuplicateApplicationException
                 | IllegalArgumentException
                 | ResourceNotFoundException
                 | AccessDeniedException
                 | kr.co.firstdayproject.exception.AccessDeniedException
                 exception) {
            return redirectToDetail(
                    jobPostingId,
                    exception.getMessage(),
                    redirectAttributes
            );
        }
    }

    @PostMapping
    public String apply(
            @RequestParam Long jobPostingId,
            @RequestParam Long resumeId,
            @RequestParam(required = false) Long coverLetterId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Long applicationId = jobApplicationService.apply(
                    jobPostingId,
                    resumeId,
                    coverLetterId,
                    authentication
            );

            return "redirect:/job/application/complete?applicationId="
                    + applicationId;
        } catch (DuplicateApplicationException
                 | IllegalArgumentException
                 | ResourceNotFoundException
                 | AccessDeniedException
                 | kr.co.firstdayproject.exception.AccessDeniedException
                 exception) {
            return redirectToDetail(
                    jobPostingId,
                    exception.getMessage(),
                    redirectAttributes
            );
        }
    }

    @GetMapping("/complete")
    public String complete(
            @RequestParam Long applicationId,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "applicationView",
                jobApplicationService.getCompleteView(
                        applicationId,
                        authentication
                )
        );

        return "job/application-complete";
    }

    private String redirectToDetail(
            Long jobPostingId,
            String message,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute(
                "applicationError",
                message
        );
        redirectAttributes.addAttribute(
                "jobPostingId",
                jobPostingId
        );

        return "redirect:/job/detail";
    }
}
