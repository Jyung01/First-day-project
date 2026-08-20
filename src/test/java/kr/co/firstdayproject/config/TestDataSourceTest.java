package kr.co.firstdayproject.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 테스트 프로파일의 DataSource가 실제로 연결되는지 확인한다.
 *
 * <p>HikariCP는 커넥션을 처음 요청할 때 풀을 만들기 때문에, 설정이 잘못돼 있어도
 * 컨텍스트만 뜨는 테스트는 통과한다. application.properties의 MySQL 드라이버가
 * H2 URL과 함께 남아 있던 문제가 오래 숨어 있다가, 기동 시 DB를 건드리는 코드가
 * 생기고 나서야 엉뚱하게 contextLoads 실패로 드러났다.
 * 같은 종류의 설정 오류를 바로 잡아내기 위한 테스트다.
 */
@SpringBootTest
@ActiveProfiles("test")
class TestDataSourceTest {

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3Presigner s3Presigner;

    @Autowired
    private DataSource dataSource;

    @Test
    void connectsWithConfiguredDriver() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:");
        }
    }
}
