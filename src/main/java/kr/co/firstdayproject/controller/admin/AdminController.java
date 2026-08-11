package kr.co.firstdayproject.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    @GetMapping({"", "/index"})
    public String index(Model model) {
        model.addAttribute("activeMenu", "dashboard");
        return "admin/index";
    }

    // salary 메서드 삭제됨 -> AdminSalaryController 가 전담
    // notice 메서드 삭제됨 -> AdminNoticeController 가 전담

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
