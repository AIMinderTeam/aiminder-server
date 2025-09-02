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
            echo "Unknown option: $1"
            echo "Usage: $0 --env-file <path> [--required-vars <var1,var2,...>] [--export] [--debug]"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [ -z "$ENV_FILE" ]; then
    echo -e "${RED}Error: --env-file is required${NC}"
    exit 1
fi

# Debug output
if [ "$DEBUG" = true ]; then
    echo "Debug mode enabled"
    echo "Environment file: $ENV_FILE"
    [ -n "$REQUIRED_VARS" ] && echo "Required variables: $REQUIRED_VARS"
    echo "Export variables: $EXPORT_VARS"
fi

echo "=== Environment Verification ==="

# Check if environment file exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}❌ Environment file not found: $ENV_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Environment file found${NC}"

# Load environment variables
echo "Loading environment variables..."

# Create a temporary file to store the environment
TEMP_ENV=$(mktemp)

# Parse the .env file and remove comments and empty lines
grep -v '^#' "$ENV_FILE" | grep -v '^[[:space:]]*$' > "$TEMP_ENV" || true

# Check if file is empty
if [ ! -s "$TEMP_ENV" ]; then
    echo -e "${YELLOW}⚠️  Environment file is empty or contains only comments${NC}"
    rm -f "$TEMP_ENV"
    exit 1
fi

# Count variables
VAR_COUNT=$(wc -l < "$TEMP_ENV")
echo "Found $VAR_COUNT environment variables"

# Function to check if a variable is set
check_variable() {
    local var_name=$1
    local var_value=""
    
    # Extract variable value from env file
    if grep -q "^${var_name}=" "$TEMP_ENV"; then
        var_value=$(grep "^${var_name}=" "$TEMP_ENV" | cut -d= -f2- | sed 's/^"//;s/"$//')
        
        if [ -z "$var_value" ]; then
            echo -e "${RED}  ❌ $var_name is empty${NC}"
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
        echo -e "${RED}  ❌ $var_name is not defined${NC}"
        return 1
    fi
}

# Verify required variables if specified
if [ -n "$REQUIRED_VARS" ]; then
    echo ""
    echo "Checking required variables:"
    
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
        echo -e "${RED}Environment validation failed:${NC}"
        
        if [ ${#MISSING_VARS[@]} -gt 0 ]; then
            echo "Missing variables:"
            for var in "${MISSING_VARS[@]}"; do
                echo "  - $var"
            done
        fi
        
        if [ ${#EMPTY_VARS[@]} -gt 0 ]; then
            echo "Empty variables:"
            for var in "${EMPTY_VARS[@]}"; do
                echo "  - $var"
            done
        fi
        
        rm -f "$TEMP_ENV"
        exit 1
    fi
    
    echo ""
    echo -e "${GREEN}✅ All required variables are present${NC}"
else
    # List all variables if no specific requirements
    echo ""
    echo "Environment variables found:"
    
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
    echo -e "${BLUE}ℹ️  Variables have been exported to the current shell${NC}"
fi

# Clean up
rm -f "$TEMP_ENV"

echo ""
echo -e "${GREEN}✅ Environment verification completed successfully${NC}"
exit 0