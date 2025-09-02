#!/bin/bash

set -euo pipefail

# Default values
ENV_FILE=""
REQUIRED_VARS=""
EXPORT_VARS=false
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
        --env-file)
            ENV_FILE="$2"
            shift 2
            ;;
        --required-vars)
            REQUIRED_VARS="$2"
            shift 2
            ;;
        --export)
            EXPORT_VARS=true
            shift
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo "사용법: $0 --env-file <path> [--required-vars <var1,var2,...>] [--export] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$ENV_FILE" ]; then
    echo -e "${RED}오류: --env-file 옵션이 필요합니다${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "디버그 모드 활성화"
    echo "환경 파일: $ENV_FILE"
    [ -n "$REQUIRED_VARS" ] && echo "필수 변수들: $REQUIRED_VARS"
    echo "변수 내보내기: $EXPORT_VARS"
fi

echo "=== 환경 변수 검증 ==="

# Check if environment file exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}❌ 환경 파일을 찾을 수 없습니다: $ENV_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 환경 파일을 찾았습니다${NC}"

# Load environment variables
echo "환경 변수를 로딩하는 중..."

# Create a temporary file to store the environment
TEMP_ENV=$(mktemp)

# Parse the .env file and remove comments and empty lines
grep -v '^#' "$ENV_FILE" | grep -v '^[[:space:]]*$' > "$TEMP_ENV" || true

# Check if file is empty
if [ ! -s "$TEMP_ENV" ]; then
    echo -e "${YELLOW}⚠️  환경 파일이 비어있거나 주석만 포함되어 있습니다${NC}"
    rm -f "$TEMP_ENV"
    exit 1
fi

# Count variables
VAR_COUNT=$(wc -l < "$TEMP_ENV")
echo "$VAR_COUNT개의 환경 변수를 찾았습니다"

# Function to check if a variable is set
check_variable() {
    local var_name=$1
    local var_value=""
    
    # Extract variable value from env file
    if grep -q "^${var_name}=" "$TEMP_ENV"; then
        var_value=$(grep "^${var_name}=" "$TEMP_ENV" | cut -d= -f2- | sed 's/^"//;s/"$//')
        
        if [ -z "$var_value" ]; then
            echo -e "${RED}  ❌ $var_name이 비어있습니다${NC}"
            return 1
        else
            if [ "$DEBUG" = true ]; then
                # Mask sensitive values in debug mode
                if [[ "$var_name" == *"SECRET"* ]] || [[ "$var_name" == *"PASSWORD"* ]] || [[ "$var_name" == *"KEY"* ]] || [[ "$var_name" == *"TOKEN"* ]]; then
                    echo -e "${GREEN}  ✅ $var_name = ****${NC}"
                else
                    echo -e "${GREEN}  ✅ $var_name = $var_value${NC}"
                fi
            else
                echo -e "${GREEN}  ✅ $var_name${NC}"
            fi
            
            # Export variable if requested
            if [ "$EXPORT_VARS" = true ]; then
                export "${var_name}=${var_value}"
            fi
            
            return 0
        fi
    else
        echo -e "${RED}  ❌ $var_name이 정의되지 않았습니다${NC}"
        return 1
    fi
}

# Verify required variables if specified
if [ -n "$REQUIRED_VARS" ]; then
    echo ""
    echo "필수 변수들을 확인하는 중:"
    
    # Convert comma-separated list to array
    IFS=',' read -ra VARS_ARRAY <<< "$REQUIRED_VARS"
    
    MISSING_VARS=()
    EMPTY_VARS=()
    
    for var in "${VARS_ARRAY[@]}"; do
        # Trim whitespace
        var=$(echo "$var" | xargs)
        
        if ! check_variable "$var"; then
            if grep -q "^${var}=" "$TEMP_ENV"; then
                EMPTY_VARS+=("$var")
            else
                MISSING_VARS+=("$var")
            fi
        fi
    done
    
    # Report results
    if [ ${#MISSING_VARS[@]} -gt 0 ] || [ ${#EMPTY_VARS[@]} -gt 0 ]; then
        echo ""
        echo -e "${RED}환경 변수 검증에 실패했습니다:${NC}"
        
        if [ ${#MISSING_VARS[@]} -gt 0 ]; then
            echo "누락된 변수들:"
            for var in "${MISSING_VARS[@]}"; do
                echo "  - $var"
            done
        fi
        
        if [ ${#EMPTY_VARS[@]} -gt 0 ]; then
            echo "비어있는 변수들:"
            for var in "${EMPTY_VARS[@]}"; do
                echo "  - $var"
            done
        fi
        
        rm -f "$TEMP_ENV"
        exit 1
    fi
    
    echo ""
    echo -e "${GREEN}✅ 모든 필수 변수가 존재합니다${NC}"
else
    # List all variables if no specific requirements
    echo ""
    echo "발견된 환경 변수들:"
    
    while IFS= read -r line; do
        var_name=$(echo "$line" | cut -d= -f1)
        
        if [ "$DEBUG" = true ]; then
            var_value=$(echo "$line" | cut -d= -f2- | sed 's/^"//;s/"$//')
            
            # Mask sensitive values
            if [[ "$var_name" == *"SECRET"* ]] || [[ "$var_name" == *"PASSWORD"* ]] || [[ "$var_name" == *"KEY"* ]] || [[ "$var_name" == *"TOKEN"* ]]; then
                echo "  - $var_name = ****"
            else
                echo "  - $var_name = $var_value"
            fi
        else
            echo "  - $var_name"
        fi
        
        # Export variable if requested
        if [ "$EXPORT_VARS" = true ]; then
            export "$line"
        fi
    done < "$TEMP_ENV"
fi

# Show export status
if [ "$EXPORT_VARS" = true ]; then
    echo ""
    echo -e "${BLUE}ℹ️  변수들이 현재 셸로 내보내졌습니다${NC}"
fi

# Clean up
rm -f "$TEMP_ENV"

echo ""
echo -e "${GREEN}✅ 환경 변수 검증이 성공적으로 완료되었습니다${NC}"
exit 0