# 배포 파이프라인 및 인프라 구성 파일

작성일: 2026-08-24

EC2 인스턴스가 소실됐을 때 **이 문서만으로 운영 환경을 재구축**할 수 있도록
배포 경로와 서버 설정 파일을 기록한다.

> 실제 비밀값(DB 비밀번호, OpenAI API 키, 인증서 개인키)은 이 문서에 적지 않는다.
> 자리표시자만 남기고 실제 값은 GitHub Secrets와 EC2에만 둔다.

관련 문서: [domain-https-handoff.md](domain-https-handoff.md) — 도메인·HTTPS·Cloudflare 구성

---

## 1. 배포 흐름

```
main 브랜치 push
  ↓
GitHub Actions (.github/workflows/deploy.yml)
  - Java 21 (temurin)
  - ./gradlew clean build      ← 테스트 실패 시 여기서 중단
  - build/libs의 실행 JAR을 firstday-new.jar로 복사
  ↓ scp (appleboy/scp-action)
EC2 /home/ec2-user/firstday/firstday-new.jar
  ↓ ssh (appleboy/ssh-action)
/home/ec2-user/firstday/deploy.sh 실행
  ↓
firstday.jar 교체 후 재기동
```

- 트리거: `main` 브랜치 push만 (`workflow_dispatch` 없음 — 수동 재배포 불가)
- 동시성: `concurrency.group=firstday-production`, `cancel-in-progress: true`
  → 연속 push 시 앞선 배포가 취소된다
- 권한: `contents: read`

### `-plain.jar` 제외 로직

Spring Boot Gradle 플러그인은 실행 JAR과 `*-plain.jar` 두 개를 만든다.
워크플로우는 `! -name "*-plain.jar"` 조건으로 실행 JAR만 고른다.
JAR을 못 찾으면 명시적으로 `exit 1`로 실패시킨다.

### GitHub Secrets

| 이름 | 용도 |
|---|---|
| `AWS_EC2_HOST` | EC2 접속 호스트 |
| `AWS_EC2_USER` | 접속 계정 (`ec2-user`) |
| `AWS_EC2_KEY` | SSH 개인키 |

> 인스턴스를 새로 만들면 `AWS_EC2_HOST`와 `AWS_EC2_KEY`를 갱신해야 한다.

---

## 2. EC2 런타임 환경

- OS: Amazon Linux
- 앱 경로: `/home/ec2-user/firstday/firstday.jar`
- 리스닝: `127.0.0.1:8080` (외부 노출 없음, Apache가 프록시)
- 웹 서버: Apache HTTP Server 2.4.67
- SELinux: `Permissive`
- DB 관리 접속: Tailscale 경유 (공개 인터넷에 DB 포트를 열지 않는다)

