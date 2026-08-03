package kr.co.firstdayproject.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping({"", "/index"})
    public String index(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        return "admin/index";
    }

    @GetMapping("/banner")
    public String banner() { return "admin/banner/index"; }

    @GetMapping("/review")
    public String review() { return "admin/review/index"; }

    @GetMapping("/report")
    public String report() { return "admin/report/index"; }

    @GetMapping("/salary")
    public String salary() { return "admin/salary/index"; }

    @GetMapping("/cs/notice")
    public String notice(Model model) {
        model.addAttribute("activeMenu", "cs");
        return "admin/cs/notice";
    }

    @GetMapping("/cs/faq")
    public String faq(Model model) {
        model.addAttribute("activeMenu", "cs");
        return "admin/cs/faq";
    }

    @GetMapping("/cs/qna")
    public String qna(Model model) {
        model.addAttribute("activeMenu", "cs");
        return "admin/cs/qna";
    }
}
