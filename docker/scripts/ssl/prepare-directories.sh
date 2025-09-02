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
            echo "Unknown option: $1"
            echo "Usage: $0 --deployment-dir <path> --backup-dir <path> --user <username> [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$DEPLOYMENT_DIR" ]; then
    echo -e "${RED}Error: --deployment-dir is required${NC}"
    exit 1
fi

if [ -z "$BACKUP_DIR" ]; then
    echo -e "${RED}Error: --backup-dir is required${NC}"
    exit 1
fi

if [ -z "$USER" ]; then
    echo -e "${RED}Error: --user is required${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "Debug mode enabled"
    echo "Deployment directory: $DEPLOYMENT_DIR"
    echo "Backup directory: $BACKUP_DIR"
    echo "User: $USER"
fi

echo "=== Preparing Directories ==="

# Function to create and set permissions for a directory
create_directory() {
    local dir=$1
    local purpose=$2
    
    echo -e "${BLUE}Preparing $purpose directory: $dir${NC}"
    
    if [ ! -d "$dir" ]; then
        echo "Creating directory..."
        mkdir -p "$dir"
        echo -e "${GREEN}✅ Directory created${NC}"
    else
        echo -e "${GREEN}✅ Directory already exists${NC}"
    fi
    
    # Set ownership and permissions
    chown "$USER:$USER" "$dir"
    chmod 755 "$dir"
    
    # Verify directory
    if [ -d "$dir" ] && [ -w "$dir" ]; then
        echo -e "${GREEN}✅ Directory is ready${NC}"
        
        if [ "$DEBUG" = true ]; then
            echo "Directory details:"
            ls -ld "$dir"
        fi
    else
        echo -e "${RED}❌ Failed to prepare directory${NC}"
        return 1
    fi
    
    echo ""
}

# Create deployment directory
if ! create_directory "$DEPLOYMENT_DIR" "deployment"; then
    echo -e "${RED}Failed to prepare deployment directory${NC}"
    exit 1
fi

# Create backup directory
if ! create_directory "$BACKUP_DIR" "backup"; then
    echo -e "${RED}Failed to prepare backup directory${NC}"
    exit 1
fi

# Create subdirectories for deployment
echo -e "${BLUE}Creating deployment subdirectories...${NC}"

# Docker compose files directory
DOCKER_DIR="$DEPLOYMENT_DIR/docker"
if [ ! -d "$DOCKER_DIR" ]; then
    mkdir -p "$DOCKER_DIR"
    chown "$USER:$USER" "$DOCKER_DIR"
    chmod 755 "$DOCKER_DIR"
    echo -e "${GREEN}✅ Created docker directory${NC}"
fi

# Scripts directory
SCRIPTS_DIR="$DEPLOYMENT_DIR/scripts"
if [ ! -d "$SCRIPTS_DIR" ]; then
    mkdir -p "$SCRIPTS_DIR/ssl"
    mkdir -p "$SCRIPTS_DIR/deployment"
    chown -R "$USER:$USER" "$SCRIPTS_DIR"
    chmod -R 755 "$SCRIPTS_DIR"
    echo -e "${GREEN}✅ Created scripts directories${NC}"
fi

# Certificate directory (prepare but don't create - Let's Encrypt will handle this)
CERT_DIR="$DEPLOYMENT_DIR/cert"
if [ ! -d "$CERT_DIR" ]; then
    echo -e "${YELLOW}ℹ️  Certificate directory will be created during SSL initialization${NC}"
fi

# Environment files directory
ENV_DIR="$DEPLOYMENT_DIR/env"
if [ ! -d "$ENV_DIR" ]; then
    mkdir -p "$ENV_DIR"
    chown "$USER:$USER" "$ENV_DIR"
    chmod 700 "$ENV_DIR"  # Restricted permissions for sensitive files
    echo -e "${GREEN}✅ Created environment directory with restricted permissions${NC}"
fi

# Show directory structure
echo ""
echo "=== Directory Structure ==="
echo "Deployment directory: $DEPLOYMENT_DIR"
tree -L 2 "$DEPLOYMENT_DIR" 2>/dev/null || {
    echo "├── docker/"
    echo "├── scripts/"
    echo "│   ├── ssl/"
    echo "│   └── deployment/"
    echo "├── env/"
    echo "└── cert/ (will be created during SSL setup)"
}

echo ""
echo "Backup directory: $BACKUP_DIR"
ls -la "$BACKUP_DIR" 2>/dev/null | head -5 || echo "Empty directory"

# Final verification
echo ""
echo "=== Directory Verification ==="

# Check write permissions
for dir in "$DEPLOYMENT_DIR" "$BACKUP_DIR" "$DOCKER_DIR" "$SCRIPTS_DIR" "$ENV_DIR"; do
    if [ -d "$dir" ] && [ -w "$dir" ]; then
        echo -e "${GREEN}✅ $dir - OK${NC}"
    else
        echo -e "${RED}❌ $dir - Failed${NC}"
        exit 1
    fi
done

echo ""
echo -e "${GREEN}✅ All directories prepared successfully${NC}"
exit 0