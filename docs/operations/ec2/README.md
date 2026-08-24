# EC2 서버 설정 파일 사본

이 디렉토리의 파일은 **EC2에만 존재하는 운영 설정의 참고용 사본**이다.
빌드나 배포에 사용되지 않으며, 인스턴스 재구축 시 복원 기준으로만 쓴다.

| 파일 | 서버 경로 |
|---|---|
| `deploy.sh` | `/home/ec2-user/firstday/deploy.sh` |
| `httpd-firstday.conf` | `/etc/httpd/conf.d/firstday.conf` |
| `httpd-ssl.conf` | `/etc/httpd/conf.d/ssl.conf` (주석 제거본) |

수집일: 2026-08-24

## 규칙

- **서버에서 설정을 바꾸면 이 사본도 같은 PR에서 갱신한다.** 어긋나면 재구축 때 못 쓴다.
- 비밀값은 넣지 않는다. 환경변수 실제 값은 EC2의 `/opt/firstday/firstday.env`,
  배포 자격증명은 GitHub Secrets에만 둔다.
- 인증서 개인키(`/etc/pki/tls/private/firstdayproject-origin.key`)는 어떤 경우에도 복사하지 않는다.

## 미수집

- `httpd-firstday-ssl.conf` — `/etc/httpd/conf.d/firstday-ssl.conf` (:443 vhost, 리버스 프록시)
- `httpd-firstday-cloudflare.conf` — `/etc/httpd/conf.d/firstday-cloudflare.conf` (`mod_remoteip`)

[../deployment.md](../deployment.md) 4장 참고.

## conf.d 백업 파일 주의

서버 `conf.d/`에 아래 백업이 남아 있다. 확장자가 `.conf`가 아니라 로드되지 않지만,
`.conf`로 되돌리면 중복 vhost가 활성화된다. `conf.d/` 밖으로 옮길 것.

- `firstday.conf.before-https-redirect`
- `ssl.conf.before-firstday`
