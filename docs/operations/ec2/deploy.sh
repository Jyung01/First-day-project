#!/bin/bash

set -u

APP_DIR="/home/ec2-user/firstday"

APP_JAR="${APP_DIR}/firstday.jar"
NEW_JAR="${APP_DIR}/firstday-new.jar"
BACKUP_JAR="${APP_DIR}/firstday-previous.jar"

ENV_FILE="/opt/firstday/firstday.env"
PID_FILE="${APP_DIR}/firstday.pid"
LOG_FILE="${APP_DIR}/firstday.log"

HEALTH_URL="http://127.0.0.1:8080/actuator/health"

MAX_RETRY=30
WAIT_SECONDS=2

echo "========================================"
echo "첫출근 배포 시작"
echo "========================================"

# 신규 배포 파일 확인
if [ ! -f "$NEW_JAR" ]; then
    echo "배포할 신규 JAR이 없습니다."
    echo "확인 경로: $NEW_JAR"
    exit 1
fi

# 환경변수 파일 확인
if [ ! -f "$ENV_FILE" ]; then
    echo "환경변수 파일이 없습니다."
    echo "확인 경로: $ENV_FILE"
    exit 1
fi

# 환경변수 로드
set -a
source "$ENV_FILE"
set +a

stop_application() {
    PID=""

    # PID 파일로 기존 프로세스 확인
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
    fi

    # PID 파일이 없거나 잘못된 경우 실행 중인 JAR 프로세스 검색
    if [ -z "$PID" ] || ! kill -0 "$PID" 2>/dev/null; then
        PID=$(pgrep -f "java.*-jar ${APP_JAR}" | head -n 1 || true)
    fi

    if [ -z "$PID" ]; then
        echo "실행 중인 애플리케이션이 없습니다."
        rm -f "$PID_FILE"
        return 0
    fi

    echo "기존 애플리케이션 종료: PID=$PID"
    kill "$PID"

    for i in {1..15}; do
        if ! kill -0 "$PID" 2>/dev/null; then
            echo "애플리케이션 정상 종료"
            rm -f "$PID_FILE"
            return 0
        fi

        echo "종료 대기: ${i}/15"
        sleep 1
    done

    if kill -0 "$PID" 2>/dev/null; then
        echo "정상 종료되지 않아 강제 종료합니다."
        kill -9 "$PID"
        sleep 1
    fi

    rm -f "$PID_FILE"
}

start_application() {
    echo "애플리케이션 실행"

    nohup java \
        -Xms128m \
        -Xmx384m \
        -jar "$APP_JAR" \
        > "$LOG_FILE" 2>&1 &

    PID=$!
    echo "$PID" > "$PID_FILE"

    echo "실행 PID: $PID"
    echo "로그 파일: $LOG_FILE"
}

check_health() {
    echo "헬스체크 시작"
    echo "주소: $HEALTH_URL"

    for ((i=1; i<=MAX_RETRY; i++)); do
        PID=$(cat "$PID_FILE" 2>/dev/null || echo "")

        if [ -z "$PID" ]; then
            echo "PID를 찾을 수 없습니다."
            return 1
        fi

        if ! kill -0 "$PID" 2>/dev/null; then
            echo "애플리케이션 프로세스가 종료되었습니다."
            tail -n 100 "$LOG_FILE"
            return 1
        fi

        RESPONSE=$(curl -fsS "$HEALTH_URL" 2>/dev/null || true)

        if echo "$RESPONSE" | grep -q '"status":"UP"'; then
            echo "헬스체크 성공"
            return 0
        fi

        echo "헬스체크 대기: ${i}/${MAX_RETRY}"
        sleep "$WAIT_SECONDS"
    done

    echo "헬스체크 시간 초과"
    tail -n 100 "$LOG_FILE"
    return 1
}

rollback() {
    echo "========================================"
    echo "배포 실패 - 이전 버전 롤백 시작"
    echo "========================================"

    stop_application

    if [ ! -f "$BACKUP_JAR" ]; then
        echo "복구할 이전 버전이 없습니다."
        echo "로그 파일: $LOG_FILE"
        exit 1
    fi

    cp "$BACKUP_JAR" "$APP_JAR"

    echo "이전 버전 복원 완료"

    start_application

    if check_health; then
        echo "이전 버전 복구 성공"
    else
        echo "이전 버전 복구도 실패했습니다."
        echo "로그 파일: $LOG_FILE"
    fi

    # 롤백 성공 여부와 관계없이 신규 배포는 실패 처리
    exit 1
}

# 기존 운영 버전 백업
if [ -f "$APP_JAR" ]; then
    echo "현재 운영 버전을 이전 버전으로 백업합니다."
    cp "$APP_JAR" "$BACKUP_JAR"
else
    echo "첫 배포이므로 백업할 이전 버전이 없습니다."
fi

# 기존 애플리케이션 종료
stop_application

# 신규 JAR을 운영 JAR로 변경
echo "신규 버전 적용"
mv "$NEW_JAR" "$APP_JAR"

# 신규 버전 실행
start_application

# 헬스체크
if check_health; then
    echo "========================================"
    echo "배포 성공"
    echo "========================================"
    exit 0
else
    rollback
fi
