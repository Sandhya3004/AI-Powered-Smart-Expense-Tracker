#!/bin/bash
# Git Cleanup Script for APSET Project
# Run this from the root of the repository

echo "=========================================="
echo "  Git Repository Cleanup Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Step 1: Checking Git status...${NC}"
git status

echo ""
echo -e "${YELLOW}Step 2: Removing tracked files that should be ignored...${NC}"

# Remove large directories from git tracking (keeping local files)
if git ls-files | grep -q "node_modules"; then
    echo "Removing node_modules from tracking..."
    git rm -r --cached frontend/node_modules 2>/dev/null || true
    git rm -r --cached ai-service/venv 2>/dev/null || true
fi

if git ls-files | grep -q "target/"; then
    echo "Removing target/ from tracking..."
    git rm -r --cached backend/target 2>/dev/null || true
fi

if git ls-files | grep -q "dist/"; then
    echo "Removing dist/ from tracking..."
    git rm -r --cached frontend/dist 2>/dev/null || true
fi

if git ls-files | grep -q "__pycache__"; then
    echo "Removing __pycache__/ from tracking..."
    git rm -r --cached ai-service/__pycache__ 2>/dev/null || true
fi

if git ls-files | grep -q "uploads/"; then
    echo "Removing uploads/ from tracking..."
    git rm -r --cached backend/uploads 2>/dev/null || true
fi

echo ""
echo -e "${YELLOW}Step 3: Adding .gitignore files...${NC}"
git add .gitignore

echo ""
echo -e "${YELLOW}Step 4: Adding all remaining files...${NC}"
git add .

echo ""
echo -e "${GREEN}Repository cleanup complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Review changes: git status"
echo "  2. Commit: git commit -m 'Clean repo for deployment - remove build files and large binaries'"
echo "  3. Push: git push origin main --force"
echo ""
echo -e "${YELLOW}Current repository size:${NC}"
du -sh .git 2>/dev/null || echo "Unable to calculate"
