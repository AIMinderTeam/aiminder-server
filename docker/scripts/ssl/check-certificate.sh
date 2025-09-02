#!/bin/bash

set -euo pipefail

# Default values
DOMAIN=""
CERT_DIR=""
DAYS_THRESHOLD=30
DEBUG=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --domain)
            DOMAIN="$2"
            shift 2
            ;;
        --cert-dir)
            CERT_DIR="$2"
            shift 2
            ;;
        --days-threshold)
            DAYS_THRESHOLD="$2"
            shift 2
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo "사용법: $0 --domain <domain> --cert-dir <path> [--days-threshold <days>] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$DOMAIN" ]; then
    echo -e "${RED}오류: --domain 옵션이 필요합니다${NC}"
    exit 1
fi

if [ -z "$CERT_DIR" ]; then
    echo -e "${RED}오류: --cert-dir 옵션이 필요합니다${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "디버그 모드 활성화"
    echo "도메인: $DOMAIN"
    echo "인증서 디렉토리: $CERT_DIR"
    echo "임계달 기준: $DAYS_THRESHOLD일"
fi

# Certificate file path
CERT_FILE="${CERT_DIR}/live/${DOMAIN}/cert.pem"

echo "=== SSL 인증서 상태 확인 ==="
echo "도메인: $DOMAIN"
echo "인증서 경로: $CERT_FILE"

# Check if certificate exists
if [ ! -f "$CERT_FILE" ]; then
    echo -e "${YELLOW}⚠️  SSL 인증서를 찾을 수 없습니다${NC}"
    echo "상태: 누락"
    exit 2
fi

echo -e "${GREEN}✅ SSL 인증서를 찾았습니다${NC}"

# Check certificate validity
if ! sudo openssl x509 -in "$CERT_FILE" -noout -checkend 0 >/dev/null 2>&1; then
    echo -e "${RED}❌ 인증서가 만료되었습니다${NC}"
    echo "상태: 만료"
    exit 3
fi

# Get certificate expiry date
CERT_EXPIRY_DATE=$(sudo openssl x509 -in "$CERT_FILE" -noout -enddate 2>/dev/null | cut -d= -f2)
echo "인증서 만료일: $CERT_EXPIRY_DATE"

# Convert expiry date to seconds since epoch
CERT_EXPIRY_EPOCH=$(date -d "$CERT_EXPIRY_DATE" +%s 2>/dev/null || date -j -f "%b %d %H:%M:%S %Y %Z" "$CERT_EXPIRY_DATE" +%s 2>/dev/null)
CURRENT_EPOCH=$(date +%s)

# Calculate days until expiry
SECONDS_UNTIL_EXPIRY=$((CERT_EXPIRY_EPOCH - CURRENT_EPOCH))
DAYS_UNTIL_EXPIRY=$((SECONDS_UNTIL_EXPIRY / 86400))

echo "만료까지 남은 일수: $DAYS_UNTIL_EXPIRY일"

# Check if renewal is needed
if [ $DAYS_UNTIL_EXPIRY -le $DAYS_THRESHOLD ]; then
    echo -e "${YELLOW}⚠️  인증서 갱신을 권장합니다 (${DAYS_UNTIL_EXPIRY}일 후 만료)${NC}"
    echo "상태: 갱신 필요"
    exit 4
fi

# Get certificate details
if [ "$DEBUG" = true ]; then
    echo ""
    echo "인증서 상세 정보:"
    sudo openssl x509 -in "$CERT_FILE" -noout -subject -issuer -dates
fi

echo -e "${GREEN}✅ 인증서가 ${DAYS_UNTIL_EXPIRY}일 더 유효합니다${NC}"
echo "상태: 유효"
exit 0