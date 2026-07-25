package kr.co.firstdayproject.controller.company;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/company")
public class CompanyController {

    @GetMapping({"", "/list"})
    public String list() { return "company/list"; }

    @GetMapping("/detail")
    public String detail() { return "company/detail"; }

    @GetMapping("/jobs")
    public String jobs() { return "company/jobs"; }
}
