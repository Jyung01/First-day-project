package kr.co.firstdayproject.repository.banner;

import kr.co.firstdayproject.entity.banner.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
}
