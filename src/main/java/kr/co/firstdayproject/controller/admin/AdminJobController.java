package kr.co.firstdayproject.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/job")
public class AdminJobController {

    @GetMapping({"", "/list"})
    public String list(Model model) {
        model.addAttribute("activeMenu", "job");
        return "admin/job/index";
    }
}
