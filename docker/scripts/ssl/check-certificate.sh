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
            echo "Unknown option: $1"
            echo "Usage: $0 --domain <domain> --cert-dir <path> [--days-threshold <days>] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$DOMAIN" ]; then
    echo -e "${RED}Error: --domain is required${NC}"
    exit 1
fi

if [ -z "$CERT_DIR" ]; then
    echo -e "${RED}Error: --cert-dir is required${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "Debug mode enabled"
    echo "Domain: $DOMAIN"
    echo "Certificate directory: $CERT_DIR"
    echo "Days threshold: $DAYS_THRESHOLD"
fi

# Certificate file path
CERT_FILE="${CERT_DIR}/live/${DOMAIN}/cert.pem"

echo "=== SSL Certificate Status Check ==="
echo "Domain: $DOMAIN"
echo "Certificate path: $CERT_FILE"

# Check if certificate exists
if [ ! -f "$CERT_FILE" ]; then
    echo -e "${YELLOW}⚠️  No SSL certificate found${NC}"
    echo "Status: MISSING"
    exit 2
fi

echo -e "${GREEN}✅ SSL certificate found${NC}"

# Check certificate validity
if ! sudo openssl x509 -in "$CERT_FILE" -noout -checkend 0 >/dev/null 2>&1; then
    echo -e "${RED}❌ Certificate has expired${NC}"
    echo "Status: EXPIRED"
    exit 3
fi

# Get certificate expiry date
CERT_EXPIRY_DATE=$(sudo openssl x509 -in "$CERT_FILE" -noout -enddate 2>/dev/null | cut -d= -f2)
echo "Certificate expiry date: $CERT_EXPIRY_DATE"

# Convert expiry date to seconds since epoch
CERT_EXPIRY_EPOCH=$(date -d "$CERT_EXPIRY_DATE" +%s 2>/dev/null || date -j -f "%b %d %H:%M:%S %Y %Z" "$CERT_EXPIRY_DATE" +%s 2>/dev/null)
CURRENT_EPOCH=$(date +%s)

# Calculate days until expiry
SECONDS_UNTIL_EXPIRY=$((CERT_EXPIRY_EPOCH - CURRENT_EPOCH))
DAYS_UNTIL_EXPIRY=$((SECONDS_UNTIL_EXPIRY / 86400))

echo "Days until expiry: $DAYS_UNTIL_EXPIRY"

# Check if renewal is needed
if [ $DAYS_UNTIL_EXPIRY -le $DAYS_THRESHOLD ]; then
    echo -e "${YELLOW}⚠️  Certificate renewal recommended (expires in ${DAYS_UNTIL_EXPIRY} days)${NC}"
    echo "Status: RENEWAL_NEEDED"
    exit 4
fi

# Get certificate details
if [ "$DEBUG" = true ]; then
    echo ""
    echo "Certificate details:"
    sudo openssl x509 -in "$CERT_FILE" -noout -subject -issuer -dates
fi

echo -e "${GREEN}✅ Certificate is valid for ${DAYS_UNTIL_EXPIRY} more days${NC}"
echo "Status: VALID"
exit 0