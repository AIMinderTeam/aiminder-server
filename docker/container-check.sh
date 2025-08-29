#!/bin/bash

# 컨테이너 상태 확인 스크립트 (container-check.sh)
# 모든 서비스 컨테이너가 정상적으로 실행 중인지 검증

set -e

# 색상 코드 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Docker Compose 파일 경로
COMPOSE_FILE="docker-compose-ssl.yml"

# 예상되는 서비스 목록
EXPECTED_SERVICES=("aiminder-database" "aiminder-server" "aiminder-client")

log_info "컨테이너 상태 확인을 시작합니다..."

# 1단계: 모든 컨테이너 상태 확인
log_info "1단계: 모든 컨테이너 상태 확인 중..."
if ! docker-compose -f "$COMPOSE_FILE" ps > /dev/null 2>&1; then
    log_error "Docker Compose 파일을 읽을 수 없습니다: $COMPOSE_FILE"
    exit 1
fi

# 컨테이너 상태 정보 수집
CONTAINER_STATUS=$(docker-compose -f "$COMPOSE_FILE" ps --format "table {{.Service}}\t{{.State}}\t{{.Status}}")
echo "$CONTAINER_STATUS"

# 2단계: 각 서비스별 실행 상태 검증
log_info "2단계: 각 서비스별 실행 상태 검증 중..."

failed_services=()

for service in "${EXPECTED_SERVICES[@]}"; do
    log_info "서비스 확인: $service"
    
    # 서비스가 Up 상태인지 확인
    service_state=$(docker-compose -f "$COMPOSE_FILE" ps "$service" --format "{{.State}}" 2>/dev/null || echo "Not Found")
    
    if [ "$service_state" != "Up" ]; then
        log_error "서비스 $service 상태: $service_state (예상: Up)"
        failed_services+=("$service")
        continue
    fi
    
    # 컨테이너가 충분한 시간 동안 실행 중인지 확인 (최소 5초)
    service_status=$(docker-compose -f "$COMPOSE_FILE" ps "$service" --format "{{.Status}}" 2>/dev/null || echo "Unknown")
    log_info "서비스 $service 실행 시간: $service_status"
    
    # Up 시간이 5초 이상인지 간단히 확인 (seconds 또는 minutes가 포함되어 있으면 충분)
    if [[ "$service_status" =~ (second|minute|hour|day) ]]; then
        log_info "✅ 서비스 $service: 정상 실행 중"
    else
        log_warn "⚠️  서비스 $service: 실행 시간이 짧을 수 있음 ($service_status)"
    fi
done

# 3단계: 컨테이너 재시작 횟수 확인
log_info "3단계: 컨테이너 재시작 상태 확인 중..."

for service in "${EXPECTED_SERVICES[@]}"; do
    container_name=$(docker-compose -f "$COMPOSE_FILE" ps "$service" --format "{{.Names}}" 2>/dev/null || echo "")
    
    if [ -n "$container_name" ]; then
        restart_count=$(docker inspect "$container_name" --format='{{.RestartCount}}' 2>/dev/null || echo "N/A")
        if [ "$restart_count" != "N/A" ] && [ "$restart_count" -gt 0 ]; then
            log_warn "⚠️  서비스 $service: 재시작 횟수 $restart_count (비정상 재시작 발생)"
        else
            log_info "✅ 서비스 $service: 재시작 없음 (정상)"
        fi
    fi
done

# 4단계: 기본적인 리소스 사용량 확인
log_info "4단계: 기본 리소스 사용량 확인 중..."
if command -v docker stats >/dev/null 2>&1; then
    echo "컨테이너 리소스 사용량:"
    timeout 5s docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}" 2>/dev/null || log_warn "리소스 사용량 확인 시간 초과"
fi

# 결과 확인
if [ ${#failed_services[@]} -eq 0 ]; then
    log_info "🎉 모든 컨테이너가 정상적으로 실행 중입니다!"
    exit 0
else
    log_error "❌ 다음 서비스에서 문제가 발견되었습니다: ${failed_services[*]}"
    
    # 실패한 서비스의 로그 출력 (최근 10줄)
    for service in "${failed_services[@]}"; do
        log_error "서비스 $service 최근 로그:"
        docker-compose -f "$COMPOSE_FILE" logs --tail=10 "$service" || log_error "$service 로그를 가져올 수 없습니다"
        echo "---"
    done
    
    exit 1
fi