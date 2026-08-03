package kr.co.firstdayproject.controller.my;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my/resume")
public class ResumeController {

    @GetMapping({"", "/list"})
    public String list() { return "my/resume/list"; }

    @GetMapping("/detail")
    public String detail() { return "my/resume/detail"; }

    @GetMapping("/form")
    public String form() { return "my/resume/form"; }
}
