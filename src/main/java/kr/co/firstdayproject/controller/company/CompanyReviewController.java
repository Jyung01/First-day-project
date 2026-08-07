package kr.co.firstdayproject.controller.company;

import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import kr.co.firstdayproject.service.company.CompanyReviewService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/company")
public class CompanyReviewController {

    private final CompanyService companyService;
    private final CompanyReviewService companyReviewService;

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleReviewException(RuntimeException e,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        String companyId = request.getParameter("companyId");
        if (companyId == null || companyId.isBlank()) {
            return "redirect:/company/list";
        }
        return "redirect:/company/review/write?companyId=" + companyId;
    }

    @GetMapping("/reviews")
    public String reviews(@RequestParam Long companyId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "latest") String sort,
                          Model model) {

        int pageSize = 4;

        int total = companyReviewService.getCompanyReviewCount(companyId);

        PageHandler pageHandler =
                new PageHandler(page, total, pageSize);


        List<CompanyReviewsDTO> reviewList =
                companyReviewService.getCompanyReviewList(
                        companyId,
                        pageHandler.getOffset(),
                        pageSize,
                        sort
                );

        model.addAttribute("company",
                companyService.getCompanyDetail(companyId));

        model.addAttribute("summary",
                companyReviewService.getCompanyReviewSummary(companyId));

        model.addAttribute("reviewList", reviewList);

        model.addAttribute("pageHandler", pageHandler);

        model.addAttribute("sort", sort);

        return "company/reviews";
    }

    @GetMapping("/review/write")
    public String reviewWrite(@RequestParam Long companyId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        CompanyReviewsDTO form;
        try {
            form = companyReviewService.getReviewWriteForm(userDetails.getUserId(), companyId);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/company/reviews?companyId=" + companyId;
        }
        model.addAttribute("companyReviewsDTO", form);
        model.addAttribute("companyName", form.getCompanyName());
        model.addAttribute("jobCategoryName", form.getJobCategoryName());
        return "company/review-write";
    }

    @PostMapping("/review/write")
    public String reviewWrite(@ModelAttribute CompanyReviewsDTO dto,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        try {
            companyReviewService.insertCompanyReview(userDetails.getUserId(), dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/company/review/write?companyId=" + dto.getCompanyId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "기업리뷰 등록 중 오류가 발생했습니다. 입력 내용을 확인해주세요.");
            return "redirect:/company/review/write?companyId=" + dto.getCompanyId();
        }

        return "redirect:/company/reviews?companyId=" + dto.getCompanyId();
    }

    @GetMapping("/interview-reviews")
    public String interviewReviews(@RequestParam Long companyId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "latest") String sort,
                                   Model model) {
        model.addAttribute("company",
                companyService.getCompanyDetail(companyId));

        InterviewReviewsDTO summary =
                companyReviewService.getInterviewReviewSummary(companyId);

        int total =
                companyReviewService.getInterviewReviewCount(companyId);

        PageHandler pageHandler =
                new PageHandler(page, total, 4);

        List<InterviewReviewsDTO> reviewList =
                companyReviewService.getInterviewReviewList(
                        companyId,
                        pageHandler.getOffset(),
                        4,
                        sort
                );

        model.addAttribute("summary", summary);
        model.addAttribute("reviewList", reviewList);

        model.addAttribute("pageHandler", pageHandler);
        model.addAttribute("sort", sort);



        return "company/interview-reviews";
    }

    @GetMapping("/interview-review/write")
    public String interviewReviewWrite(@RequestParam Long companyId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        try {
            InterviewReviewsDTO form = companyReviewService.getInterviewReviewWriteForm(
                    userDetails.getUserId(), companyId);
            model.addAttribute("interviewReviewsDTO", form);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/company/interview-reviews?companyId=" + companyId;
        }
        return "company/interview-review-write";
    }

    @PostMapping("/interview-review/write")
    public String interviewReviewWrite(@ModelAttribute InterviewReviewsDTO dto,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        try {
            companyReviewService.insertInterviewReview(userDetails.getUserId(), dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/company/interview-review/write?companyId=" + dto.getCompanyId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "면접후기 등록 중 오류가 발생했습니다. 입력 내용을 확인해주세요.");
            return "redirect:/company/interview-review/write?companyId=" + dto.getCompanyId();
        }
        return "redirect:/company/interview-reviews?companyId=" + dto.getCompanyId()
                + "&page=1&sort=latest";
    }

    @PostMapping("/review/help")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleReviewHelp(
            @RequestParam String reviewType,
            @RequestParam Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            return ResponseEntity.ok(companyReviewService.toggleReviewReaction(
                    userDetails.getUserId(), reviewType, reviewId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "도움돼요 처리 중 오류가 발생했습니다."));
        }
    }
}
