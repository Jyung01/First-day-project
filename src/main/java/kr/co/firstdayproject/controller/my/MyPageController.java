package kr.co.firstdayproject.controller.my;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/my")
public class MyPageController {

    @GetMapping({"", "/index"})
    public String index() { return "my/index"; }

    @GetMapping("/profile-edit")
    public String profileEdit() { return "my/profile-edit"; }

    @GetMapping("/applications")
    public String applications(Model model) {
        model.addAttribute("activeMenu", "applications");
        return "my/applications";
    }

    @GetMapping("/applications/{applicationNo}")
    public String applicationDetail(
            @PathVariable Long applicationNo,
            @RequestParam(defaultValue = "APPLIED") String status,
            Model model
    ) {
        model.addAttribute("applicationNo", applicationNo);
        model.addAttribute("applicationStatus", status.toUpperCase());
        model.addAttribute("activeMenu", "applications");
        return "my/application-detail";
    }

    @GetMapping("/saved-jobs")
    public String savedJobs() { return "my/saved-jobs"; }

    @GetMapping("/saved-companies")
    public String savedCompanies() { return "my/saved-companies"; }

    @GetMapping("/reviews")
    public String reviews() { return "my/reviews"; }
}
