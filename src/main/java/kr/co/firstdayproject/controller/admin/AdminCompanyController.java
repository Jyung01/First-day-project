package kr.co.firstdayproject.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/company")
public class AdminCompanyController {

    @GetMapping({"", "/list"})
    public String list(Model model) {
        model.addAttribute("activeMenu", "company");
        return "admin/company/index";
    }
}
