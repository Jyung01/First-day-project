# 도메인 + Cloudflare + HTTPS 전환 인수인계

작성일: 2026-08-21 (금)
착수 예정: 2026-08-25 (월)
담당: 양지웅

이 문서만 읽고 작업을 이어받을 수 있도록 정리했다. 아래 "확인된 사실"은 2026-08-21 기준으로
실제 코드를 확인한 내용이고, "확인 필요"는 저장소만으로는 알 수 없어 직접 봐야 하는 것들이다.

---

## 1. 목표

현재 운영 주소는 `http://54.116.131.165:8080` (IP + 포트 직접 노출).
도메인을 연결하고 **Cloudflare 프록시를 통한 HTTPS**로 전환한다.

전환이 끝나야 세션 쿠키와 CSRF 쿠키에 `Secure` 속성을 붙일 수 있고,
그래야 진행 중인 CSRF 전환 작업(4단계)을 마무리할 수 있다.

---

## 2. 현재 상태 (확인된 사실)

### 애플리케이션

- Spring Boot, 내장 톰캣, **포트 8080**(`application.properties`에 `server.port` 항목이 없어 기본값 사용).
- **`server.forward-headers-strategy` 설정이 없다.** 즉 프록시 뒤에 두면 앱은 자신이 HTTP로
  서비스되는 줄 안다.
- HTTPS·프록시 관련 설정은 현재 저장소에 하나도 없다.

### 배포

- GitHub Actions `.github/workflows/deploy.yml`
  → Gradle 빌드 → `firstday-new.jar`를 EC2 `/home/ec2-user/firstday`로 scp
  → EC2에서 `/home/ec2-user/firstday/deploy.sh` 실행.
- **`deploy.sh`는 저장소에 없다.** EC2에만 존재하므로 직접 열어봐야 한다(아래 "확인 필요" 참고).

### CSRF (진행 중인 별개 작업과 맞물림)

- `SecurityConfig`에서 `app.security.csrf.enabled` 속성으로 켜고 끈다. **기본값 false.**
- 켜지면 `http.csrf(csrf -> csrf.spa())` — `XSRF-TOKEN` 쿠키 발급 + `X-XSRF-TOKEN` 헤더 검증.
- 쿠키의 `Secure` 속성은 요청이 HTTPS로 인식될 때만 붙는다. 즉 3번 항목(포워딩 헤더)에 의존한다.
- 화면 검증은 로컬(`localhost`, HTTP)에서 진행 중이라 **도메인 작업과 무관하게 병행 가능**하다.

### DB

- **로컬과 운영이 EC2의 같은 MySQL을 공유한다.** 로컬에서 만든 데이터가 운영에 그대로 반영된다.
  이번 작업과 직접 관련은 없지만, 테스트할 때 주의할 것.

---

## 3. 작업 순서와 의존성

| 순서 | 작업 | 의존 | 위치 |
|---|---|---|---|
| A | 도메인 구입·네임서버를 Cloudflare로 변경 | 없음 | 외부 |
| B | 오리진 포트 정리 (8080 → 80/443 또는 Nginx) | A와 무관 | EC2 |
| C | Cloudflare SSL 설정 + 오리진 인증서 | A, B | Cloudflare + EC2 |
| D | 앱 설정 변경 (포워딩 헤더, 쿠키 Secure) | C | 이 저장소 |
| E | CSRF 기본값 true 전환 | D + CSRF 화면 검증 완료 | 이 저장소 |

**E를 D보다 먼저 하지 말 것.** 쿠키 설정을 두 번 만지게 된다.

---

## 4. 이 저장소에서 바꿀 것 (D단계)

### 4-1. 포워딩 헤더 — 필수

`src/main/resources/application.properties`에 추가:

```properties
# Cloudflare -> (Nginx) -> 앱 구조에서 X-Forwarded-* 를 신뢰해
# 리다이렉트 스킴(https)과 실제 클라이언트 IP를 정상화한다.
server.forward-headers-strategy=NATIVE
```

