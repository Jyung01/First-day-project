package kr.co.firstdayproject.controller.my;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.firstdayproject.dto.my.CoverLetterDto;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.service.my.CoverLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/my/cover-letter")
public class CoverLetterController {

    private final CoverLetterService coverLetterService;

    @GetMapping({"", "/list"})
    public String list(Model model, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetters", coverLetterService.findMyList(userId));
        return "my/cover-letter/list";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam(required = false) Long id, Model model, HttpServletRequest request) {
        // id 파라미터가 없으면 목록으로 리다이렉트
        if (id == null) {
            return "redirect:/my/cover-letter/list";
        }

        Long userId = getCurrentUserId(request);
        CoverLetter letter = coverLetterService.getMine(id, userId);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetter", letter);
        return "my/cover-letter/detail";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model, HttpServletRequest request) {
        model.addAttribute("activeMenu", "coverLetters");

        if (id != null) {
            Long userId = getCurrentUserId(request);
            CoverLetter letter = coverLetterService.getMine(id, userId);
            model.addAttribute("coverLetter", letter);
        }
        return "my/cover-letter/form";
    }

    @GetMapping("/ai-result")
    public String aiResult(@RequestParam Long id, Model model, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        CoverLetter letter = coverLetterService.getMine(id, userId);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetter", letter);
        return "my/cover-letter/ai-result";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<Long> create(
            @RequestBody CoverLetterDto.CreateRequest request,
            HttpServletRequest servletRequest
    ) {
        Long userId = getCurrentUserId(servletRequest);
        Long id = coverLetterService.create(userId, request);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        coverLetterService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    private static final Long TEMP_USER_ID = 1L;

    private Long getCurrentUserId(HttpServletRequest request) {
        return TEMP_USER_ID;
    }
}