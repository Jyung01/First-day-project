package kr.co.firstdayproject.controller.corp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/corp/job")
public class CorpJobController {

    @GetMapping({"", "/list"})
    public String list() { return "corp/job-list"; }

    @GetMapping("/create")
    public String create() { return "corp/job-create"; }

    @GetMapping("/edit")
    public String edit() { return "corp/job-edit"; }

    @GetMapping("/hidden-edit")
    public String hiddenEdit() { return "corp/job-hidden-edit"; }
}
