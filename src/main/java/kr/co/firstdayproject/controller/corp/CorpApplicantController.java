package kr.co.firstdayproject.controller.corp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/corp/applicant")
public class CorpApplicantController {

    @GetMapping({"", "/list"})
    public String list() { return "corp/applicants"; }

    @GetMapping("/detail")
    public String detail() { return "corp/applicant-detail"; }
}
