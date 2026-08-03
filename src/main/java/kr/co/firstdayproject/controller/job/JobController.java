package kr.co.firstdayproject.controller.job;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/job")
public class JobController {

    @GetMapping({"", "/list"})
    public String list() { return "job/list"; }

    @GetMapping("/search")
    public String search() { return "job/search"; }

    @GetMapping("/detail")
    public String detail() { return "job/detail"; }
}
