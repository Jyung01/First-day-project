# 도메인 + Cloudflare + HTTPS 전환 — 완료 기록 및 운영 가이드

- 최초 작성: 2026-08-21 (착수 전 인수인계용)
- 전환 완료: 2026-08-24
- 담당: 양지웅
- 운영 주소: **https://firstdayproject.site** (`www`는 301로 정규화됨)

이 문서는 전환이 끝난 뒤 **완료 기록 + 운영 가이드**로 다시 썼다.
착수 전 계획 문서(Nginx 전제)는 이 문서로 대체되었다 — 실제 구성은 **Apache**다.

---

## 1. 최종 요청 구조

```
브라우저
  ↓ HTTPS (TLS 1.2 이상)
Cloudflare  ─ DNS · 프록시 · Universal SSL · DNSSEC
  ↓ HTTPS (Full strict, Origin Certificate 검증)
EC2 Apache :443  ─ mod_ssl · mod_proxy · mod_remoteip
  ↓ HTTP (127.0.0.1 내부 통신)
Spring Boot :8080
  ↓
MySQL (회원·기업·공고·지원 등 업무 원본)
PostgreSQL + pgvector (임베딩 검색 전용 파생 데이터)
```

> DB는 두 개다. 원본은 **MySQL**이고 PostgreSQL은 임베딩 전용이다.
> 장애 대응 시 둘을 혼동하지 말 것. DB 간 외래키는 없다.

외부에서 EC2의 `8080`이나 오리진 IP의 `80/443`으로 직접 접근할 수 없다.

---

## 2. 인프라 구성

### 도메인 / DNS

- 도메인: `firstdayproject.site` (등록기관 Spaceship)
- 네임서버: `bailey.ns.cloudflare.com`, `clint.ns.cloudflare.com`
- Elastic IP: `54.116.131.165`

| Type | Name | Content | Proxy |
|---|---|---|---|
| A | `firstdayproject.site` | `54.116.131.165` | **Proxied** |
| CNAME | `www` | `firstdayproject.site` | **Proxied** |

**주황색 구름(Proxied)을 끄지 말 것.** 끄면 오리진 IP가 노출되고, 보안 그룹이
Cloudflare 대역만 허용하고 있으므로 사이트가 아예 열리지 않는다.

### Cloudflare SSL/TLS

- 암호화 모드: **Full (strict)** — Flexible로 내리면 리다이렉트 루프가 난다
- Universal Edge Certificate: Active (Cloudflare 자동 갱신)
- 최소 TLS 버전: **1.2** / TLS 1.3 지원
- Redirect Rule: `www` → apex `301` (쿼리스트링 보존)
- Always Use HTTPS: 켬
- HSTS: `max-age=31536000; includeSubDomains`

### Origin Certificate (EC2)

- 발급: Cloudflare Origin CA — `firstdayproject.site`, `*.firstdayproject.site`
- 유효기간: **2026-08-24 ~ 2041-08-20**
- 경로:
  - `/etc/pki/tls/certs/firstdayproject-origin.pem`
  - `/etc/pki/tls/private/firstdayproject-origin.key`

> `.key`는 EC2 밖으로 내보내지 않는다. Git·Notion·메신저에 올리지 말 것.

### Apache

- Amazon Linux / Apache 2.4.67
- 설정: `/etc/httpd/conf.d/firstday.conf` (프록시·리다이렉트), `/etc/httpd/conf.d/ssl.conf` (인증서)
- 모듈: `proxy`, `proxy_http`, `ssl`, `headers`, `remoteip`
- 로그: `/var/log/httpd/firstday-ssl-access.log`, `/var/log/httpd/error_log`

설정 변경 시 **반드시** 문법 검사 후 reload:

```bash
sudo httpd -t && sudo systemctl reload httpd
```

### AWS 보안 그룹

- 외부 `8080`: 차단
- `80/443`: 고객 관리형 접두사 목록 `cloudflare-origin-ipv4`로 제한 (최대 항목 20)
- 현재 IPv6 DNS 레코드가 없어 IPv4 대역만 적용

> Cloudflare IP 대역이 바뀌면 접두사 목록을 갱신해야 한다.
> 공식 목록: https://www.cloudflare.com/ips/

