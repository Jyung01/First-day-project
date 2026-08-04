package kr.co.firstdayproject.service.admin.config;

import kr.co.firstdayproject.entity.job.Skill;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.resume.ResumeSkillRepository;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSkillService {

    private static final Sort SKILL_TREE_SORT = Sort.by(
        "depth",
        "parentId",
        "displayOrder",
        "skillId"
    );

    private static final Sort DISPLAY_ORDER_SORT = Sort.by(
        "displayOrder",
        "skillId"
    );

    private final SkillRepository skillRepository;
    private final JobPostingSkillRepository jobPostingSkillRepository;
    private final ResumeSkillRepository resumeSkillRepository;

    public List<Skill> getSkills(boolean activeOnly) {
        if (activeOnly) {
            return skillRepository.findByIsActiveTrue(SKILL_TREE_SORT);
        }

        return skillRepository.findAll(SKILL_TREE_SORT);
    }

    public List<Skill> getSkillsByGroup(
        Long skillGroupId,
        boolean activeOnly
    ) {
        if (activeOnly) {
            return skillRepository.findByParentIdAndIsActiveTrue(
                skillGroupId,
                DISPLAY_ORDER_SORT
            );
        }

        return skillRepository.findByParentId(
            skillGroupId,
            DISPLAY_ORDER_SORT
        );
    }

    public List<Skill> getSkillGroups(boolean activeOnly) {
        if (activeOnly) {
            return skillRepository.findByDepthAndIsActiveTrue(
                1,
                DISPLAY_ORDER_SORT
            );
        }

        return skillRepository.findByDepth(
            1,
            DISPLAY_ORDER_SORT
        );
    }

    public long getSkillUsageCount(Long skillId) {
        return jobPostingSkillRepository.countByIdSkillId(skillId)
            + resumeSkillRepository.countByIdSkillId(skillId);
    }

    public long getTotalSkillUsageCount() {
        return jobPostingSkillRepository.count()
            + resumeSkillRepository.count();
    }

    @Transactional
    public Skill createSkill(String skillName, Long parentId) {
        String normalizedName = normalizeSkillName(skillName);
        Skill parent = validateParent(parentId);
        int depth = parent == null ? 1 : 2;
        Optional<Skill> existingSkill = findDuplicateSkill(
            depth,
            parentId,
            normalizedName
        );

        if (existingSkill.isPresent()) {
            throw new IllegalArgumentException(
                Boolean.TRUE.equals(existingSkill.get().getIsActive())
                    ? "이미 등록된 기술 항목입니다."
                    : "삭제된 기술 항목입니다. 복구 여부를 확인해 주세요."
            );
        }

        Skill skill = Skill.builder()
            .parentId(parentId)
            .depth(depth)
            .skillName(normalizedName)
            .slug(createUniqueSlug(normalizedName, parentId, null))
            .displayOrder(getNextDisplayOrder(parentId))
            .isActive(true)
            .build();

        return skillRepository.save(skill);
    }

    public Optional<Skill> findRestorableSkill(
        String skillName,
        Long parentId
    ) {
        String normalizedName = normalizeSkillName(skillName);
        int depth = parentId == null ? 1 : 2;

        return findDuplicateSkill(depth, parentId, normalizedName)
            .filter(skill -> !Boolean.TRUE.equals(skill.getIsActive()));
    }

    @Transactional
    public Skill restoreSkill(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
            .orElseThrow(() -> new IllegalArgumentException(
                "복구할 기술 항목을 찾을 수 없습니다."
            ));

        if (Boolean.TRUE.equals(skill.getIsActive())) {
            throw new IllegalArgumentException(
                "이미 복구된 기술 항목입니다."
            );
        }

        if (skill.getDepth() == 2) {
            validateParent(skill.getParentId());
        }

        skill.setDisplayOrder(getNextDisplayOrder(skill.getParentId()));
        skill.setIsActive(true);
        return skill;
    }

    @Transactional
    public Skill updateSkill(
        Long skillId,
        String skillName,
        Long parentId
    ) {
        Skill skill = skillRepository.findById(skillId)
            .orElseThrow(() -> new IllegalArgumentException(
                "수정할 기술 항목을 찾을 수 없습니다."
            ));

        if (!Boolean.TRUE.equals(skill.getIsActive())) {
            throw new IllegalArgumentException(
                "비활성화된 기술 항목은 수정할 수 없습니다."
            );
        }

        Long resolvedParentId = resolveUpdateParent(skill, parentId);
        String normalizedName = normalizeSkillName(skillName);
        validateDuplicateName(
            skill.getSkillId(),
            skill.getDepth(),
            resolvedParentId,
            normalizedName
        );

        boolean parentChanged = skill.getDepth() == 2
            && !skill.getParentId().equals(resolvedParentId);

        if (parentChanged && getSkillUsageCount(skill.getSkillId()) > 0) {
            throw new IllegalArgumentException(
                "사용 중인 기술은 기술 분류를 변경할 수 없습니다."
            );
        }

        skill.setSkillName(normalizedName);
        skill.setSlug(createUniqueSlug(
            normalizedName,
            resolvedParentId,
            skill.getSkillId()
        ));

        if (parentChanged) {
            skill.setParentId(resolvedParentId);
            skill.setDisplayOrder(getNextDisplayOrder(resolvedParentId));
        }

        return skill;
    }

    @Transactional
    public Skill deleteSkill(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
            .orElseThrow(() -> new IllegalArgumentException(
                "삭제할 기술 항목을 찾을 수 없습니다."
            ));

        if (!Boolean.TRUE.equals(skill.getIsActive())) {
            throw new IllegalArgumentException(
                "이미 삭제된 기술 항목입니다."
            );
        }

        skill.setIsActive(false);
        return skill;
    }

    @Transactional
    public void reorderSkills(
        List<Long> orderedIds,
        Long parentId
    ) {
        if (parentId != null) {
            validateParent(parentId);
        }

        List<Skill> skills = parentId == null
            ? getSkillGroups(true)
            : getSkillsByGroup(parentId, true);

        validateReorderIds(
            orderedIds,
            skills.stream()
                .map(Skill::getSkillId)
                .toList()
        );

        for (int index = 0; index < orderedIds.size(); index++) {
            Long skillId = orderedIds.get(index);
            Skill skill = skills.stream()
                .filter(item -> item.getSkillId().equals(skillId))
                .findFirst()
                .orElseThrow();

            skill.setDisplayOrder(index + 1);
        }
    }

    private void validateReorderIds(
        List<Long> orderedIds,
        List<Long> expectedIds
    ) {
        if (orderedIds == null
            || orderedIds.size() != expectedIds.size()
            || new HashSet<>(orderedIds).size() != orderedIds.size()
            || !new HashSet<>(orderedIds).equals(new HashSet<>(expectedIds))) {
            throw new IllegalArgumentException(
                "정렬할 기술 목록이 올바르지 않습니다."
            );
        }
    }

    private Long resolveUpdateParent(Skill skill, Long parentId) {
        if (skill.getDepth() == 1) {
            if (parentId != null) {
                throw new IllegalArgumentException(
                    "기술 분류에는 상위 분류를 지정할 수 없습니다."
                );
            }

            return null;
        }

        if (parentId == null) {
            throw new IllegalArgumentException(
                "기술 분류를 선택해주세요."
            );
        }

        validateParent(parentId);
        return parentId;
    }

    private void validateDuplicateName(
        Long skillId,
        Integer depth,
        Long parentId,
        String skillName
    ) {
        Optional<Skill> duplicate = findDuplicateSkill(
            depth,
            parentId,
            skillName
        );

        if (duplicate.isPresent()
            && !duplicate.get().getSkillId().equals(skillId)) {
            throw new IllegalArgumentException(
                "이미 등록된 기술 항목입니다."
            );
        }
    }

    private Optional<Skill> findDuplicateSkill(
        Integer depth,
        Long parentId,
        String skillName
    ) {
        if (depth == 1) {
            return skillRepository.findByDepthAndSkillName(
                1,
                skillName
            );
        }

        return skillRepository.findByParentIdAndSkillName(
            parentId,
            skillName
        );
    }

    private String normalizeSkillName(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException("기술명을 입력해주세요.");
        }

        String normalizedName = skillName.strip().replaceAll("\\s+", " ");

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                "기술명은 100자 이하로 입력해주세요."
            );
        }

        return normalizedName;
    }

    private Skill validateParent(Long parentId) {
        if (parentId == null) {
            return null;
        }

        Skill parent = skillRepository.findById(parentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "선택한 기술 분류를 찾을 수 없습니다."
            ));

        if (parent.getDepth() != 1 || !Boolean.TRUE.equals(parent.getIsActive())) {
            throw new IllegalArgumentException(
                "활성화된 기술 분류만 선택할 수 있습니다."
            );
        }

        return parent;
    }

    private int getNextDisplayOrder(Long parentId) {
        Optional<Skill> lastSkill;

        if (parentId == null) {
            lastSkill = skillRepository
                .findFirstByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderDescSkillIdDesc();
        } else {
            lastSkill = skillRepository
                .findFirstByParentIdAndIsActiveTrueOrderByDisplayOrderDescSkillIdDesc(
                    parentId
                );
        }

        return lastSkill
            .map(Skill::getDisplayOrder)
            .orElse(0) + 1;
    }

    private String createUniqueSlug(
        String skillName,
        Long parentId,
        Long excludedSkillId
    ) {
        String slug = skillName
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", "-")
            .replaceAll("(^-+|-+$)", "");

        if (slug.isBlank()) {
            slug = "skill";
        }

        String candidate = parentId == null
            ? "skill-group-" + slug
            : slug;

        boolean slugExists = excludedSkillId == null
            ? skillRepository.existsBySlug(candidate)
            : skillRepository.existsBySlugAndSkillIdNot(
                candidate,
                excludedSkillId
            );

        if (!slugExists) {
            return candidate;
        }

        return candidate + "-" + UUID.randomUUID()
            .toString()
            .substring(0, 8);
    }
}
