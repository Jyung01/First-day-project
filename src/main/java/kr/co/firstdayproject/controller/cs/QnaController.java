package kr.co.firstdayproject.controller.cs;

import kr.co.firstdayproject.dto.admin.InquiryDetailDto;
import kr.co.firstdayproject.dto.admin.InquiryListItemDto;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.cs.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cs/qna")
public class QnaController {

    private final QnaService qnaService;

    /** 1:1 문의 목록 (비로그인 상태에서도 조회는 가능, 본인 문의만 노출) */
    @GetMapping({"", "/list"})
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        Long userId = getLoginUserId(userDetails);
        boolean loggedIn = userId != null;

        Page<InquiryListItemDto> result = loggedIn
                ? qnaService.getMyInquiryList(userId, status, PageRequest.of(page, 10))
                : Page.empty(PageRequest.of(page, 10));

        model.addAttribute("inquiries", result.getContent());
        model.addAttribute("totalCount", result.getTotalElements());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("status", status);
        model.addAttribute("loggedIn", loggedIn);

        return "cs/qna/index";
    }

    /** 1:1 문의 상세 (로그인 + 본인 문의만 조회 가능) */
    @GetMapping("/detail")
    public String detail(
            @RequestParam Long inquiryId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        Long userId = getLoginUserId(userDetails);
        if (userId == null) {
            return redirectToLogin("/cs/qna/detail?inquiryId=" + inquiryId);
        }

        InquiryDetailDto inquiry = qnaService.getMyInquiryDetail(inquiryId, userId);
        model.addAttribute("inquiry", inquiry);

        return "cs/qna/detail";
    }

    /** 1:1 문의 작성 화면 (로그인 필요 - 개인/기업회원 모두 작성 가능) */
    @GetMapping("/write")
    public String write(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (getLoginUserId(userDetails) == null) {
            return redirectToLogin("/cs/qna/write");
        }
        model.addAttribute("categories", qnaService.getActiveCategories());
        return "cs/qna/write";
    }

    /** 1:1 문의 등록 (AJAX) */
    @PostMapping
    @ResponseBody
    public ResponseEntity<?> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = getLoginUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요한 서비스입니다."));
        }

        Long categoryId = body.get("categoryId") == null
                ? null
                : Long.valueOf(String.valueOf(body.get("categoryId")));
        String title = (String) body.get("title");
        String content = (String) body.get("content");

        Long inquiryId = qnaService.createInquiry(userId, categoryId, title, content);
        return ResponseEntity.ok(Map.of("inquiryId", inquiryId));
    }

    /** 1:1 문의 삭제 (AJAX, 답변대기 상태만 가능) */
    @DeleteMapping("/{inquiryId}")
    @ResponseBody
    public ResponseEntity<?> delete(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = getLoginUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요한 서비스입니다."));
        }

        qnaService.deleteMyInquiry(inquiryId, userId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    /**
     * 로그인 안 되어 있으면 userDetails가 null로 주입됨(익명 사용자).
     * NOTE: CustomUserDetails에 회원 PK를 반환하는 getUserId()가 있다고 가정했습니다.
     *       실제 메서드명이 다르면(getMemberId, getId 등) 이 한 줄만 바꿔주세요.
     */
    private Long getLoginUserId(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getUserId();
    }

    private String redirectToLogin(String redirectUrl) {
        String encoded = UriUtils.encode(redirectUrl, StandardCharsets.UTF_8);
        return "redirect:/auth/login?redirectUrl=" + encoded;
    }
}