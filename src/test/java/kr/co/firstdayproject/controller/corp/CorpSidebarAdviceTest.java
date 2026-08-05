package kr.co.firstdayproject.controller.corp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

class CorpSidebarAdviceTest {

    @Test
    void addsLoggedInCompanyLogoAndNameToSidebar() {
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        CorpSidebarAdvice advice = new CorpSidebarAdvice(companyRepository);
        Authentication authentication = mock(Authentication.class);
        Model model = mock(Model.class);
        User user = User.builder()
                .userId(100L)
                .companyId(10L)
                .loginId("company01")
                .passwordHash("encoded")
                .name("담당자")
                .userType("기업")
                .build();
        when(authentication.getPrincipal())
                .thenReturn(new CustomUserDetails(user, "COMPANY", true));
        when(companyRepository.findById(10L)).thenReturn(Optional.of(
                Company.builder()
                        .companyId(10L)
                        .companyName("첫출근랩")
                        .logoUrl("https://cdn.test/companies_logo/logo.png")
                        .build()
        ));

        advice.addCompanySidebarProfile(authentication, model);

        verify(model).addAttribute(
                "companySidebarLogoUrl",
                "https://cdn.test/companies_logo/logo.png"
        );
        verify(model).addAttribute("companySidebarName", "첫출근랩");
        verify(model).addAttribute("companySidebarInitial", "첫");
    }
}
