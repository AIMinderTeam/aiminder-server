#!/bin/bash

set -euo pipefail

# Default values
DOMAIN=""
DEPLOYMENT_DIR=""
ENV_FILE=""
RENEW_THRESHOLD=30
DEBUG=false
FORCE_RENEW=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --domain)
            DOMAIN="$2"
            shift 2
            ;;
        --deployment-dir)
            DEPLOYMENT_DIR="$2"
            shift 2
            ;;
        --env-file)
            ENV_FILE="$2"
            shift 2
            ;;
        --renew-threshold)
            RENEW_THRESHOLD="$2"
            shift 2
            ;;
        --force-renew)
            FORCE_RENEW=true
            shift
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo "사용법: $0 --domain <domain> --deployment-dir <path> --env-file <path> [--renew-threshold <days>] [--force-renew] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$DOMAIN" ]; then
    echo -e "${RED}오류: --domain 옵션이 필요합니다${NC}"
    exit 1
fi

if [ -z "$DEPLOYMENT_DIR" ]; then
    echo -e "${RED}오류: --deployment-dir 옵션이 필요합니다${NC}"
    exit 1
fi

if [ -z "$ENV_FILE" ]; then
    echo -e "${RED}오류: --env-file 옵션이 필요합니다${NC}"
    exit 1
fi

# Check if env file exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}오류: 환경 파일을 찾을 수 없습니다: $ENV_FILE${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "디버그 모드 활성화"
    echo "도메인: $DOMAIN"
    echo "배포 디렉토리: $DEPLOYMENT_DIR"
    echo "환경 파일: $ENV_FILE"
    echo "갱신 임계값: $RENEW_THRESHOLD일"
    echo "강제 갱신: $FORCE_RENEW"
fi

echo "=== SSL 자동 감지 배포 ==="

# Change to deployment directory
cd "$DEPLOYMENT_DIR"

# Certificate paths
CERT_DIR="./cert"
CERT_FILE="${CERT_DIR}/letsencrypt/live/${DOMAIN}/cert.pem"

# Function to check certificate validity
check_certificate_validity() {
    if [ ! -f "$CERT_FILE" ]; then
        echo -e "${YELLOW}⚠️  인증서를 찾을 수 없습니다${NC}"
        return 1
    fi
    
    echo "인증서 유효성 확인 중..."
    
    # Get certificate expiry date
    CERT_EXPIRY=$(sudo openssl x509 -in "$CERT_FILE" -noout -enddate 2>/dev/null | cut -d= -f2)
    if [ -z "$CERT_EXPIRY" ]; then
        echo -e "${RED}❌ 인증서 읽기 실패${NC}"
        return 1
    fi
    
    # Convert to epoch for comparison
    CERT_EXPIRY_EPOCH=$(date -d "$CERT_EXPIRY" +%s 2>/dev/null || date -j -f "%b %d %H:%M:%S %Y %Z" "$CERT_EXPIRY" +%s 2>/dev/null)
    CURRENT_EPOCH=$(date +%s)
    
    if [ "$CERT_EXPIRY_EPOCH" -le "$CURRENT_EPOCH" ]; then
        echo -e "${RED}❌ 인증서가 만료되었습니다${NC}"
        return 1
    fi
    
    # Calculate days until expiry
    DAYS_UNTIL_EXPIRY=$(( ($CERT_EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))
    echo "인증서가 $DAYS_UNTIL_EXPIRY일 더 유효합니다"
    
    if [ $DAYS_UNTIL_EXPIRY -le $RENEW_THRESHOLD ]; then
        echo -e "${YELLOW}⚠️  인증서 갱신을 권장합니다 (${DAYS_UNTIL_EXPIRY}일 후 만료)${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ 인증서가 유효합니다${NC}"
    return 0
}

# Function to stop running containers
stop_containers() {
    echo -e "${BLUE}기존 컨테이너 중지 중...${NC}"
    sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml down || true
    echo -e "${GREEN}✅ 컨테이너 중지 완료${NC}"
}

# Function to deploy with existing certificate
deploy_with_existing_cert() {
    echo -e "${BLUE}기존 SSL 인증서로 배포 중...${NC}"
    
    stop_containers
    
    echo "서비스 시작 중..."
    if sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml up -d; then
        echo -e "${GREEN}✅ 기존 인증서로 서비스 시작 완료${NC}"
        return 0
    else
        echo -e "${RED}❌ 서비스 시작 실패${NC}"
        return 1
    fi
}

# Function to deploy with new certificate
deploy_with_new_cert() {
    echo -e "${BLUE}새 SSL 인증서로 배포 중...${NC}"
    
    stop_containers
    
    # Stop any existing certbot containers
    sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-certbot-init.yml down || true
    
    # Create certificate directories if they don't exist
    if [ ! -d "$CERT_DIR/webroot" ] || [ ! -d "$CERT_DIR/letsencrypt" ]; then
        echo "인증서 디렉토리 생성 중..."
        sudo mkdir -p "$CERT_DIR/webroot" "$CERT_DIR/letsencrypt"
        echo -e "${GREEN}✅ 디렉토리 생성 완료${NC}"
    fi
    
    echo "SSL 인증서 생성 시작..."
    if sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-certbot-init.yml up --abort-on-container-exit; then
        echo -e "${GREEN}✅ 인증서 생성 성공${NC}"
        
        # Clean up certbot containers
        sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-certbot-init.yml down
        
        # Start services with new certificate
        echo "새 인증서로 서비스 시작 중..."
        if sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml up -d; then
            echo -e "${GREEN}✅ 새 인증서로 서비스 시작 완료${NC}"
            return 0
        else
            echo -e "${RED}❌ 서비스 시작 실패${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ 인증서 생성 실패${NC}"
        return 1
    fi
}

# Main deployment logic
NEED_NEW_CERT=false

# Check if force renewal is requested
if [ "$FORCE_RENEW" = true ]; then
    echo -e "${YELLOW}강제 갱신이 요청되었습니다${NC}"
    NEED_NEW_CERT=true
else
    # Check existing certificate
    if check_certificate_validity; then
        NEED_NEW_CERT=false
    else
        NEED_NEW_CERT=true
    fi
fi

# Deploy based on certificate status
if [ "$NEED_NEW_CERT" = true ]; then
    if ! deploy_with_new_cert; then
        echo -e "${RED}배포 실패${NC}"
        exit 1
    fi
else
    if ! deploy_with_existing_cert; then
        echo -e "${RED}배포 실패${NC}"
        exit 1
    fi
fi

# Verify deployment
echo ""
echo "=== 배포 검증 ==="

# Check running containers
echo "실행 중인 컨테이너:"
sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml ps

# Check SSL certificate status
if [ -f "$CERT_FILE" ]; then
    echo ""
    echo "SSL 인증서 상태:"
    sudo openssl x509 -in "$CERT_FILE" -text -noout | grep -E "(Subject:|Not After)" || echo "인증서 읽기 실패"
    
    if [ "$DEBUG" = true ]; then
        echo ""
        echo "인증서 상세 정보:"
        sudo openssl x509 -in "$CERT_FILE" -noout -subject -issuer -dates
    fi
else
    echo -e "${YELLOW}⚠️  배포 후 SSL 인증서를 찾을 수 없습니다${NC}"
fi

echo ""
echo -e "${GREEN}✅ 배포가 성공적으로 완료되었습니다${NC}"
exit 0