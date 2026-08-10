package kr.co.firstdayproject.dto.job;

import java.util.List;

public record SkillFilterGroup(
        Long skillId,
        String skillName,
        List<SkillFilterOption> children
) {

    public SkillFilterGroup {
        children = List.copyOf(children);
    }
}
