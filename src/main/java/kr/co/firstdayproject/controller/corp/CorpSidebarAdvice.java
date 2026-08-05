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
                });
    }
}
