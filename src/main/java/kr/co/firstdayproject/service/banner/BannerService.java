package kr.co.firstdayproject.service.banner;

import kr.co.firstdayproject.dao.banner.BannerDao;
import kr.co.firstdayproject.dto.banner.BannerDTO;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
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
        LocalDate today = LocalDate.now();
        List<BannerDTO> banners = bannerDao.selectAdminBanners();
        banners.forEach(banner -> banner.setDisplayStatus(resolveDisplayStatus(banner, today)));
        return banners;
    }

    @Transactional
    public void register(BannerDTO bannerDTO,
                         MultipartFile bannerFile,
                         Long userId) throws IOException {

        validate(bannerDTO, bannerFile, true);
        String imageUrl = awsS3Service.upload(bannerFile, "banners");

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

        if (bannerDao.insertBanner(dto) != 1) {
            awsS3Service.deletePublicByUrl(imageUrl);
            throw new IllegalStateException("배너를 등록하지 못했습니다.");
        }
    }

    public BannerDTO getBanner(Long bannerId) {
        if (bannerId == null) throw new IllegalArgumentException("배너 번호가 필요합니다.");
        BannerDTO banner = bannerDao.selectBannerById(bannerId);
        if (banner == null) throw new IllegalArgumentException("배너를 찾을 수 없습니다.");
        return banner;
    }

    @Transactional
    public void update(Long bannerId, BannerDTO request, MultipartFile bannerFile) throws IOException {
        BannerDTO existing = getBanner(bannerId);
        boolean replaceImage = bannerFile != null && !bannerFile.isEmpty();
        validate(request, bannerFile, false);
        String uploadedUrl = replaceImage ? awsS3Service.upload(bannerFile, "banners") : existing.getImageUrl();
        request.setBannerId(bannerId);
        request.setImageUrl(uploadedUrl);
        if (bannerDao.updateBanner(request) != 1) {
            if (replaceImage) awsS3Service.deletePublicByUrl(uploadedUrl);
            throw new IllegalStateException("배너를 수정하지 못했습니다.");
        }
        if (replaceImage) awsS3Service.synchronizePublicReplacement(existing.getImageUrl(), uploadedUrl);
    }

    @Transactional
    public boolean toggleActive(Long bannerId) {
        BannerDTO banner = getBanner(bannerId);
        boolean nextActive = !Boolean.TRUE.equals(banner.getIsActive());
        if (bannerDao.updateBannerActive(bannerId, nextActive) != 1)
            throw new IllegalStateException("배너 노출 상태를 변경하지 못했습니다.");
        return nextActive;
    }

    private void validate(BannerDTO dto, MultipartFile file, boolean fileRequired) {
        if (dto.getBannerName() == null || dto.getBannerName().isBlank()) throw new IllegalArgumentException("배너 제목을 입력해주세요.");
        if (!List.of("main", "job", "companies").contains(dto.getPlacement())) throw new IllegalArgumentException("올바른 노출 위치를 선택해주세요.");
        if (dto.getAltText() == null || dto.getAltText().isBlank()) throw new IllegalArgumentException("대체 텍스트를 입력해주세요.");
        if (dto.getDisplayOrder() == null || dto.getDisplayOrder() < 0) throw new IllegalArgumentException("노출 순서는 0 이상이어야 합니다.");
        if (dto.getStartsAt() != null && dto.getEndsAt() != null && !dto.getEndsAt().isAfter(dto.getStartsAt()))
            throw new IllegalArgumentException("종료일은 시작일보다 이후여야 합니다.");
        if (fileRequired && (file == null || file.isEmpty())) throw new IllegalArgumentException("배너 이미지를 선택해주세요.");
        if (file != null && !file.isEmpty() && (file.getContentType() == null || !file.getContentType().startsWith("image/")))
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
    }

    private String resolveDisplayStatus(BannerDTO banner, LocalDate today) {
        if (!Boolean.TRUE.equals(banner.getIsActive())) return "숨김";
        if (banner.getStartsAt() != null && banner.getStartsAt().isAfter(today)) return "노출 예정";
        if (banner.getEndsAt() != null && banner.getEndsAt().isBefore(today)) return "기간 종료";
        return "노출 중";
    }
}
