package kr.co.firstdayproject.controller.cs;

import kr.co.firstdayproject.service.cs.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cs/faq")
public class FaqController {

    private final FaqService faqService;

    @GetMapping({"", "/list"})
    public String list() { return "cs/faq/index"; }

    // 카테고리 필터 pill 목록
    @GetMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<?> categories() {
        return ResponseEntity.ok(faqService.getCategories());
    }

    // 공개된 FAQ 목록 (카테고리 필터 + 검색어)
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<?> list(@RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(faqService.getPublicList(categoryId, keyword));
    }
}