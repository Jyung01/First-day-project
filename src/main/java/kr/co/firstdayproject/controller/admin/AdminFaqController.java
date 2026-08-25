package kr.co.firstdayproject.controller.admin;

import kr.co.firstdayproject.dto.cs.FaqDto;
import kr.co.firstdayproject.security.AdminPrincipal;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.cs.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 참고: 관리자 FAQ 화면(/admin/cs/faq) 자체는 기존 AdminController.faq()가 매핑하고 있으므로
// 이 컨트롤러는 화면 매핑 없이 API(/admin/cs/faq/api/**)만 제공한다. (@Controller와 경로가 겹치면
// "Ambiguous handler methods" 에러가 나므로 반드시 @RestController + /api 하위 경로만 사용할 것)
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/cs/faq/api")
public class AdminFaqController {

    private final FaqService faqService;

    // 카테고리 옵션 (필터 셀렉트 + 등록/수정 모달 공용)
    @GetMapping("/categories")
    public ResponseEntity<?> categories() {
        return ResponseEntity.ok(faqService.getCategories());
    }

    // 목록 조회 (카테고리 필터 + 질문 검색 + 페이지네이션)
    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(faqService.getAdminList(categoryId, keyword, page));
    }

    // 수정 모달 프리필용 상세
    @GetMapping("/{faqId}")
    public ResponseEntity<?> detail(@PathVariable Long faqId) {
        return ResponseEntity.ok(faqService.getDetail(faqId));
    }

    // FAQ 등록 -> 저장 즉시 사용자 고객센터에 노출
    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestBody FaqDto.SaveRequest request) {
        Long faqId = faqService.create(request, AdminPrincipal.requireAdminId(userDetails));
        return ResponseEntity.ok(java.util.Map.of("faqId", faqId));
    }

    // FAQ 수정 -> 저장 즉시 반영
    @PutMapping("/{faqId}")
    public ResponseEntity<?> update(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @PathVariable Long faqId, @RequestBody FaqDto.SaveRequest request) {
        faqService.update(faqId, request, AdminPrincipal.requireAdminId(userDetails));
        return ResponseEntity.ok().build();
    }

    // FAQ 삭제
    @DeleteMapping("/{faqId}")
    public ResponseEntity<?> delete(@PathVariable Long faqId) {
        faqService.delete(faqId);
        return ResponseEntity.ok().build();
    }
}