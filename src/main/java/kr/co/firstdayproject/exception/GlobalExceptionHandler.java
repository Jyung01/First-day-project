package kr.co.firstdayproject.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 화면(Controller) 요청에서 발생하는 표준 예외를 최소한의 에러 뷰로 연결한다. */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        String requestUri = request.getRequestURI();
        // 비공개·삭제된 사용자용 공고/기업은 존재 여부를 드러내지 않고 목록으로 보낸다.
        if (requestUri.startsWith("/job/")) {
            redirectAttributes.addFlashAttribute("errorMessage", "현재 확인할 수 없는 채용공고입니다.");
            return "redirect:/job/list";
        }
        if (requestUri.startsWith("/company/")) {
            redirectAttributes.addFlashAttribute("errorMessage", "현재 확인할 수 없는 기업정보입니다.");
            return "redirect:/company/list";
        }

        response.setStatus(HttpStatus.NOT_FOUND.value());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return "error/403";
    }
}
