package kr.co.firstdayproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 테스트에서는 스케줄링을 끈다. 배치가 테스트 도중 공고 상태를 바꾸면
 * 다른 테스트에 간섭하고, 실패 원인도 엉뚱한 곳에서 찾게 된다.
 */
@Configuration
@Profile("!test")
@EnableScheduling
public class SchedulingConfig {
}
