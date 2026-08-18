package kr.co.firstdayproject.repository.site;

import kr.co.firstdayproject.entity.site.SiteVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteVersionRepository extends JpaRepository<SiteVersion, Long> {

    boolean existsByVersionName(String versionName);

    boolean existsByVersionNameAndSiteVersionIdNot(
            String versionName,
            Long siteVersionId
    );

    Optional<SiteVersion> findFirstByOrderByCreatedAtDescSiteVersionIdDesc();
}
