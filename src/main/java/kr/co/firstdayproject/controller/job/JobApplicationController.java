package kr.co.firstdayproject.controller.job;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/job/application")
public class JobApplicationController {

    @GetMapping("/resume-select")
    public String resumeSelect() { return "job/resume-select"; }

    @GetMapping("/confirm")
    public String confirm() { return "job/application-confirm"; }

    @GetMapping("/complete")
    public String complete() { return "job/application-complete"; }
}
