package kr.co.firstdayproject.controller.corp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/corp")
public class CorpController {

    @GetMapping({"", "/index"})
    public String index(Model model) {

        model.addAttribute("activeMenu", "dashboard");

        // 화면 테스트용
        model.addAttribute("companyStatus", "APPROVED");

        return "corp/index";
    }

    @GetMapping("/company-info")
    public String companyInfo(Model model) {

        model.addAttribute("activeMenu", "company");
        model.addAttribute("companyStatus", "APPROVED");

        return "corp/company-info";
    }

    @GetMapping("/company-info-rejected")
    public String companyInfoRejected(Model model) {

        model.addAttribute("activeMenu", "company");
        model.addAttribute("companyStatus", "REJECTED");

        // 화면 확인용 반려 정보
        model.addAttribute(
                "rejectionReason",
                "사업자등록번호와 사업자명 및 설립일 정보가 제출된 정보와 일치하지 않습니다."
        );

        return "corp/company-info-rejected";
    }

    @GetMapping("/account")
    public String account(Model model) {

        model.addAttribute("activeMenu", "account");
        model.addAttribute("companyStatus", "APPROVED");

        return "corp/account";
    }
}