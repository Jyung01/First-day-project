package kr.co.firstdayproject.controller;

import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.job.MainJobListItem;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 메인 초기 렌더링과 분리된 AI 맞춤 추천 조회 API. */
@RestController
@RequiredArgsConstructor
public class MainRecommendationController {

    private final JobService jobService;

    @GetMapping("/api/main/recommendations")
    public ResponseEntity<?> recommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (!"개인".equals(userDetails.getUserType())) {
            return ResponseEntity.status(403).body(Map.of("message", "개인회원만 이용할 수 있습니다."));
        }

        Long userId = userDetails.getUserId();
        Map<Long, Integer> scores = jobService.getPersonalizedJobMatchScores(userId);
        List<MainJobListItem> jobs = jobService.getPersonalizedJobPostingList(userId, scores);
        return ResponseEntity.ok(Map.of(
                "jobs", jobs,
                "matchScores", scores,
                "matchReasons", jobService.getPersonalizedJobMatchReasons(userId, jobs, scores)
        ));
    }
}
