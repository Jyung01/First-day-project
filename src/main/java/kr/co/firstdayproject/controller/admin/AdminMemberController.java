package kr.co.firstdayproject.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/member")
public class AdminMemberController {

    @GetMapping({"", "/list"})
    public String list() { return "admin/member/index"; }
}
