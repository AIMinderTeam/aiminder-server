#!/bin/bash
set -e

PASSWORD=""
VERSION="0.0.0"

echo "VERSION=$VERSION"

# 인자 파싱: -password 와 -version 옵션 사용`
while [ "$#" -gt 0 ]; do
  case "$1" in
    -password)
      PASSWORD="$2"
      shift 2
      ;;
    -version)
      VERSION="$2"
      shift 2
      ;;
    *)
      echo "Unknown parameter: $1"
      exit 1
      ;;
  esac
done

# 2. 데이터베이스 컨테이너가 실행 중인지 확인하고, 실행 중이 아니면 실행
echo "Checking if database container 'aiminder-database' is already running..."
if [ "$(docker ps -q -f name=aiminder-database)" ]; then
  echo "Database container 'aiminder-database' is already running. Skipping docker run."
else
  echo "Starting database container using docker run..."
  docker run -d \
    --name aiminder-database \
    -e POSTGRES_DB=aiminderdb \
    -e POSTGRES_USER=aiminder \
    -e POSTGRES_PASSWORD=aiminder \
    -v "$(pwd)/data":/var/lib/postgresql/data \
    -p 5432:5432 \
    postgres:14
  if [ $? -ne 0 ]; then
    echo "[ERROR] Failed to start database container via docker run"
    exit 1
  fi
fi
echo "Database container is running."

# 3. Execute Gradle task: generateOpenApiDocs
echo "Running './gradlew generateOpenApiDocs' ..."
./gradlew generateOpenApiDocs
if [ $? -ne 0 ]; then
  echo "[ERROR] Gradle task generateOpenApiDocs failed"
  exit 1
fi

rm -rf openapi-generator
mkdir openapi-generator

cat <<EOF > openapi-generator/.openapi-generator-ignore
# OpenAPI Generator Ignore
.gitignore
.npmignore
git_push.sh
EOF

cat <<EOF > openapi-generator/package.json
{
  "name": "@leesm0518/aiminder-api",
  "version": "$VERSION",
  "type": "module",
  "repository": {
    "url": "https://github.com/AIMinderTeam/aiminder-server"
  },
  "scripts": {
    "build": "rm -rf dist/* && tsc -p tsconfig.json"
  },
  "main": "dist/index.js",
  "module": "dist/index.js",
  "types": "dist/index.d.ts",
  "exports": {
    ".": {
      "import": "./dist/index.js",
      "require": "./dist/index.js"
    }
  },
  "devDependencies": {
    "typescript": "^5.8.2"
  },
  "dependencies": {
    "axios": "^1.8.2"
  }
}
EOF

cat <<EOF > openapi-generator/tsconfig-base.json
{
  "compilerOptions": {
    "target": "ES5",
    "allowJs": true,
    "allowSyntheticDefaultImports": true,
    "baseUrl": "src",
    "declaration": true,
    "esModuleInterop": true,
    "inlineSourceMap": false,
    "listEmittedFiles": false,
    "listFiles": false,
    "moduleResolution": "node",
    "noFallthroughCasesInSwitch": true,
    "pretty": true,
    "resolveJsonModule": true,
    "rootDir": "src",
    "skipLibCheck": true,
    "strict": true,
    "traceResolution": false
  },
  "compileOnSave": false,
  "exclude": ["node_modules", "dist"],
  "include": ["src"]
}
EOF

cat <<EOF > openapi-generator/tsconfig.json
{
  "extends": "./tsconfig-base.json",
  "compilerOptions": {
    "module": "esnext",
    "outDir": "dist",
    "target": "esnext"
  }
}
EOF

cat <<EOF > openapi-generator/.npmrc
@leesm0518:registry=https://npm.pkg.github.com
@LeeSM0518:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken="$PASSWORD"
EOF

cat <<EOF > openapi-generator/.npmignore
**/*
!/dist/**
EOF

cp "$(ls -t build/openapi.json | head -n 1)" openapi-generator/openapi.json

docker run --rm \
  -v "$(pwd)/openapi-generator":/local openapitools/openapi-generator-cli generate \
  -i /local/openapi.json \
  -g typescript-axios \
  -o /local/src \
  --additional-properties=withSeparateModelsAndApi=true,apiPackage=apis,modelPackage=models,useSingleRequestParameter=true

cd openapi-generator && npm i && npm run build

# 5. npm publish 시도 및 실패 시 버전 업데이트 후 재시도
set +e
npm publish -f
PUBLISH_EXIT_CODE=$?
set -e

if [ $PUBLISH_EXIT_CODE -ne 0 ]; then
  echo "npm publish failed, updating version..."
  # 현재 날짜와 시간을 UTC 기준으로 YYYYMMDDHHmmss 형식으로 생성
  CURRENT_DATE=$(date -u +"%Y%m%d%H%M%S")
  NEW_VERSION="${VERSION}-${CURRENT_DATE}"
  echo "Updating version to: $NEW_VERSION"

  # OS별 sed 옵션 처리: macOS와 Linux 호환
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/\"version\": *\"$VERSION\"/\"version\": \"$NEW_VERSION\"/" package.json
  else
    sed -i.bak "s/\"version\": *\"$VERSION\"/\"version\": \"$NEW_VERSION\"/" package.json
  fi

  # 다시 npm publish 수행
  npm publish -f
fi

echo "All steps completed successfully."