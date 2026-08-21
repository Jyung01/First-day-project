package kr.co.firstdayproject.controller.corp;

import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "kr.co.firstdayproject.controller.corp")
@RequiredArgsConstructor
public class CorpSidebarAdvice {

    private final CompanyRepository companyRepository;

    @ModelAttribute
    public void addCompanySidebarProfile(
            Authentication authentication,
            Model model
    ) {
        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)
                || userDetails.getCompanyId() == null) {
            return;
        }

        companyRepository.findById(userDetails.getCompanyId())
                .ifPresent(company -> {
                    String companyName = company.getCompanyName();
                    model.addAttribute(
                            "companySidebarLogoUrl",
                            company.getLogoUrl()
                    );
                    model.addAttribute(
                            "companySidebarName",
                            companyName
                    );
                    model.addAttribute(
                            "companySidebarInitial",
                            companyName == null || companyName.isBlank()
                                    ? "기"
                                    : companyName.substring(0, 1)
                    );
                    /*
                     * 사이드바 하단의 "마지막 수정" 표시용.
                     * 이 프래그먼트는 기업 화면 6곳에서 쓰이므로 컨트롤러마다 넣지 않고
                     * 여기서 한 번만 채운다.
                     */
                    model.addAttribute(
                            "companySidebarUpdatedAt",
                            company.getUpdatedAt()
                    );
                });
    }
}
