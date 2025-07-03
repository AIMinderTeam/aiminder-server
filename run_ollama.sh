#!/bin/bash

# Ollama Llama 3.1 실행용 스크립트 (Homebrew 지원)
# nohup을 사용하여 백그라운드에서 실행

# Ollama 설치 확인 및 설치 함수
install_ollama() {
    echo "Ollama가 설치되어 있지 않습니다."

    # Homebrew 설치 확인
    if command -v brew >/dev/null 2>&1; then
        echo "Homebrew를 사용하여 Ollama를 설치합니다..."
        if brew install ollama; then
            echo "✅ Ollama가 성공적으로 설치되었습니다."
            return 0
        else
            echo "❌ Homebrew를 통한 Ollama 설치에 실패했습니다."
            return 1
        fi
    else
        echo "❌ Homebrew가 설치되어 있지 않습니다."
        echo "다음 중 하나의 방법으로 Ollama를 설치해주세요:"
        echo "1. Homebrew 설치 후 'brew install ollama' 실행"
        echo "2. https://ollama.com/download 에서 직접 다운로드"
        return 1
    fi
}

# Ollama 설치 상태 확인
check_ollama_installation() {
    if command -v ollama >/dev/null 2>&1; then
        echo "✅ Ollama가 이미 설치되어 있습니다."

        # 설치된 Ollama 버전 확인
        OLLAMA_VERSION=$(ollama --version 2>/dev/null | head -n1)
        if [ -n "$OLLAMA_VERSION" ]; then
            echo "설치된 버전: $OLLAMA_VERSION"
        fi

        return 0
    else
        return 1
    fi
}

# Homebrew 업데이트 확인 함수
check_ollama_update() {
    if command -v brew >/dev/null 2>&1; then
        echo "Ollama 업데이트를 확인합니다..."

        # Homebrew로 설치된 ollama인지 확인
        if brew list ollama >/dev/null 2>&1; then
            # 업데이트 가능한지 확인
            OUTDATED=$(brew outdated ollama 2>/dev/null)
            if [ -n "$OUTDATED" ]; then
                echo "Ollama 업데이트가 있습니다: $OUTDATED"
                read -p "업데이트하시겠습니까? (y/N): " -r
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    echo "Ollama를 업데이트합니다..."
                    brew upgrade ollama
                    echo "✅ Ollama 업데이트가 완료되었습니다."
                fi
            else
                echo "✅ Ollama가 최신 버전입니다."
            fi
        else
            echo "ℹ️  Ollama가 Homebrew로 설치되지 않았습니다. 업데이트 확인을 건너뜁니다."
        fi
    fi
}

echo "=== Ollama Llama 3.1 자동 설치 및 실행 스크립트 ==="
echo ""

# Ollama 설치 상태 확인 및 설치
if ! check_ollama_installation; then
    if ! install_ollama; then
        echo "❌ Ollama 설치에 실패했습니다. 스크립트를 종료합니다."
        exit 1
    fi
else
    # 이미 설치되어 있는 경우 업데이트 확인
    check_ollama_update
fi

echo ""

# 기존 ollama 프로세스 종료
echo "기존 ollama 프로세스를 확인하고 종료합니다..."
pkill -f ollama

# 잠시 대기
sleep 2

# Ollama 서버를 백그라운드에서 시작
echo "Ollama 서버를 백그라운드에서 시작합니다..."
nohup ollama serve > /dev/null 2>&1 &
OLLAMA_PID=$!

echo "Ollama 서버가 시작되었습니다. PID: $OLLAMA_PID"

# Ollama 서버가 완전히 시작될 때까지 대기
echo "Ollama 서버 시작을 기다리는 중..."
sleep 5

# 서버 상태 확인 함수
check_ollama_status() {
    curl -s http://localhost:11434/api/tags > /dev/null 2>&1
    return $?
}

# 최대 30초 동안 서버 시작을 기다림
TIMEOUT=30
COUNTER=0

while ! check_ollama_status && [ $COUNTER -lt $TIMEOUT ]; do
    echo "Ollama 서버 시작 대기 중... ($COUNTER/$TIMEOUT)"
    sleep 1
    COUNTER=$((COUNTER + 1))
done

