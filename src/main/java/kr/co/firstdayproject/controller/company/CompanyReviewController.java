package kr.co.firstdayproject.controller.company;

import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.dto.company.InterviewReviewsDTO;
import kr.co.firstdayproject.service.company.CompanyReviewService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/company")
public class CompanyReviewController {

    private final CompanyService companyService;
    private final CompanyReviewService companyReviewService;

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
    public String reviewWrite(Model model) {
        model.addAttribute("companyReviewsDTO", new CompanyReviewsDTO());
        return "company/review-write";
    }

    @PostMapping("/review/write")
    public String reviewWrite(@ModelAttribute CompanyReviewsDTO dto) {

        companyReviewService.insertCompanyReview(dto);

        return "redirect:/company/detail/" + dto.getCompanyId();
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
    public String interviewReviewWrite() {
        return "company/interview-review-write";
    }
}
