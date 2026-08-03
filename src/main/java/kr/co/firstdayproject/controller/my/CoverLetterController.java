package kr.co.firstdayproject.controller.my;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my/cover-letter")
public class CoverLetterController {

    @GetMapping({"", "/list"})
    public String list(Model model) {
        model.addAttribute("activeMenu", "coverLetters");
        return "my/cover-letter/list";
    }

    @GetMapping("/detail")
    public String detail(Model model) {
        model.addAttribute("activeMenu", "coverLetters");
        return "my/cover-letter/detail";
    }

    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("activeMenu", "coverLetters");
        return "my/cover-letter/form";
    }

    @GetMapping("/ai-result")
    public String aiResult(Model model) {
        model.addAttribute("activeMenu", "coverLetters");
        return "my/cover-letter/ai-result";
    }
}
