package kr.co.firstdayproject.repository.site;

import kr.co.firstdayproject.entity.site.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
}
