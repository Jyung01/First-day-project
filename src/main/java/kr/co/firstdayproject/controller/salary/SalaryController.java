package kr.co.firstdayproject.controller.salary;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/salary")
public class SalaryController {

    @GetMapping({"", "/list"})
    public String list() { return "salary/list"; }

    @GetMapping("/detail")
    public String detail() { return "salary/detail"; }

    @GetMapping("/create")
    public String create() { return "salary/create"; }

    @GetMapping("/edit")
    public String edit() { return "salary/edit"; }

    @GetMapping("/my-list")
    public String myList() { return "salary/my-list"; }
}
