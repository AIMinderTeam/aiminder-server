#!/bin/bash

set -euo pipefail

# Default values
DOMAIN=""
DEPLOYMENT_DIR=""
ENV_FILE=""
RENEW_THRESHOLD=30
DEBUG=false
FORCE_RENEW=false

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
        --deployment-dir)
            DEPLOYMENT_DIR="$2"
            shift 2
            ;;
        --env-file)
            ENV_FILE="$2"
            shift 2
            ;;
        --renew-threshold)
            RENEW_THRESHOLD="$2"
            shift 2
            ;;
        --force-renew)
            FORCE_RENEW=true
            shift
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 --domain <domain> --deployment-dir <path> --env-file <path> [--renew-threshold <days>] [--force-renew] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$DOMAIN" ]; then
    echo -e "${RED}Error: --domain is required${NC}"
    exit 1
fi

if [ -z "$DEPLOYMENT_DIR" ]; then
    echo -e "${RED}Error: --deployment-dir is required${NC}"
    exit 1
fi

if [ -z "$ENV_FILE" ]; then
    echo -e "${RED}Error: --env-file is required${NC}"
    exit 1
fi

# Check if env file exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: Environment file not found: $ENV_FILE${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "Debug mode enabled"
    echo "Domain: $DOMAIN"
    echo "Deployment directory: $DEPLOYMENT_DIR"
    echo "Environment file: $ENV_FILE"
    echo "Renewal threshold: $RENEW_THRESHOLD days"
    echo "Force renewal: $FORCE_RENEW"
fi

echo "=== SSL Auto-Detection Deployment ==="

# Change to deployment directory
cd "$DEPLOYMENT_DIR"

# Certificate paths
CERT_DIR="./cert"
CERT_FILE="${CERT_DIR}/letsencrypt/live/${DOMAIN}/cert.pem"

# Function to check certificate validity
check_certificate_validity() {
    if [ ! -f "$CERT_FILE" ]; then
        echo -e "${YELLOW}⚠️  No certificate found${NC}"
        return 1
    fi
    
    echo "Checking certificate validity..."
    
    # Get certificate expiry date
    CERT_EXPIRY=$(sudo openssl x509 -in "$CERT_FILE" -noout -enddate 2>/dev/null | cut -d= -f2)
    if [ -z "$CERT_EXPIRY" ]; then
        echo -e "${RED}❌ Failed to read certificate${NC}"
        return 1
    fi
    
    # Convert to epoch for comparison
    CERT_EXPIRY_EPOCH=$(date -d "$CERT_EXPIRY" +%s 2>/dev/null || date -j -f "%b %d %H:%M:%S %Y %Z" "$CERT_EXPIRY" +%s 2>/dev/null)
    CURRENT_EPOCH=$(date +%s)
    
    if [ "$CERT_EXPIRY_EPOCH" -le "$CURRENT_EPOCH" ]; then
        echo -e "${RED}❌ Certificate has expired${NC}"
        return 1
    fi
    
    # Calculate days until expiry
    DAYS_UNTIL_EXPIRY=$(( ($CERT_EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))
    echo "Certificate valid for $DAYS_UNTIL_EXPIRY more days"
    
    if [ $DAYS_UNTIL_EXPIRY -le $RENEW_THRESHOLD ]; then
        echo -e "${YELLOW}⚠️  Certificate renewal recommended (expires in ${DAYS_UNTIL_EXPIRY} days)${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ Certificate is valid${NC}"
    return 0
}

# Function to stop running containers
stop_containers() {
    echo -e "${BLUE}Stopping existing containers...${NC}"
    sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml down || true
    echo -e "${GREEN}✅ Containers stopped${NC}"
}

# Function to deploy with existing certificate
deploy_with_existing_cert() {
    echo -e "${BLUE}Deploying with existing SSL certificate...${NC}"
    
    stop_containers
    
    echo "Starting services..."
    if sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml up -d; then
        echo -e "${GREEN}✅ Services started with existing certificate${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to start services${NC}"
        return 1
    fi
}

# Function to deploy with new certificate
deploy_with_new_cert() {
    echo -e "${BLUE}Deploying with new SSL certificate...${NC}"
    
    stop_containers
    
    # Stop any existing certbot containers
    sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-certbot-init.yml down || true
    
    # Create certificate directories if they don't exist
    if [ ! -d "$CERT_DIR/webroot" ] || [ ! -d "$CERT_DIR/letsencrypt" ]; then
        echo "Creating certificate directories..."
        sudo mkdir -p "$CERT_DIR/webroot" "$CERT_DIR/letsencrypt"
        echo -e "${GREEN}✅ Directories created${NC}"
    fi
    
    echo "Initiating SSL certificate generation..."
    if sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-certbot-init.yml up --abort-on-container-exit; then
        echo -e "${GREEN}✅ Certificate generated successfully${NC}"
        
        # Clean up certbot containers
        sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-certbot-init.yml down
        
        # Start services with new certificate
        echo "Starting services with new certificate..."
        if sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml up -d; then
            echo -e "${GREEN}✅ Services started with new certificate${NC}"
            return 0
        else
            echo -e "${RED}❌ Failed to start services${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ Failed to generate certificate${NC}"
        return 1
    fi
}

# Main deployment logic
NEED_NEW_CERT=false

# Check if force renewal is requested
if [ "$FORCE_RENEW" = true ]; then
    echo -e "${YELLOW}Force renewal requested${NC}"
    NEED_NEW_CERT=true
else
    # Check existing certificate
    if check_certificate_validity; then
        NEED_NEW_CERT=false
    else
        NEED_NEW_CERT=true
    fi
fi

# Deploy based on certificate status
if [ "$NEED_NEW_CERT" = true ]; then
    if ! deploy_with_new_cert; then
        echo -e "${RED}Deployment failed${NC}"
        exit 1
    fi
else
    if ! deploy_with_existing_cert; then
        echo -e "${RED}Deployment failed${NC}"
        exit 1
    fi
fi

# Verify deployment
echo ""
echo "=== Deployment Verification ==="

# Check running containers
echo "Running containers:"
sudo docker-compose --env-file "$ENV_FILE" -f docker-compose-ssl.yml ps

# Check SSL certificate status
if [ -f "$CERT_FILE" ]; then
    echo ""
    echo "SSL Certificate Status:"
    sudo openssl x509 -in "$CERT_FILE" -text -noout | grep -E "(Subject:|Not After)" || echo "Failed to read certificate"
    
    if [ "$DEBUG" = true ]; then
        echo ""
        echo "Certificate details:"
        sudo openssl x509 -in "$CERT_FILE" -noout -subject -issuer -dates
    fi
else
    echo -e "${YELLOW}⚠️  No SSL certificate found after deployment${NC}"
fi

echo ""
echo -e "${GREEN}✅ Deployment completed successfully${NC}"
exit 0