package kr.co.firstdayproject.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.service.job.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private JobService jobService;

    @Mock
    private CompanyService companyService;

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private MainController mainController;

    @BeforeEach
    void setUpMainData() {
        when(jobService.getLatestJobPostingList()).thenReturn(List.of());
        when(jobService.getPopularJobPostingList()).thenReturn(List.of());
        when(companyService.getPopularCompanyList()).thenReturn(List.of());
        when(bannerService.getActiveBanners("main")).thenReturn(List.of());
        when(jobService.getActiveJobCategoryGroups()).thenReturn(List.of(
                new JobCategoryGroup(1L, "개발", List.of()),
                new JobCategoryGroup(2L, "디자인", List.of())
        ));
    }

    @Test
    void rendersRedesignedMainForAnonymousVisitor() {
        ConcurrentModel model = new ConcurrentModel();

        String view = mainController.index(null, model);

        assertThat(view).isEqualTo("index-redesign");
        assertThat(model.getAttribute("personalMember")).isEqualTo(false);
        assertThat(model.getAttribute("companyMember")).isEqualTo(false);
        assertThat(model.getAttribute("quickJobCategory"))
                .isEqualTo(new JobCategoryGroup(1L, "개발", List.of()));
    }

    @Test
    void exposesCompanyMemberStateForCompanyTools() {
        CustomUserDetails companyUser = mock(CustomUserDetails.class);
        when(companyUser.getUserType()).thenReturn("기업");
        ConcurrentModel model = new ConcurrentModel();

        String view = mainController.index(companyUser, model);

        assertThat(view).isEqualTo("index-redesign");
        assertThat(model.getAttribute("personalMember")).isEqualTo(false);
        assertThat(model.getAttribute("companyMember")).isEqualTo(true);
    }
}
