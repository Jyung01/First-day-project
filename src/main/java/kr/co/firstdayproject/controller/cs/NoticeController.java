package kr.co.firstdayproject.controller.cs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cs/notice")
public class NoticeController {

    @GetMapping({"", "/list"})
    public String list() { return "cs/notice/index"; }

    @GetMapping("/detail")
    public String detail() { return "cs/notice/detail"; }
}
