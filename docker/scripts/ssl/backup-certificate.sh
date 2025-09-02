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
            echo "Unknown option: $1"
            echo "Usage: $0 --cert-dir <path> --backup-dir <path> [--domain <domain>] [--keep-count <count>] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$CERT_DIR" ]; then
    echo -e "${RED}Error: --cert-dir is required${NC}"
    exit 1
fi

if [ -z "$BACKUP_DIR" ]; then
    echo -e "${RED}Error: --backup-dir is required${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "Debug mode enabled"
    echo "Certificate directory: $CERT_DIR"
    echo "Backup directory: $BACKUP_DIR"
    echo "Keep count: $KEEP_COUNT"
    [ -n "$DOMAIN" ] && echo "Domain: $DOMAIN"
fi

echo "=== SSL Certificate Backup ==="

# Create backup directory if it doesn't exist
if [ ! -d "$BACKUP_DIR" ]; then
    echo "Creating backup directory: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
    chmod 755 "$BACKUP_DIR"
fi

# Check if certificate directory exists
if [ ! -d "$CERT_DIR" ]; then
    echo -e "${YELLOW}ℹ️  Certificate directory does not exist (initial deployment)${NC}"
    exit 0
fi

# Check if there are certificates to backup
if [ ! -d "$CERT_DIR/letsencrypt/live" ] || [ -z "$(ls -A $CERT_DIR/letsencrypt/live 2>/dev/null)" ]; then
    echo -e "${YELLOW}ℹ️  No certificates to backup (empty directory)${NC}"
    exit 0
fi

# Generate backup filename
TIMESTAMP=$(date +%Y%m%d%H%M%S)
if [ -n "$DOMAIN" ]; then
    BACKUP_FILE="cert-backup-${DOMAIN}-${TIMESTAMP}.tar.gz"
else
    BACKUP_FILE="cert-backup-${TIMESTAMP}.tar.gz"
fi

echo -e "${BLUE}📦 Creating backup: $BACKUP_FILE${NC}"

# Create backup
PARENT_DIR=$(dirname "$CERT_DIR")
CERT_BASENAME=$(basename "$CERT_DIR")

cd "$PARENT_DIR"
if sudo tar -czf "$BACKUP_DIR/$BACKUP_FILE" "$CERT_BASENAME/" 2>/dev/null; then
    echo -e "${GREEN}✅ Backup created successfully${NC}"
else
    echo -e "${RED}❌ Failed to create backup${NC}"
    exit 1
fi

# Set proper permissions
sudo chown $(whoami):$(whoami) "$BACKUP_DIR/$BACKUP_FILE"
chmod 644 "$BACKUP_DIR/$BACKUP_FILE"

# Get backup file size
BACKUP_SIZE=$(du -h "$BACKUP_DIR/$BACKUP_FILE" | cut -f1)
echo "Backup size: $BACKUP_SIZE"

# Verify backup integrity
echo "Verifying backup integrity..."
if sudo tar -tzf "$BACKUP_DIR/$BACKUP_FILE" >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Backup integrity verified${NC}"
else
    echo -e "${RED}❌ Backup verification failed${NC}"
    rm -f "$BACKUP_DIR/$BACKUP_FILE"
    exit 1
fi

# List contents if debug mode
if [ "$DEBUG" = true ]; then
    echo ""
    echo "Backup contents:"
    sudo tar -tzf "$BACKUP_DIR/$BACKUP_FILE" | head -20
    echo "..."
fi

# Clean up old backups
echo ""
echo -e "${BLUE}🗑️  Cleaning up old backups (keeping last $KEEP_COUNT)${NC}"

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
        echo "Removing old backup: $old_backup"
        rm -f "$old_backup"
    done
    echo -e "${GREEN}✅ Old backups cleaned${NC}"
else
    echo "No cleanup needed ($BACKUP_COUNT backups found)"
fi

# Show current backups
echo ""
echo "=== Current Backups ==="
ls -lah $PATTERN 2>/dev/null | tail -$KEEP_COUNT || echo "No backup files found"

echo ""
echo -e "${GREEN}✅ Backup completed successfully${NC}"
exit 0