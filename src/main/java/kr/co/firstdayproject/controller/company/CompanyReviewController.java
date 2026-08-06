package kr.co.firstdayproject.controller.company;

import kr.co.firstdayproject.dto.company.CompanyReviewsDTO;
import kr.co.firstdayproject.service.company.CompanyReviewService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
                          Model model) {

        int pageSize = 4;

        int total = companyReviewService.getCompanyReviewCount(companyId);

        PageHandler pageHandler =
                new PageHandler(page, total, pageSize);


        List<CompanyReviewsDTO> reviewList =
                companyReviewService.getCompanyReviewList(
                        companyId,
                        pageHandler.getOffset(),
                        pageSize);

        model.addAttribute("company",
                companyService.getCompanyDetail(companyId));

        model.addAttribute("summary",
                companyReviewService.getCompanyReviewSummary(companyId));

        model.addAttribute("reviewList", reviewList);

        model.addAttribute("pageHandler", pageHandler);
        return "company/reviews";
    }

    @GetMapping("/review/write")
    public String reviewWrite() {
        return "company/review-write";
    }

    @GetMapping("/interview-reviews")
    public String interviewReviews(@RequestParam Long companyId, Model model) {
        model.addAttribute("company",
                companyService.getCompanyDetail(companyId));
        return "company/interview-reviews";
    }

    @GetMapping("/interview-review/write")
    public String interviewReviewWrite() {
        return "company/interview-review-write";
    }
}
