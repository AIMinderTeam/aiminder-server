#!/bin/bash
set -euo pipefail

PASSWORD=""
VERSION="0.0.0"
API_DOCS_URL="http://localhost:8080/api/v3/api-docs"
BOOT_LOG=".openapi-boot.log"
ENABLE_NPM_PUBLISH="${ENABLE_NPM_PUBLISH:-}" # set to any non-empty value to publish

echo "VERSION=$VERSION"

# Parse args: -password and -version
while [ "$#" -gt 0 ]; do
  case "$1" in
    -password)
      PASSWORD="$2"; shift 2 ;;
    -version)
      VERSION="$2"; shift 2 ;;
    *) echo "Unknown parameter: $1"; exit 1 ;;
  esac
done

# Ensure Docker daemon is up (if available)
if command -v docker >/dev/null 2>&1; then
  if ! docker ps >/dev/null 2>&1; then
    echo "[WARN] Docker seems not running or not accessible. Steps using Docker may fail."
  fi
fi

# 1) Ensure database container is up (best-effort)
if command -v docker >/dev/null 2>&1; then
  echo "Checking if database container 'aiminder-database' is already running..."
  if [ "$(docker ps -q -f name=aiminder-database)" ]; then
    echo "Database container is already running."
  else
    echo "Starting database container using docker run..."
    if ! docker run -d \
      --name aiminder-database \
      -e POSTGRES_DB=aiminderdb \
      -e POSTGRES_USER=aiminder \
      -e POSTGRES_PASSWORD=aiminder \
      -p 5432:5432 \
      postgres:14; then
      echo "[WARN] Failed to start database container via docker run. Continuing; app may start without DB for api-docs."
    fi
  fi
fi

# 3) Generate openapi.json via gradle task
if ! ./gradlew generateOpenApiDocs; then
  echo "[ERROR] Gradle task generateOpenApiDocs failed. See ${BOOT_LOG}"
  exit 1
fi

# Stop the background app before continuing heavy steps
stop_app

rm -rf openapi-generator
mkdir -p openapi-generator

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

# 4) Generate TypeScript client using OpenAPI Generator (Docker image)
docker run --rm \
  -v "$(pwd)/openapi-generator":/local openapitools/openapi-generator-cli generate \
  -i /local/openapi.json \
  -g typescript-axios \
  -o /local/src \
  --additional-properties=withSeparateModelsAndApi=true,apiPackage=apis,modelPackage=models,useSingleRequestParameter=true

cd openapi-generator

# 5) Install deps; fallback to known-good versions if initial install fails (e.g., E404)
set +e
npm i
NPM_I_CODE=$?
set -e
if [ $NPM_I_CODE -ne 0 ]; then
  echo "[WARN] npm install failed. Falling back to stable dependency versions..."
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' 's/"typescript": *"[^"]\+"/"typescript": "^5.6.3"/' package.json
    sed -i '' 's/"axios": *"[^"]\+"/"axios": "^1.7.7"/' package.json
  else
    sed -i.bak 's/"typescript": *"[^"]\+"/"typescript": "^5.6.3"/' package.json
    sed -i.bak 's/"axios": *"[^"]\+"/"axios": "^1.7.7"/' package.json
  fi
  npm i
fi

npm run build

# 6) Optionally publish to GitHub Packages
if [ -n "$ENABLE_NPM_PUBLISH" ]; then
  echo "Publishing package to GitHub Packages..."
  set +e
  npm publish -f
  PUBLISH_EXIT_CODE=$?
  set -e
  if [ $PUBLISH_EXIT_CODE -ne 0 ]; then
    echo "npm publish failed, updating version and retrying..."
    CURRENT_DATE=$(date -u +"%Y%m%d%H%M%S")
    NEW_VERSION="${VERSION}-${CURRENT_DATE}"
    echo "Updating version to: $NEW_VERSION"
    if [[ "$OSTYPE" == "darwin"* ]]; then
      sed -i '' "s/\"version\": *\"$VERSION\"/\"version\": \"$NEW_VERSION\"/" package.json
    else
      sed -i.bak "s/\"version\": *\"$VERSION\"/\"version\": \"$NEW_VERSION\"/" package.json
    fi
    npm publish -f
  fi
else
  echo "Skipping npm publish (ENABLE_NPM_PUBLISH not set)."
fi

echo "All steps completed successfully. Output: openapi-generator/dist"
