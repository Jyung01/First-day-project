package kr.co.firstdayproject.controller.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class HeaderNavigationAdvice {

    @ModelAttribute("activeHeaderMenu")
    public String activeHeaderMenu(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.startsWith("/job")) {
            return "job";
        }
        if (path.startsWith("/company")) {
            return "company";
        }
        if (path.startsWith("/my/resume")) {
            return "resume";
        }
        if (path.startsWith("/salary")) {
            return "salary";
        }
        return "";
    }
}