이게 없으면 다음 두 가지가 깨진다.

1. **리다이렉트 루프.** 앱이 자신을 HTTP로 인식해 `http://`로 리다이렉트를 내보낸다.
   Cloudflare가 다시 HTTPS로 바꾸고, 앱이 또 HTTP로 돌리는 순환이 생긴다.
2. **약관 동의 IP가 Cloudflare 엣지 IP로 저장된다.** 아래 4-2 참고.

`NATIVE`는 톰캣의 `RemoteIpValve`를 쓴다. 앞단에 Nginx를 두면 Nginx가
`X-Forwarded-For`/`X-Forwarded-Proto`를 그대로 전달하도록 설정해야 한다.

### 4-2. 약관 동의 IP 기록 — 위 설정으로 함께 해결됨

회원가입 시 약관 동의 IP를 남기는 지점:

- `src/main/java/kr/co/firstdayproject/controller/auth/AuthController.java:161` (개인회원)
- `src/main/java/kr/co/firstdayproject/controller/auth/AuthController.java:327` (기업회원)

둘 다 `request.getRemoteAddr()`를 쓰고, 값은 `user_policy_consents.ip_address`에 저장된다
(`CorporateSignupService`에서 45자로 truncate).

**동의 증빙 목적이므로 실제 사용자 IP여야 한다.** 4-1을 적용하면 `getRemoteAddr()`가
`X-Forwarded-For`의 원 IP를 돌려주므로 코드 변경은 필요 없다.
적용 후 실제로 사용자 IP가 들어가는지 DB에서 확인할 것.

### 4-3. 쿠키 Secure — HTTPS 확인 후

```properties
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=lax
```

`XSRF-TOKEN` 쿠키는 `csrf.spa()`가 관리하므로 별도 속성이 없다.
요청이 HTTPS로 인식되면 자동으로 `Secure`가 붙는다 — 즉 4-1이 전제다.

주의: 이 설정을 넣은 뒤 **로컬(HTTP)에서 로그인이 안 될 수 있다.** 브라우저가 `Secure` 쿠키를
평문 연결에 저장하지 않기 때문이다. 로컬 개발용 프로파일에서는 끄거나, 환경변수로 분기할 것.

### 4-4. 손댈 필요 없는 것 (확인 완료)

- **절대 URL 생성 코드 없음.** `ServletUriComponentsBuilder`나 `getRequestURL()`을 쓰는 곳이
  저장소에 없다. 메일 본문 등에 하드코딩된 `http://54.116...` 주소도 없다.
- 코드에 있는 `https://` 문자열은 S3/CloudFront(`AwsS3Service`), 카카오 우편번호 API,
  Font Awesome CDN뿐이다. 도메인 전환과 무관하다.

---

## 5. 인프라 작업 (A~C단계)

### Cloudflare SSL 모드

**반드시 Full (strict).** Flexible로 두면 브라우저↔Cloudflare만 HTTPS이고
Cloudflare↔EC2는 평문이라, 앱이 여전히 HTTP 요청으로 인식해 리다이렉트 루프가 난다.
`X-Forwarded-Proto`도 http로 오기 때문에 4-1을 적용해도 해결되지 않는다.

절차:

1. Cloudflare에서 **Origin Certificate** 발급 (무료, 15년)
2. EC2에 인증서·키 배치
3. Nginx(또는 톰캣)에 443 리스너 설정
4. Cloudflare SSL/TLS 모드를 **Full (strict)** 로 변경

### 오리진 포트

현재 8080을 직접 노출 중이다. 선택지:

- **(권장) Nginx 리버스 프록시** — 80/443을 받아 `localhost:8080`으로 전달.
  인증서 관리와 로그가 앱과 분리되고, 정적 파일 캐싱도 붙일 수 있다.
- 앱을 직접 443으로 — 인증서를 앱이 들고 있어야 하고, 배포할 때마다 다운타임이 생긴다.

