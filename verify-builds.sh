#!/bin/bash
# Build Verification Script for APSET Project

echo "=========================================="
echo "  Build Verification Script"
echo "=========================================="
echo ""

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SUCCESS=true

# Function to check if command succeeded
check_result() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Success${NC}"
    else
        echo -e "${RED}✗ Failed${NC}"
        SUCCESS=false
    fi
}

echo -e "${YELLOW}1. Verifying Backend (Spring Boot)...${NC}"
cd backend

echo "  Cleaning previous builds..."
mvn clean -q
check_result

echo "  Compiling..."
mvn compile -q
check_result

echo "  Packaging..."
mvn package -DskipTests -q
check_result

if [ -f "target/*.jar" ]; then
    echo -e "  ${GREEN}✓ JAR file created successfully${NC}"
else
    echo -e "  ${RED}✗ JAR file not found${NC}"
    SUCCESS=false
fi

cd ..
echo ""

echo -e "${YELLOW}2. Verifying Frontend (React/Vite)...${NC}"
cd frontend

echo "  Installing dependencies..."
npm install --silent
check_result

echo "  Building for production..."
npm run build 2>&1 | tail -5
check_result

if [ -d "dist" ] && [ "$(ls -A dist)" ]; then
    echo -e "  ${GREEN}✓ Build output created${NC}"
else
    echo -e "  ${RED}✗ Build output not found${NC}"
    SUCCESS=false
fi

cd ..
echo ""

echo -e "${YELLOW}3. Checking AI Service...${NC}"
cd ai-service

if [ -d "venv" ]; then
    echo "  Virtual environment exists"
else
    echo "  Creating virtual environment..."
    python3 -m venv venv
fi

echo "  Installing requirements..."
source venv/bin/activate 2>/dev/null || venv\Scripts\activate 2>/dev/null
pip install -q -r requirements.txt
check_result

cd ..
echo ""

echo "=========================================="
if [ "$SUCCESS" = true ]; then
    echo -e "${GREEN}All builds successful!${NC}"
    echo ""
    echo "Project is ready for deployment:"
    echo "  - Backend JAR: backend/target/*.jar"
    echo "  - Frontend dist: frontend/dist/"
    echo "  - AI Service: ai-service/ (Python)"
else
    echo -e "${RED}Some builds failed. Please check the errors above.${NC}"
    exit 1
fi
echo "=========================================="
