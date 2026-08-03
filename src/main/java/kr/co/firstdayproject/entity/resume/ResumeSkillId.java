package kr.co.firstdayproject.entity.resume;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ResumeSkillId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    @Column(name = "skill_id", nullable = false)
    private Long skillId;
}
