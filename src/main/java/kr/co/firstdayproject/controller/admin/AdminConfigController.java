package kr.co.firstdayproject.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/config")
public class AdminConfigController {

    @GetMapping("/job-category")
    public String jobCategory() { return "admin/config/job-category"; }

    @GetMapping("/skill")
    public String skill() { return "admin/config/skill"; }

    @GetMapping("/site-setting")
    public String siteSetting() { return "admin/config/site-setting"; }

    @GetMapping("/version")
    public String version() { return "admin/config/version"; }

    @GetMapping("/policy")
    public String policy() { return "admin/config/policy"; }
}
