package kr.co.firstdayproject.service.job;

import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.service.my.CoverLetterService;
import kr.co.firstdayproject.service.my.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ApplicationSnapshotService {

    private final ResumeService resumeService;
    private final CoverLetterService coverLetterService;
    private final ObjectMapper objectMapper;

    public String createResumeSnapshot(Resume resume) {
        Long resumeId = resume.getResumeId();
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("resumeId", resumeId);
        snapshot.put("title", resume.getTitle());
        snapshot.put("applicantName", resume.getApplicantName());
        snapshot.put("email", resume.getEmail());
        snapshot.put("phone", resume.getPhone());
        snapshot.put("careerType", resume.getCareerType());
        snapshot.put("summary", resume.getSummary());
        snapshot.put(
                "educations",
                resumeService.getEducations(resumeId)
        );
        snapshot.put(
                "careers",
                resumeService.getCareers(resumeId)
        );
        snapshot.put(
                "projects",
                resumeService.getProjects(resumeId)
        );
        snapshot.put(
                "skills",
                resumeService.getSkillNames(resumeId)
        );

        return toJson(snapshot);
    }

    public String createCoverLetterSnapshot(
            CoverLetter coverLetter,
            Long userId
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("coverLetterId", coverLetter.getCoverLetterId());
        snapshot.put("title", coverLetter.getTitle());
        snapshot.put(
                "items",
                coverLetterService.getItems(
                        coverLetter.getCoverLetterId(),
                        userId
                )
        );

        return toJson(snapshot);
    }

    private String toJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "지원 서류를 처리할 수 없습니다.",
                    exception
            );
        }
    }
}