Nginx를 쓸 경우 아래 헤더를 반드시 전달할 것:

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

### EC2 보안 그룹

- 80/443 인바운드 허용
- **8080 인바운드는 닫을 것.** 열어두면 도메인을 우회해 평문으로 접근할 수 있어
  HTTPS 전환의 의미가 없어진다.
- 가능하면 80/443도 Cloudflare IP 대역으로 제한 (오리진 IP 노출 시 프록시 우회 방지)

---

## 6. 확인 필요 (저장소만으로는 알 수 없음)

- [ ] `deploy.sh` 내용 — 앱을 어떻게 띄우는지(`nohup java -jar`? systemd? docker?).
      포트를 바꾸거나 프로파일을 추가하려면 이 파일을 고쳐야 한다. EC2에만 있다.
- [ ] EC2에 Nginx가 이미 깔려 있는지
- [ ] 도메인을 어디서 구입했는지 / 네임서버 변경 가능 여부
- [ ] 팀에서 쓰는 Cloudflare 계정이 있는지
- [ ] 환경변수 관리 방식 — 현재 로컬은 IntelliJ 실행 구성에 저장되어 있고,
      EC2 쪽은 `deploy.sh`나 systemd 유닛에 있을 것으로 추정

---

## 7. 검증 체크리스트

전환 후 아래를 순서대로 확인한다.

- [ ] `https://도메인` 접속 시 인증서 경고 없이 열림
- [ ] `http://도메인` 접속 시 HTTPS로 리다이렉트되고 **루프에 빠지지 않음**
- [ ] `http://54.116.131.165:8080` 직접 접근이 차단됨
- [ ] 로그인 → 로그아웃 정상 (리다이렉트가 `https://도메인`으로 나가는지 확인)
- [ ] 브라우저 개발자도구 → Application → Cookies에서
      `JSESSIONID`에 `Secure` 플래그가 붙어 있는지
- [ ] 신규 회원가입 후 `user_policy_consents.ip_address`에
      **Cloudflare IP가 아닌 실제 IP**가 들어갔는지 (DB에서 직접 확인)
- [ ] 기업 로고·프로필 이미지 업로드 정상 (S3/CloudFront는 별개 도메인이라 영향 없어야 함)
- [ ] 카카오 우편번호 API 정상 (HTTPS 페이지에서 HTTP 리소스를 부르면 차단됨)

---

## 8. 롤백

- 앱 설정(4번)은 `application.properties` 되돌리고 재배포하면 끝.
- Cloudflare는 DNS 레코드의 프록시(주황 구름)를 끄면 도메인이 오리진으로 직접 연결된다.
- 최악의 경우 네임서버를 원복하면 기존 IP 접속으로 돌아간다. 단 DNS 전파에 시간이 걸린다.

---

## 9. 관련 문서·코드

- `.github/workflows/deploy.yml` — 배포 파이프라인
- `src/main/resources/application.properties` — 설정 (환경변수 참조 16곳)
- `src/main/java/kr/co/firstdayproject/config/SecurityConfig.java` — CSRF 스위치, 로그인/로그아웃 URL
- `src/main/java/kr/co/firstdayproject/controller/auth/AuthController.java:161,327` — 동의 IP 기록
- `CLAUDE.md` — 프로젝트 개요와 협업 규칙 (main 직접 push 금지, PR 필수)

---

## 10. 이 작업과 CSRF 전환의 관계

CSRF 전환은 별개 작업이며 현재 화면 검증 단계다. 두 작업의 접점은 **쿠키 `Secure` 설정 하나**다.

- CSRF **화면 검증**은 로컬 HTTP에서 하므로 도메인 작업을 기다릴 필요가 없다.
- CSRF **기본값 true 전환 후 배포**만 이 문서의 D단계 뒤에 해야 한다.

CSRF 쪽 상세 내용(감사 로그 판독법, multipart 위양성 등)은 별도 체크리스트로 관리 중이다.
