package kr.co.firstdayproject.controller.corp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/corp")
public class CorpController {

    @GetMapping({"", "/index"})
    public String index() { return "corp/index"; }

    @GetMapping("/company-info")
    public String companyInfo() { return "corp/company-info"; }

    @GetMapping("/company-info-rejected")
    public String companyInfoRejected() { return "corp/company-info-rejected"; }

    @GetMapping("/account")
    public String account() { return "corp/account"; }
}
