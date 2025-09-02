#!/bin/bash

set -euo pipefail

# Default values
DOMAIN=""
CERT_DIR=""
BACKUP_DIR=""
KEEP_COUNT=5
DEBUG=false

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
        --cert-dir)
            CERT_DIR="$2"
            shift 2
            ;;
        --backup-dir)
            BACKUP_DIR="$2"
            shift 2
            ;;
        --keep-count)
            KEEP_COUNT="$2"
            shift 2
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo "사용법: $0 --cert-dir <path> --backup-dir <path> [--domain <domain>] [--keep-count <count>] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$CERT_DIR" ]; then
    echo -e "${RED}오류: --cert-dir 옵션이 필요합니다${NC}"
    exit 1
fi

if [ -z "$BACKUP_DIR" ]; then
    echo -e "${RED}오류: --backup-dir 옵션이 필요합니다${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "디버그 모드 활성화"
    echo "인증서 디렉토리: $CERT_DIR"
    echo "백업 디렉토리: $BACKUP_DIR"
    echo "보관 개수: $KEEP_COUNT"
    [ -n "$DOMAIN" ] && echo "도메인: $DOMAIN"
fi

echo "=== SSL 인증서 백업 ==="

# Create backup directory if it doesn't exist
if [ ! -d "$BACKUP_DIR" ]; then
    echo "백업 디렉토리 생성 중: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
    chmod 755 "$BACKUP_DIR"
fi

# Check if certificate directory exists
if [ ! -d "$CERT_DIR" ]; then
    echo -e "${YELLOW}ℹ️  인증서 디렉토리가 존재하지 않습니다 (초기 배포)${NC}"
    exit 0
fi

# Check if there are certificates to backup
if [ ! -d "$CERT_DIR/letsencrypt/live" ] || [ -z "$(ls -A $CERT_DIR/letsencrypt/live 2>/dev/null)" ]; then
    echo -e "${YELLOW}ℹ️  백업할 인증서가 없습니다 (빈 디렉토리)${NC}"
    exit 0
fi

# Generate backup filename
TIMESTAMP=$(date +%Y%m%d%H%M%S)
if [ -n "$DOMAIN" ]; then
    BACKUP_FILE="cert-backup-${DOMAIN}-${TIMESTAMP}.tar.gz"
else
    BACKUP_FILE="cert-backup-${TIMESTAMP}.tar.gz"
fi

echo -e "${BLUE}📦 백업 생성 중: $BACKUP_FILE${NC}"

# Create backup
PARENT_DIR=$(dirname "$CERT_DIR")
CERT_BASENAME=$(basename "$CERT_DIR")

cd "$PARENT_DIR"
if sudo tar -czf "$BACKUP_DIR/$BACKUP_FILE" "$CERT_BASENAME/" 2>/dev/null; then
    echo -e "${GREEN}✅ 백업이 성공적으로 생성되었습니다${NC}"
else
    echo -e "${RED}❌ 백업 생성 실패${NC}"
    exit 1
fi

# Set proper permissions
sudo chown $(whoami):$(whoami) "$BACKUP_DIR/$BACKUP_FILE"
chmod 644 "$BACKUP_DIR/$BACKUP_FILE"

# Get backup file size
BACKUP_SIZE=$(du -h "$BACKUP_DIR/$BACKUP_FILE" | cut -f1)
echo "백업 크기: $BACKUP_SIZE"

# Verify backup integrity
echo "백업 무결성 검증 중..."
if sudo tar -tzf "$BACKUP_DIR/$BACKUP_FILE" >/dev/null 2>&1; then
    echo -e "${GREEN}✅ 백업 무결성 검증 완료${NC}"
else
    echo -e "${RED}❌ 백업 검증 실패${NC}"
    rm -f "$BACKUP_DIR/$BACKUP_FILE"
    exit 1
fi

# List contents if debug mode
if [ "$DEBUG" = true ]; then
    echo ""
    echo "백업 내용:"
    sudo tar -tzf "$BACKUP_DIR/$BACKUP_FILE" | head -20
    echo "..."
fi

# Clean up old backups
echo ""
echo -e "${BLUE}🗑️  이전 백업 정리 중 (최근 $KEEP_COUNT개 보관)${NC}"

cd "$BACKUP_DIR"

# Count existing backups
if [ -n "$DOMAIN" ]; then
    PATTERN="cert-backup-${DOMAIN}-*.tar.gz"
else
    PATTERN="cert-backup-*.tar.gz"
fi

BACKUP_COUNT=$(ls -1 $PATTERN 2>/dev/null | wc -l)

if [ $BACKUP_COUNT -gt $KEEP_COUNT ]; then
    # Remove old backups
    ls -t $PATTERN 2>/dev/null | tail -n +$((KEEP_COUNT + 1)) | while read -r old_backup; do
        echo "이전 백업 삭제 중: $old_backup"
        rm -f "$old_backup"
    done
    echo -e "${GREEN}✅ 이전 백업 정리 완료${NC}"
else
    echo "정리할 백업이 없습니다 ($BACKUP_COUNT개 백업 발견)"
fi

# Show current backups
echo ""
echo "=== 현재 백업 목록 ==="
ls -lah $PATTERN 2>/dev/null | tail -$KEEP_COUNT || echo "백업 파일을 찾을 수 없습니다"

echo ""
echo -e "${GREEN}✅ 백업이 성공적으로 완료되었습니다${NC}"
exit 0