package kr.co.firstdayproject.controller.cs;

import kr.co.firstdayproject.dto.cs.NoticeDto;
import kr.co.firstdayproject.service.cs.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cs/notice")
@RequiredArgsConstructor
public class NoticeController {

    private static final int PAGE_SIZE = 10;

    private final NoticeService noticeService;

    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE);
        Page<NoticeDto.ListItem> noticePage = noticeService.getUserList(keyword, pageable);

        model.addAttribute("noticeList", noticePage.getContent());
        model.addAttribute("noticePage", noticePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        return "cs/notice/index";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam Long id, Model model) {
        model.addAttribute("notice", noticeService.getDetail(id));
        return "cs/notice/detail";
    }
}