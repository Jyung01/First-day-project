package kr.co.firstdayproject.dao.banner;

import kr.co.firstdayproject.dto.banner.BannerDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface  BannerDao {
    // 인덱스 : 배너 조회
    List<BannerDTO> selectActiveBanners(
            @Param("placement") String placement
    );

    // 관리자: 전체 배너 목록 조회
    List<BannerDTO> selectAdminBanners();

    // 관리자 : 배너 등록
    void insertBanner(BannerDTO dto);
}
