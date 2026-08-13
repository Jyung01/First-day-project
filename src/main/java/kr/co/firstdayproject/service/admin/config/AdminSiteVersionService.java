package kr.co.firstdayproject.service.admin.config;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.firstdayproject.dto.admin.config.SiteVersionItem;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.site.SiteVersion;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.repository.site.SiteVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSiteVersionService {

    private static final int PAGE_SIZE = 10;

    private final SiteVersionRepository siteVersionRepository;
    private final UserRepository userRepository;

    public Page<SiteVersionItem> getVersions(int page) {
        Page<SiteVersion> versions = siteVersionRepository.findAll(
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                                .and(Sort.by(Sort.Direction.DESC, "siteVersionId"))
                )
        );

        Map<Long, User> creators = userRepository.findAllById(
                        versions.stream()
                                .map(SiteVersion::getCreatedBy)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        return versions.map(version -> toItem(version, creators.get(version.getCreatedBy())));
    }

    @Transactional
    public void create(
            Long adminUserId,
            String versionName,
            String changeNotes
    ) {
        validateAdmin(adminUserId);
        String normalizedName = requireText(versionName, "버전을 입력해 주세요.");
        String normalizedNotes = requireText(changeNotes, "변경내역을 입력해 주세요.");

        if (siteVersionRepository.existsByVersionName(normalizedName)) {
            throw new IllegalArgumentException("이미 등록된 버전입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        siteVersionRepository.save(
                SiteVersion.builder()
                        .versionName(normalizedName)
                        .changeNotes(normalizedNotes)
                        .createdBy(adminUserId)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );
    }

    @Transactional
    public void update(
            Long siteVersionId,
            String versionName,
            String changeNotes
    ) {
        SiteVersion version = siteVersionRepository.findById(siteVersionId)
                .orElseThrow(() -> new IllegalArgumentException("버전 내역을 찾을 수 없습니다."));
        String normalizedName = requireText(versionName, "버전을 입력해 주세요.");
        String normalizedNotes = requireText(changeNotes, "변경내역을 입력해 주세요.");

        if (siteVersionRepository.existsByVersionNameAndSiteVersionIdNot(
                normalizedName,
                siteVersionId
        )) {
            throw new IllegalArgumentException("이미 등록된 버전입니다.");
        }

        version.setVersionName(normalizedName);
        version.setChangeNotes(normalizedNotes);
        version.setUpdatedAt(LocalDateTime.now());
    }

    private SiteVersionItem toItem(SiteVersion version, User creator) {
        String creatorName = creator == null
                ? "관리자"
                : creator.getLoginId();

        return new SiteVersionItem(
                version.getSiteVersionId(),
                version.getVersionName(),
                version.getChangeNotes(),
                creatorName,
                version.getCreatedAt()
        );
    }

    private void validateAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new IllegalArgumentException("관리자 로그인이 필요합니다.");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
