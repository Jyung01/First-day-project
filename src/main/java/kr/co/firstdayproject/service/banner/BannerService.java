package kr.co.firstdayproject.service.banner;

import jakarta.transaction.Transactional;
import kr.co.firstdayproject.dao.banner.BannerDao;
import kr.co.firstdayproject.dto.banner.BannerDTO;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerDao bannerDao;
    private final AwsS3Service awsS3Service;

    public List<BannerDTO> getActiveBanners() {
        return getActiveBanners("main");
    }

    public List<BannerDTO> getActiveBanners(String placement) {
        return bannerDao.selectActiveBanners(placement);
    }

    public List<BannerDTO> getAdminBanners() {
        return bannerDao.selectAdminBanners();
    }

    public void register(BannerDTO bannerDTO,
                         MultipartFile bannerFile,
                         Long userId) throws IOException {

        String imageUrl = awsS3Service.upload(bannerFile);

        BannerDTO dto = new BannerDTO();

        dto.setBannerName(bannerDTO.getBannerName());
        dto.setPlacement(bannerDTO.getPlacement());
        dto.setDisplayOrder(bannerDTO.getDisplayOrder());
        dto.setLinkUrl(bannerDTO.getLinkUrl());
        dto.setAltText(bannerDTO.getAltText());
        dto.setStartsAt(bannerDTO.getStartsAt());
        dto.setEndsAt(bannerDTO.getEndsAt());

        dto.setImageUrl(imageUrl);
        dto.setCreatedBy(userId);

        bannerDao.insertBanner(dto);
    }
}
