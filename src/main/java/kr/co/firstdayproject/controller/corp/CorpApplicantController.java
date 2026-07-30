package kr.co.firstdayproject.controller.corp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/corp/applicant")
public class CorpApplicantController {

    @GetMapping({"", "/list"})
    public String list(Model model) {
        prepare(model);
        return "corp/applicants";
    }

    @GetMapping("/detail")
    public String detail(Model model) {
        prepare(model);
        return "corp/applicant-detail";
    }

    private void prepare(Model model) {
        model.addAttribute("activeMenu", "applicants");
        model.addAttribute("companyStatus", "APPROVED");
    }
}