### DNSSEC

Cloudflare에서 활성화, DS를 Spaceship에 등록. Key Tag `2371` / Algorithm `13` / Digest Type `2`.

> DNSSEC 활성 상태에서 네임서버를 바꿀 때는 DS 레코드를 먼저 정리한다.
> 잘못된 DS가 남으면 도메인 전체가 조회되지 않는다.

---

## 3. 애플리케이션 설정

`src/main/resources/application.properties` (커밋 `d013257`, PR #89로 `main` 병합):

```properties
server.forward-headers-strategy=NATIVE
server.servlet.session.cookie.secure=${APP_SESSION_COOKIE_SECURE:false}
server.servlet.session.cookie.same-site=lax
```

운영 EC2 환경변수: `APP_SESSION_COOKIE_SECURE=true`

- `NATIVE` — 톰캣 `RemoteIpValve`가 `X-Forwarded-*`를 해석한다.
  이게 없으면 앱이 자신을 HTTP로 인식해 **리다이렉트 루프**가 나고,
  약관 동의 IP에 Cloudflare 엣지 IP가 저장된다.
- 쿠키 `Secure`는 기본값 `false`다. **로컬 HTTP 개발에서 로그인이 막히지 않도록 의도한 것**이니
  기본값을 `true`로 바꾸지 말 것. 운영에서만 환경변수로 켠다.

### 실제 사용자 IP 복원

Apache `mod_remoteip` + Cloudflare `CF-Connecting-IP`로 복원한다.
`AuthController`의 `request.getRemoteAddr()`(개인/기업 회원가입 약관 동의 IP 기록)는
**코드 변경 없이** 실제 사용자 IP를 받는다. `user_policy_consents.ip_address`에
실제 공인 IP가 저장되는 것을 확인했다.

---

## 4. 검증 결과 (2026-08-24)

| 항목 | 결과 |
|---|---|
| `https://firstdayproject.site` | `200 OK` (`Server: cloudflare`) |
| `https://www.firstdayproject.site` | `301` → apex (2026-08-24 정규화) |
| `http://.../test` | `301` → `https://.../test` (경로 보존) |
| TLS 1.1 | 핸드셰이크 거부 |
| TLS 1.2 / 1.3 | 정상 (`TLS_AES_256_GCM_SHA384`, 인증서 검증 통과) |
| 오리진 직접 접근 (`54.116.131.165:80/443`) | `TcpTestSucceeded: False` |
| 외부 `8080` | 차단 |
| `JSESSIONID` | `Secure` · `HttpOnly` · `SameSite=Lax` |
| 약관 동의 IP | Cloudflare IP 아닌 실제 IP 저장 확인 |
| DNSSEC DS 전파 | `1.1.1.1` · `8.8.8.8` 모두 응답 |

재검증 명령:

```bash
curl -sSI https://firstdayproject.site/
curl -sSI http://firstdayproject.site/test
curl -sS --tlsv1.1 --tls-max 1.1 https://firstdayproject.site/
echo | openssl s_client -connect firstdayproject.site:443 -tls1_3 -servername firstdayproject.site
```

---

## 5. 남은 작업

### 5-1. `/actuator/health` 외부 노출 차단 (권장)

현재 `https://firstdayproject.site/actuator/health`가 외부에서 `200`으로 열린다.

```
{"groups":["liveness","readiness"],"status":"UP"}
```

`management.endpoints.web.exposure.include=health` 하나만 열려 있고
`show-details=never`라 `env`·`beans`·`heapdump`·`metrics`는 모두 `404`다.
유출 정보는 "떠 있다"뿐이라 **위험도는 낮지만**, 헬스 체크는 EC2 내부에서만 하므로
외부 노출은 불필요하다. Apache에서 차단한다:

```apache
<Location "/actuator">
    Require all denied
</Location>
```

적용 후 `sudo httpd -t && sudo systemctl reload httpd`,
그리고 EC2 내부에서 `curl -fsS http://127.0.0.1:8080/actuator/health`가
여전히 되는지 확인할 것.

### 5-2. CSRF 운영 활성화

```properties
app.security.csrf.enabled=${APP_SECURITY_CSRF_ENABLED:false}
```

HTTPS·Secure 쿠키 기반은 준비됐으나 **HTTPS 완료만으로 CSRF가 켜지지는 않는다.**
순서:

1. 로컬에서 `APP_SECURITY_CSRF_ENABLED=true`로 모든 쓰기 화면 검증
   (로그인·로그아웃·회원가입·수정·삭제·**파일 업로드**·AJAX)
2. `XSRF-TOKEN` 쿠키 발급, AJAX에 `X-XSRF-TOKEN` 헤더 포함 확인
3. 토큰 없는 요청이 `403`인지 확인
4. 운영 환경변수 `APP_SECURITY_CSRF_ENABLED=true` 설정 후 배포
5. 문제 시 환경변수를 `false`로 되돌려 즉시 롤백

> multipart(파일 업로드)에서 위양성 `403`이 나기 쉽다.
> 기업 로고·프로필 이미지·이력서 첨부 화면을 우선 확인할 것.
> `SameSite=Lax`가 이미 적용돼 교차 사이트 POST는 브라우저 단에서 상당 부분 차단된다.

### 5-3. 검토만 하고 미적용

- Cloudflare WAF Managed Rules
- 로그인·회원가입 Rate Limiting (무료 플랜은 규칙 수가 적으니 로그인 POST 우선)
- robots.txt 세부 정책 / AI 크롤러 정책 (Cloudflare AI Crawl Control에서 며칠 관찰 후 판단)

> 캐시 Cache Everything은 로그인·세션 기반 사이트에 그대로 적용하지 않는다.

---

## 6. 장애 시 확인 순서

1. Cloudflare DNS 레코드 · 프록시(주황 구름) 상태
2. Cloudflare SSL/TLS 모드가 **Full (strict)** 인지
3. EC2 Apache 상태
4. `sudo httpd -t`
5. `443` 리스닝
6. Spring Boot 상태
7. Apache 액세스·에러 로그
8. 보안 그룹의 Cloudflare 접두사 목록
9. DNSSEC DS 값

```bash
sudo httpd -t
sudo systemctl status httpd --no-pager
sudo ss -lntp | grep -E ':80|:443'
curl -fsS http://127.0.0.1:8080/actuator/health
pgrep -af 'java.*firstday.jar'
sudo tail -n 100 /var/log/httpd/firstday-ssl-access.log
sudo tail -n 100 /var/log/httpd/error_log
```

---

## 7. 롤백

- 앱 설정: `application.properties` 되돌리고 재배포
- 운영 쿠키만 끄려면: 환경변수 `APP_SESSION_COOKIE_SECURE=false` 후 재기동
- Cloudflare: DNS 프록시를 끄면 오리진 직결
  — 단 **보안 그룹이 Cloudflare 대역만 허용하므로 함께 풀어야 접속된다**
- 최악의 경우 네임서버 원복 (DNSSEC DS 먼저 제거, DNS 전파 시간 소요)

---

## 8. 운영 원칙

- Cloudflare SSL/TLS는 **Full (strict)** 유지
- DNS 레코드는 **Proxied** 유지
- 외부 `8080`을 다시 열지 않는다
- Origin Certificate 개인키를 저장소·문서에 올리지 않는다
- 인증서·DNS·보안 그룹 변경 후에는 **외부 네트워크에서** 검증
- 애플리케이션 설정 변경은 PR을 거친다
- 정기 점검: Cloudflare IP 대역 변경, Origin Certificate 만료일(2041-08-20)

---

## 9. 관련 코드·문서

- `src/main/resources/application.properties` — 프록시·쿠키·CSRF 스위치
- `src/main/java/kr/co/firstdayproject/config/SecurityConfig.java` — CSRF 스위치, 로그인/로그아웃 URL
- `src/main/java/kr/co/firstdayproject/controller/auth/AuthController.java` — 약관 동의 IP 기록
- `.github/workflows/deploy.yml` — 배포 파이프라인 (EC2의 `deploy.sh` 호출, `deploy.sh`는 EC2에만 존재)
- `CLAUDE.md` — 프로젝트 개요와 협업 규칙