if ! check_ollama_status; then
    echo "❌ Ollama 서버 시작에 실패했습니다."
    echo "다음을 확인해주세요:"
    echo "1. 포트 11434가 사용 중인지 확인: lsof -i :11434"
    echo "2. Ollama 설치 상태 확인: ollama --version"
    exit 1
fi

echo "✅ Ollama 서버가 성공적으로 시작되었습니다."

# Llama 3.1 모델이 설치되어 있는지 확인
echo "Llama 3.1 모델 설치 상태를 확인합니다..."
if ! ollama list | grep -q "llama3.1"; then
    echo "Llama 3.1 모델이 설치되어 있지 않습니다."
    echo "사용 가능한 Llama 3.1 모델 크기:"
    echo "1. llama3.1 (8B) - 기본, 약 4.7GB"
    echo "2. llama3.1:70b - 대형, 약 40GB"
    echo "3. llama3.1:405b - 초대형, 약 230GB"
    echo ""

    read -p "다운로드할 모델을 선택하세요 (1-3, 기본값: 1): " -r MODEL_CHOICE

    case $MODEL_CHOICE in
        2)
            MODEL_NAME="llama3.1:70b"
            ;;
        3)
            MODEL_NAME="llama3.1:405b"
            ;;
        *)
            MODEL_NAME="llama3.1"
            ;;
    esac

    echo "선택된 모델: $MODEL_NAME"
    echo "모델 다운로드를 시작합니다... (시간이 오래 걸릴 수 있습니다)"

    if ollama pull "$MODEL_NAME"; then
        echo "✅ $MODEL_NAME 모델 다운로드가 완료되었습니다."
    else
        echo "❌ $MODEL_NAME 모델 다운로드에 실패했습니다."
        echo "네트워크 연결을 확인하거나 다음 명령어를 시도해보세요:"
        echo "ollama pull $MODEL_NAME --insecure"
        exit 1
    fi
else
    echo "✅ Llama 3.1 모델이 이미 설치되어 있습니다."

    # 설치된 모델 목록 표시
    echo "설치된 Llama 3.1 모델:"
    ollama list | grep llama3.1 | while read -r line; do
        echo "  - $line"
    done

    MODEL_NAME="llama3.1"  # 기본 모델 사용
fi

# 선택된 모델을 백그라운드에서 미리 로드 (warming up)
echo "Llama 3.1 모델($MODEL_NAME)을 미리 로드합니다..."
nohup bash -c "echo 'Hello' | ollama run $MODEL_NAME" > /dev/null 2>&1 &
MODEL_PID=$!

echo "✅ 모든 서비스가 백그라운드에서 실행 중입니다."
echo ""
echo "=== 실행 정보 ==="
echo "Ollama 서버 PID: $OLLAMA_PID"
echo "모델 로딩 PID: $MODEL_PID"
echo "사용 중인 모델: $MODEL_NAME"
echo ""
echo "=== 사용법 ==="
echo "1. 대화형 모드: ollama run $MODEL_NAME"
echo "2. API 요청 예시:"
echo "   curl -X POST http://localhost:11434/api/generate -d '{\"model\": \"$MODEL_NAME\", \"prompt\": \"안녕하세요\"}'"
echo ""
echo "=== 유용한 명령어 ==="
echo "- 설치된 모델 목록: ollama list"
echo "- 실행 중인 모델 확인: ollama ps"
echo "- 서버 상태 확인: curl http://localhost:11434/api/tags"
echo ""
echo "=== 서비스 중지 ==="
echo "서비스를 중지하려면: pkill -f ollama"
echo "또는 특정 PID 종료: kill $OLLAMA_PID"
echo ""

# 스크립트 종료 시 정리 함수
cleanup() {
    echo ""
    echo "스크립트를 종료합니다..."
    echo "Ollama 서버는 계속 실행 중입니다. (PID: $OLLAMA_PID)"
    echo "중지하려면: kill $OLLAMA_PID 또는 pkill -f ollama"
}

# Ctrl+C 시그널 처리
trap cleanup EXIT

echo "Ollama와 Llama 3.1이 백그라운드에서 실행 중입니다."
echo "이 스크립트는 계속 실행되며, Ctrl+C로 종료할 수 있습니다."
echo "종료해도 Ollama 서버는 백그라운드에서 계속 실행됩니다."

# 스크립트가 즉시 종료되지 않도록 대기
read -p "Enter 키를 누르면 스크립트를 종료합니다 (Ollama는 계속 실행): "