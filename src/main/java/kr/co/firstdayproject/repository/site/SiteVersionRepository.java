package kr.co.firstdayproject.repository.site;

import kr.co.firstdayproject.entity.site.SiteVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteVersionRepository extends JpaRepository<SiteVersion, Long> {
}
