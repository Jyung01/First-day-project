package kr.co.firstdayproject.controller.admin.config;

import kr.co.firstdayproject.entity.job.Skill;
import kr.co.firstdayproject.service.admin.config.AdminSkillService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/config/skill")
@RequiredArgsConstructor
public class AdminSkillController {

    private final AdminSkillService adminSkillService;

    @GetMapping
    public String skill(
        @RequestParam(required = false) Long groupId,
        Model model
    ) {
        List<Skill> skillGroups = adminSkillService.getSkillGroups(true);
        Skill selectedSkillGroup = findSelectedSkillGroup(
            skillGroups,
            groupId
        );
        List<Skill> skills = getSelectedGroupSkills(selectedSkillGroup);
        List<Skill> activeSkills = adminSkillService.getSkills(true)
            .stream()
            .filter(skill -> skill.getDepth() == 2)
            .filter(skill -> skillGroups.stream().anyMatch(group ->
                group.getSkillId().equals(skill.getParentId())
            ))
            .toList();

        model.addAttribute("activeMenu", "jobConfig");
        model.addAttribute("skillGroups", skillGroups);
        model.addAttribute("selectedSkillGroup", selectedSkillGroup);
        model.addAttribute("skills", skills);
        model.addAttribute(
            "skillCountsByGroup",
            createSkillCountsByGroup(activeSkills)
        );
        model.addAttribute(
            "skillUsageCounts",
            createSkillUsageCounts(skills)
        );
        model.addAttribute("activeSkillCount", activeSkills.size());
        model.addAttribute(
            "totalSkillUsageCount",
            adminSkillService.getTotalSkillUsageCount()
        );

        return "admin/config/skill";
    }

    @PostMapping
    public String createSkill(
        @RequestParam String skillType,
        @RequestParam String skillName,
        @RequestParam(required = false) Long parentId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            validateSkillType(skillType, parentId);
            Long resolvedParentId = "GROUP".equals(skillType)
                ? null
                : parentId;
            Optional<Skill> restorableSkill = adminSkillService
                .findRestorableSkill(skillName, resolvedParentId);

            if (restorableSkill.isPresent()) {
                addRestoreModalAttributes(
                    redirectAttributes,
                    restorableSkill.get()
                );

                if (resolvedParentId != null) {
                    redirectAttributes.addAttribute(
                        "groupId",
                        resolvedParentId
                    );
                }

                return "redirect:/admin/config/skill";
            }

            Skill skill = adminSkillService.createSkill(
                skillName,
                resolvedParentId
            );

            redirectAttributes.addFlashAttribute(
                "configMessage",
                "기술 항목이 등록되었습니다."
            );

            if (skill.getParentId() != null) {
                redirectAttributes.addAttribute(
                    "groupId",
                    skill.getParentId()
                );
            } else {
                redirectAttributes.addAttribute(
                    "groupId",
                    skill.getSkillId()
                );
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/skill";
    }

    @PostMapping("/restore")
    public String restoreSkill(
        @RequestParam Long skillId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Skill skill = adminSkillService.restoreSkill(skillId);

            redirectAttributes.addFlashAttribute(
                "configMessage",
                "기술 항목이 복구되었습니다."
            );
            redirectAttributes.addAttribute(
                "groupId",
                skill.getDepth() == 1
                    ? skill.getSkillId()
                    : skill.getParentId()
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/skill";
    }

    @PostMapping("/delete")
    public String deleteSkill(
        @RequestParam Long skillId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Skill skill = adminSkillService.deleteSkill(skillId);

            redirectAttributes.addFlashAttribute(
                "configMessage",
                skill.getDepth() == 1
                    ? "기술 분류가 삭제되었습니다."
                    : "기술이 삭제되었습니다."
            );

            if (skill.getParentId() != null) {
                redirectAttributes.addAttribute(
                    "groupId",
                    skill.getParentId()
                );
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/skill";
    }

    @PostMapping("/reorder")
    public String reorderSkills(
        @RequestParam List<Long> orderedIds,
        @RequestParam(required = false) Long parentId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            adminSkillService.reorderSkills(orderedIds, parentId);
            redirectAttributes.addFlashAttribute(
                "configMessage",
                "기술 순서가 저장되었습니다."
            );

            if (parentId != null) {
                redirectAttributes.addAttribute("groupId", parentId);
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/skill";
    }

    @PostMapping("/edit")
    public String updateSkill(
        @RequestParam Long skillId,
        @RequestParam String skillName,
        @RequestParam(required = false) Long parentId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Skill skill = adminSkillService.updateSkill(
                skillId,
                skillName,
                parentId
            );

            redirectAttributes.addFlashAttribute(
                "configMessage",
                "기술 항목이 수정되었습니다."
            );
            redirectAttributes.addAttribute(
                "groupId",
                skill.getDepth() == 1
                    ? skill.getSkillId()
                    : skill.getParentId()
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "configError",
                exception.getMessage()
            );
        }

        return "redirect:/admin/config/skill";
    }

    private void validateSkillType(String skillType, Long parentId) {
        if (!"GROUP".equals(skillType) && !"SKILL".equals(skillType)) {
            throw new IllegalArgumentException(
                "기술 구분을 확인해주세요."
            );
        }

        if ("SKILL".equals(skillType) && parentId == null) {
            throw new IllegalArgumentException(
                "기술 분류를 선택해주세요."
            );
        }
    }

    private void addRestoreModalAttributes(
        RedirectAttributes redirectAttributes,
        Skill skill
    ) {
        redirectAttributes.addFlashAttribute(
            "restoreSkillId",
            skill.getSkillId()
        );
        redirectAttributes.addFlashAttribute(
            "restoreSkillName",
            skill.getSkillName()
        );
        redirectAttributes.addFlashAttribute(
            "restoreSkillDepth",
            skill.getDepth()
        );
    }

    private Skill findSelectedSkillGroup(
        List<Skill> skillGroups,
        Long groupId
    ) {
        if (skillGroups.isEmpty()) {
            return null;
        }

        if (groupId == null) {
            return skillGroups.getFirst();
        }

        return skillGroups.stream()
            .filter(group -> group.getSkillId().equals(groupId))
            .findFirst()
            .orElse(skillGroups.getFirst());
    }

    private List<Skill> getSelectedGroupSkills(Skill selectedSkillGroup) {
        if (selectedSkillGroup == null) {
            return Collections.emptyList();
        }

        return adminSkillService.getSkillsByGroup(
            selectedSkillGroup.getSkillId(),
            true
        );
    }

    private Map<Long, Long> createSkillCountsByGroup(
        List<Skill> activeSkills
    ) {
        Map<Long, Long> skillCounts = new LinkedHashMap<>();

        for (Skill skill : activeSkills) {
            skillCounts.merge(skill.getParentId(), 1L, Long::sum);
        }

        return skillCounts;
    }

    private Map<Long, Long> createSkillUsageCounts(List<Skill> skills) {
        Map<Long, Long> usageCounts = new LinkedHashMap<>();

        for (Skill skill : skills) {
            usageCounts.put(
                skill.getSkillId(),
                adminSkillService.getSkillUsageCount(skill.getSkillId())
            );
        }

        return usageCounts;
    }
}
