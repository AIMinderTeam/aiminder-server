#!/bin/bash

set -euo pipefail

# Default values
DEPLOYMENT_DIR=""
BACKUP_DIR=""
USER=""
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
        --deployment-dir)
            DEPLOYMENT_DIR="$2"
            shift 2
            ;;
        --backup-dir)
            BACKUP_DIR="$2"
            shift 2
            ;;
        --user)
            USER="$2"
            shift 2
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo "사용법: $0 --deployment-dir <path> --backup-dir <path> --user <username> [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$DEPLOYMENT_DIR" ]; then
    echo -e "${RED}오류: --deployment-dir 옵션이 필요합니다${NC}"
    exit 1
fi

if [ -z "$BACKUP_DIR" ]; then
    echo -e "${RED}오류: --backup-dir 옵션이 필요합니다${NC}"
    exit 1
fi

if [ -z "$USER" ]; then
    echo -e "${RED}오류: --user 옵션이 필요합니다${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "디버그 모드 활성화"
    echo "배포 디렉토리: $DEPLOYMENT_DIR"
    echo "백업 디렉토리: $BACKUP_DIR"
    echo "사용자: $USER"
fi

echo "=== 디렉토리 준비 중 ==="

# Function to create and set permissions for a directory
create_directory() {
    local dir=$1
    local purpose=$2
    
    echo -e "${BLUE}$purpose 디렉토리 준비 중: $dir${NC}"
    
    if [ ! -d "$dir" ]; then
        echo "디렉토리 생성 중..."
        mkdir -p "$dir"
        echo -e "${GREEN}✅ 디렉토리 생성 완료${NC}"
    else
        echo -e "${GREEN}✅ 디렉토리가 이미 존재합니다${NC}"
    fi
    
    # Set ownership and permissions
    chown "$USER:$USER" "$dir"
    chmod 755 "$dir"
    
    # Verify directory
    if [ -d "$dir" ] && [ -w "$dir" ]; then
        echo -e "${GREEN}✅ 디렉토리 준비 완료${NC}"
        
        if [ "$DEBUG" = true ]; then
            echo "디렉토리 상세 정보:"
            ls -ld "$dir"
        fi
    else
        echo -e "${RED}❌ 디렉토리 준비 실패${NC}"
        return 1
    fi
    
    echo ""
}

# Create deployment directory
if ! create_directory "$DEPLOYMENT_DIR" "배포"; then
    echo -e "${RED}배포 디렉토리 준비 실패${NC}"
    exit 1
fi

# Create backup directory
if ! create_directory "$BACKUP_DIR" "백업"; then
    echo -e "${RED}백업 디렉토리 준비 실패${NC}"
    exit 1
fi

# Create subdirectories for deployment
echo -e "${BLUE}배포 하위 디렉토리 생성 중...${NC}"

# Docker compose files directory
DOCKER_DIR="$DEPLOYMENT_DIR/docker"
if [ ! -d "$DOCKER_DIR" ]; then
    mkdir -p "$DOCKER_DIR"
    chown "$USER:$USER" "$DOCKER_DIR"
    chmod 755 "$DOCKER_DIR"
    echo -e "${GREEN}✅ 도커 디렉토리 생성 완료${NC}"
fi

# Scripts directory
SCRIPTS_DIR="$DEPLOYMENT_DIR/scripts"
if [ ! -d "$SCRIPTS_DIR" ]; then
    mkdir -p "$SCRIPTS_DIR/ssl"
    mkdir -p "$SCRIPTS_DIR/deployment"
    chown -R "$USER:$USER" "$SCRIPTS_DIR"
    chmod -R 755 "$SCRIPTS_DIR"
    echo -e "${GREEN}✅ 스크립트 디렉토리 생성 완료${NC}"
fi

# Certificate directory (prepare but don't create - Let's Encrypt will handle this)
CERT_DIR="$DEPLOYMENT_DIR/cert"
if [ ! -d "$CERT_DIR" ]; then
    echo -e "${YELLOW}ℹ️  인증서 디렉토리는 SSL 초기화 중에 생성됩니다${NC}"
fi

# Environment files directory
ENV_DIR="$DEPLOYMENT_DIR/env"
if [ ! -d "$ENV_DIR" ]; then
    mkdir -p "$ENV_DIR"
    chown "$USER:$USER" "$ENV_DIR"
    chmod 700 "$ENV_DIR"  # Restricted permissions for sensitive files
    echo -e "${GREEN}✅ 제한된 권한으로 환경 디렉토리 생성 완료${NC}"
fi

# Show directory structure
echo ""
echo "=== 디렉토리 구조 ==="
echo "배포 디렉토리: $DEPLOYMENT_DIR"
tree -L 2 "$DEPLOYMENT_DIR" 2>/dev/null || {
    echo "├── docker/"
    echo "├── scripts/"
    echo "│   ├── ssl/"
    echo "│   └── deployment/"
    echo "├── env/"
    echo "└── cert/ (SSL 설정 중에 생성됨)"
}

echo ""
echo "백업 디렉토리: $BACKUP_DIR"
ls -la "$BACKUP_DIR" 2>/dev/null | head -5 || echo "빈 디렉토리"

# Final verification
echo ""
echo "=== 디렉토리 검증 ==="

# Check write permissions
for dir in "$DEPLOYMENT_DIR" "$BACKUP_DIR" "$DOCKER_DIR" "$SCRIPTS_DIR" "$ENV_DIR"; do
    if [ -d "$dir" ] && [ -w "$dir" ]; then
        echo -e "${GREEN}✅ $dir - 정상${NC}"
    else
        echo -e "${RED}❌ $dir - 실패${NC}"
        exit 1
    fi
done

echo ""
echo -e "${GREEN}✅ 모든 디렉토리 준비가 성공적으로 완료되었습니다${NC}"
exit 0