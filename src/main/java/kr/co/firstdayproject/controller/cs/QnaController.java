package kr.co.firstdayproject.controller.cs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cs/qna")
public class QnaController {

    @GetMapping({"", "/list"})
    public String list() { return "cs/qna/index"; }

    @GetMapping("/detail")
    public String detail() { return "cs/qna/detail"; }

    @GetMapping("/write")
    public String write() { return "cs/qna/write"; }
}
