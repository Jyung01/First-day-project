package kr.co.firstdayproject.controller.my;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my")
public class MyPageController {

    @GetMapping({"", "/index"})
    public String index() { return "my/index"; }

    @GetMapping("/profile-edit")
    public String profileEdit() { return "my/profile-edit"; }

    @GetMapping("/applications")
    public String applications() { return "my/applications"; }

    @GetMapping("/application/applied")
    public String applicationApplied() { return "my/application-detail-applied"; }

    @GetMapping("/application/reviewing")
    public String applicationReviewing() { return "my/application-detail-reviewing"; }

    @GetMapping("/application/interviewed")
    public String applicationInterviewed() { return "my/application-detail-interviewed"; }

    @GetMapping("/application/rejected")
    public String applicationRejected() { return "my/application-detail-rejected"; }

    @GetMapping("/saved-jobs")
    public String savedJobs() { return "my/saved-jobs"; }

    @GetMapping("/saved-companies")
    public String savedCompanies() { return "my/saved-companies"; }

    @GetMapping("/reviews")
    public String reviews() { return "my/reviews"; }
}
