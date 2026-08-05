package kr.co.firstdayproject.controller.my;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.firstdayproject.dto.my.CoverLetterDto;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.coverletter.CoverLetterItem;
import kr.co.firstdayproject.service.my.CoverLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import kr.co.firstdayproject.security.CustomUserDetails;

import java.util.List;

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
        if (id == null) {
            return "redirect:/my/cover-letter/list";
        }

        Long userId = getCurrentUserId(request);
        CoverLetter letter = coverLetterService.getMine(id, userId);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetter", letter);
        model.addAttribute("coverLetterItems", coverLetterService.getItems(id, userId));
        return "my/cover-letter/detail";
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model, HttpServletRequest request) {
        model.addAttribute("activeMenu", "coverLetters");

        if (id != null) {
            Long userId = getCurrentUserId(request);
            CoverLetter letter = coverLetterService.getMine(id, userId);
            model.addAttribute("coverLetter", letter);
            model.addAttribute("coverLetterItems", coverLetterService.getItems(id, userId));
        }
        return "my/cover-letter/form";
    }

    @GetMapping("/ai-result")
    public String aiResult(@RequestParam(required = false) Long id, Model model, HttpServletRequest request) {
        if (id == null) {
            return "redirect:/my/cover-letter/list";
        }

        Long userId = getCurrentUserId(request);
        CoverLetter letter = coverLetterService.getMine(id, userId);
        List<CoverLetterItem> items = coverLetterService.getItems(id, userId);

        model.addAttribute("activeMenu", "coverLetters");
        model.addAttribute("coverLetter", letter);
        model.addAttribute("coverLetterItems", items);
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

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody CoverLetterDto.CreateRequest request,
            HttpServletRequest servletRequest
    ) {
        Long userId = getCurrentUserId(servletRequest);
        coverLetterService.update(id, userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        coverLetterService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }
}