상태 확인:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
pgrep -af 'java.*firstday.jar'
sudo systemctl status httpd --no-pager
sudo ss -lntp | grep -E ':80|:443|:8080'
```

---

## 3. 서버 설정 파일 사본

EC2에만 존재하는 파일의 사본을 [`ec2/`](ec2/) 에 두었다.
**서버에서 값을 바꾸면 같은 PR에서 사본도 갱신한다.**

| 사본 | 서버 경로 | 상태 |
|---|---|---|
| [`ec2/deploy.sh`](ec2/deploy.sh) | `/home/ec2-user/firstday/deploy.sh` | 수집 완료 |
| [`ec2/httpd-firstday.conf`](ec2/httpd-firstday.conf) | `/etc/httpd/conf.d/firstday.conf` | 수집 완료 |
| [`ec2/httpd-ssl.conf`](ec2/httpd-ssl.conf) | `/etc/httpd/conf.d/ssl.conf` | 수집 완료 (주석 제거본) |
| [`ec2/httpd-firstday-ssl.conf`](ec2/httpd-firstday-ssl.conf) | `/etc/httpd/conf.d/firstday-ssl.conf` | 수집 완료 |
| [`ec2/httpd-firstday-cloudflare.conf`](ec2/httpd-firstday-cloudflare.conf) | `/etc/httpd/conf.d/firstday-cloudflare.conf` | 수집 완료 |
| 환경변수 (이름만) | `/opt/firstday/firstday.env` | 아래 3-3 |

### 3-1. `deploy.sh` 동작 요약

무중단 배포는 아니고 **중단 후 교체 + 실패 시 자동 롤백** 방식이다.

1. `firstday-new.jar` 존재 확인 → 없으면 실패
2. `/opt/firstday/firstday.env` 존재 확인 → 없으면 실패
3. `set -a; source` 로 환경변수 로드
4. 현재 `firstday.jar` → `firstday-previous.jar` 로 백업
5. 기존 프로세스 종료 — PID 파일 우선, 없으면 `pgrep`로 탐색.
   `kill` 후 15초 대기, 안 죽으면 `kill -9`
6. `firstday-new.jar` → `firstday.jar` 로 `mv`
7. `nohup java -Xms128m -Xmx384m -jar` 로 기동, PID를 `firstday.pid`에 기록
8. 헬스체크 — `127.0.0.1:8080/actuator/health`가 `"status":"UP"`을 반환할 때까지
   **2초 간격 최대 30회(약 60초)**. 도중에 프로세스가 죽으면 즉시 실패하고 로그 100줄 출력
9. 실패 시 `rollback()` — 종료 → `firstday-previous.jar` 복원 → 재기동 → 재헬스체크.
   **롤백 성공 여부와 무관하게 `exit 1`** 로 배포는 실패 처리

주요 경로:

| 항목 | 경로 |
|---|---|
| 앱 JAR | `/home/ec2-user/firstday/firstday.jar` |
| 이전 버전 | `/home/ec2-user/firstday/firstday-previous.jar` |
| 환경변수 | `/opt/firstday/firstday.env` |
| PID | `/home/ec2-user/firstday/firstday.pid` |
| 로그 | `/home/ec2-user/firstday/firstday.log` |

> 힙이 `-Xmx384m`로 잡혀 있다. 인스턴스 사양 대비 작으니 OOM이 나면 여기부터 본다.
>
> `set -u`는 켜져 있으나 `set -e`는 없다. 명령 실패가 스크립트를 중단시키지 않으므로
> 각 단계의 실패는 명시적 `if` 검사에만 의존한다 — 단계를 추가할 때 주의할 것.

### 3-2. Apache `conf.d` 인벤토리

수집일 2026-08-24 기준. Apache는 `conf.d/*.conf`만 로드한다.

| 파일 | 로드됨 | 역할 |
|---|---|---|
| `firstday.conf` | ✅ | :80 → HTTPS 리다이렉트 |
| `firstday-ssl.conf` | ✅ | :443 vhost, `127.0.0.1:8080` 리버스 프록시 |
| `firstday-cloudflare.conf` | ✅ | `mod_remoteip` — 실제 사용자 IP 복원 |
| `ssl.conf` | ✅ | SSL 전역 설정 + 인증서 경로 |
| `firstday.conf.before-https-redirect` | ❌ | **HTTPS 전환 전 백업** |
| `ssl.conf.before-firstday` | ❌ | **패키지 기본값 백업** |
| `README` `autoindex.conf` `userdir.conf` `welcome.conf` | — | 배포판 기본 |

> **백업 파일 2개를 `conf.d/` 밖으로 옮길 것.**
> 확장자가 `.conf`가 아니라 지금은 로드되지 않지만,
> `firstday.conf.before-https-redirect`에는 `ProxyPass`를 포함한 vhost가 통째로 들어 있다.
> 누군가 정리하다 `.conf`로 되돌리면 중복 vhost가 조용히 활성화된다.

### 3-3. 환경변수

실제 값은 EC2의 `/opt/firstday/firstday.env`에만 있다. **이름만 기록한다.**
`deploy.sh`가 `set -a; source`로 읽으며, 이 파일이 없으면 배포가 즉시 실패한다.

| 변수 | 비고 |
|---|---|
| `MYSQL_URL` `MYSQL_USERNAME` `MYSQL_PASSWORD` | 업무 데이터 원본 |
| `POSTGRES_URL` `POSTGRES_USERNAME` `POSTGRES_PASSWORD` | pgvector 임베딩 전용 |
| `OPENAI_API_KEY` | 자소서 첨삭·공고 다듬기·임베딩 |
| `AWS_REGION` `AWS_S3_PUBLIC_BUCKET` `AWS_S3_PRIVATE_BUCKET` `AWS_CLOUDFRONT_DOMAIN` | 파일 업로드 |
| `MAIL_USERNAME` `MAIL_APP_PASSWORD` | 이메일 인증 발송 |
| `MANAGEMENT_HEALTH_MAIL_ENABLED` | 메일 헬스 인디케이터 스위치 |
| `APP_SESSION_COOKIE_SECURE` | 운영 `true`. 미설정 시 세션 쿠키에 `Secure`가 안 붙는다 |

`APP_SECURITY_CSRF_ENABLED`는 **아직 이 파일에 없다**(미설정 = `false`).
CSRF 검증이 끝나면 여기에 `true`로 추가한다.

> AWS 액세스 키·시크릿이 목록에 없다. IAM 인스턴스 역할로 자격증명을 받는 구조로 보인다.
> 인스턴스를 새로 만들 때 **같은 IAM 역할을 붙여야** S3 업로드가 동작한다.

---

## 4. 요청이 앱까지 닿는 경로

수집한 파일들이 어떻게 맞물리는지 정리한다. 장애 시 어느 파일을 볼지 판단하는 기준이다.

```
HTTPS 요청 (Host: firstdayproject.site 또는 www)
  ↓
firstday-cloudflare.conf   RemoteIPHeader CF-Connecting-IP
                           → 커넥션의 remote IP를 실제 사용자 IP로 치환
                             (전역 설정이라 모든 vhost에 적용)
  ↓
firstday-ssl.conf          ServerName/ServerAlias로 매칭되는 *:443 vhost
                           RequestHeader set X-Forwarded-Proto "https"
                           ProxyAddHeaders On → X-Forwarded-For에 위 실제 IP를 실어 전달
                           ProxyPass / → http://127.0.0.1:8080/
  ↓
Spring Boot                server.forward-headers-strategy=NATIVE
                           → 톰캣 RemoteIpValve가 X-Forwarded-* 해석
                           → request.getRemoteAddr()가 실제 사용자 IP를 반환
                           → 요청을 HTTPS로 인식하므로 세션 쿠키에 Secure가 붙는다
```

**IP 복원 사슬이 3단계다.** 약관 동의 IP에 Cloudflare 엣지 IP가 찍히기 시작하면
이 중 어디가 끊긴 것이다. 확인 순서:

1. `/etc/httpd/conf/cloudflare-ips-v4.txt`가 공식 목록과 같은지
   (대역이 추가됐는데 갱신 안 하면 그 대역에서 온 요청만 조용히 틀어진다)
2. `firstday-cloudflare.conf`가 로드되는지 (`.conf` 확장자 유지)
3. `application.properties`의 `server.forward-headers-strategy=NATIVE`가 살아 있는지

> Cloudflare IP 목록은 AWS 보안 그룹의 `cloudflare-origin-ipv4` 접두사 목록과
> **항상 같이** 갱신한다. 한쪽만 갱신하면 — 보안 그룹만 하면 IP 복원이 깨지고,
> Apache만 하면 새 대역 트래픽이 방화벽에서 막힌다. 둘 다 증상이 늦게 드러난다.

### 4-1. `:443` vhost가 두 개인 점

`firstday-ssl.conf`의 `*:443`과 `ssl.conf`의 `_default_:443`이 공존한다.
`firstday-ssl.conf`에 `ServerName`/`ServerAlias`가 명시돼 있어
정상 요청은 이름으로 매칭되므로 **로드 순서에 의존하지 않는다.**

다만 `ssl.conf`의 `_default_:443`에는 `ProxyPass`가 없다.
`firstday-ssl.conf`를 지우거나 이름을 바꾸면 443 요청이 그쪽으로 떨어져
**앱 대신 `/var/www/html`이 서비스된다.** 프록시 설정을 `ssl.conf`에서 찾지 말 것.

### 4-2. `www` 정규화 (Cloudflare Redirect Rule)

`firstday-ssl.conf`의 `ServerAlias www.firstdayproject.site` 때문에
Apache는 `www`와 apex를 **둘 다 받아 서비스한다.** 오리진 설정만 보면 두 주소가 살아 있다.

정규화는 Cloudflare Redirect Rule(`Redirect from WWW to root` 템플릿)로 처리한다.
**엣지에서 끊기므로 Apache 설정에는 흔적이 없다** — 오리진만 보고 "www가 그대로 뚫려 있다"고
판단하지 말 것.

- 조건: Wildcard pattern `https://www.*`
- 대상: `https://${1}` / `301` / **Preserve query string 켬**

적용 전에는 `www`와 apex가 각각 리다이렉트 없이 `200`을 반환해
`JSESSIONID`가 호스트별로 분리됐다 (www로 로그인 후 apex로 이동하면 로그아웃 상태).
2026-08-24 적용으로 해소됨.

검증 (2026-08-24):

```
https://www/                    301 → https://firstdayproject.site/
https://www/cs/faq              301 → .../cs/faq                  (경로 보존)
https://www/job?page=2&category=3  301 → ...?page=2&category=3     (쿼리 보존)
https://firstdayproject.site/   200, 리다이렉트 없음               (루프 없음)
http://www/cs/faq?page=2        2홉 → https://firstdayproject.site/cs/faq?page=2
```

```bash
curl -sS -L -o /dev/null -w "%{num_redirects} %{url_effective} %{http_code}\n" \
  "http://www.firstdayproject.site/cs/faq?page=2"
```

> Redirect Rule 생성 시 Cloudflare가 "www가 프록시되지 않는 것 같다"는 경고를 띄울 수 있다.
> `www`는 apex를 가리키는 **CNAME**이라 생기는 오탐이다. `Ignore and deploy` 를 고른다.
> `Create a new proxied DNS record` 를 고르면 기존 CNAME과 충돌하는 레코드가 생긴다.

---

## 5. 재구축 순서 (인스턴스 소실 시)

1. EC2 기동 → Elastic IP `54.116.131.165` 연결
2. Java 21, Apache 설치 / `proxy`·`proxy_http`·`ssl`·`headers`·`remoteip` 모듈 활성화
3. Cloudflare에서 Origin Certificate 재발급 → `/etc/pki/tls/`에 배치
4. [`ec2/`](ec2/)의 Apache 설정 복원 → `sudo httpd -t && sudo systemctl reload httpd`
5. 보안 그룹: `8080` 차단, `80/443`을 `cloudflare-origin-ipv4` 접두사 목록으로 제한
6. `/home/ec2-user/firstday/` 생성 → [`ec2/deploy.sh`](ec2/deploy.sh) 복원 (`chmod +x`)
7. `/opt/firstday/firstday.env` 재작성 (`APP_SESSION_COOKIE_SECURE=true` 포함) — 없으면 `deploy.sh`가 즉시 실패한다
8. GitHub Secrets의 `AWS_EC2_HOST`·`AWS_EC2_KEY` 갱신
9. `main`에 push해 배포 트리거
10. [domain-https-handoff.md](domain-https-handoff.md) 4장의 검증 항목 전부 재확인
