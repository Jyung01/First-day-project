package kr.co.firstdayproject.controller.company;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/company")
public class CompanyReviewController {

    @GetMapping("/reviews")
    public String reviews() { return "company/reviews"; }

    @GetMapping("/review/write")
    public String reviewWrite() { return "company/review-write"; }

    @GetMapping("/interview-reviews")
    public String interviewReviews() { return "company/interview-reviews"; }

    @GetMapping("/interview-review/write")
    public String interviewReviewWrite() { return "company/interview-review-write"; }
}
