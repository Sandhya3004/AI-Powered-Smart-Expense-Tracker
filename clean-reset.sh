#!/bin/bash
# CLEAN RESET SCRIPT - Fix 3.89GB Repository Size
# This script removes all git history with large files and creates fresh clean repo

echo "=========================================="
echo "  🔥 CLEAN RESET - Repository Size Fix"
echo "=========================================="
echo ""
echo "⚠️  WARNING: This will DELETE all git history!"
echo "   Make sure you have a backup or are in the right directory."
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_DIR="/home/regus/projects/APSET"
BACKUP_DIR="/home/regus/projects/APSET_backup_$(date +%Y%m%d_%H%M%S)"

echo -e "${YELLOW}Step 1: Creating backup...${NC}"
echo "   Source: $PROJECT_DIR"
echo "   Backup: $BACKUP_DIR"
cp -r "$PROJECT_DIR" "$BACKUP_DIR"
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Backup created successfully${NC}"
else
    echo -e "${RED}✗ Backup failed! Aborting.${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Checking current size...${NC}"
cd "$PROJECT_DIR"
echo -n "   Total size: "
du -sh .

echo ""
echo -e "${YELLOW}Step 3: Removing .git directory (deletes all history)...${NC}"
rm -rf .git
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Git history removed${NC}"
else
    echo -e "${RED}✗ Failed to remove .git${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 4: Verifying .gitignore exists...${NC}"
if [ ! -f ".gitignore" ]; then
    echo "   Creating .gitignore..."
    cat > .gitignore << 'EOF'
# Node
node_modules/
dist/
build/

# Java / Spring Boot
target/
*.jar
*.war

# Python
__pycache__/
*.py[cod]
venv/
env/
ENV/
*.egg-info/

# Environment files
.env
.env.*
.env.local

# Logs
*.log
logs/

# IDE files
.vscode/
.idea/
*.iml
*.iws
*.ipr
.classpath
.project
.settings/

# OS files
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db

# Build outputs
/dist
/build
/out

# Uploads (user data)
uploads/
receipts/
*.jpg
*.jpeg
*.png
!public/*.jpg
!public/*.png

# Database
*.db
*.sqlite
*.sqlite3

# Coverage
coverage/
*.lcov

# Temporary files
*.tmp
*.temp
*.swp
*.swo
*~

# Spring Boot
.mvn/
!**/.mvn/wrapper/maven-wrapper.properties

# Misc
HELP.md
*.Zone.Identifier
EOF
    echo -e "${GREEN}✓ .gitignore created${NC}"
else
    echo -e "${GREEN}✓ .gitignore already exists${NC}"
fi

echo ""
echo -e "${YELLOW}Step 5: Initializing fresh Git repository...${NC}"
git init
git branch -M main
git remote add origin https://github.com/Sandhya3004/AI-Powered-Smart-Expense-Tracker.git
echo -e "${GREEN}✓ Git initialized${NC}"

echo ""
echo -e "${YELLOW}Step 6: Adding clean files to git...${NC}"
git add .
echo -e "${GREEN}✓ Files staged${NC}"

echo ""
echo -e "${YELLOW}Step 7: Committing...${NC}"
git commit -m "Clean project for deployment - remove large files from history"
echo -e "${GREEN}✓ Committed${NC}"

echo ""
echo -e "${YELLOW}Step 8: Final size check...${NC}"
echo -n "   Repository size: "
du -sh .

echo ""
echo -e "${YELLOW}Step 9: Pushing to GitHub...${NC}"
echo "   Running: git push -u origin main --force"
git push -u origin main --force
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}==========================================${NC}"
    echo -e "${GREEN}  🎉 SUCCESS! Repository pushed to GitHub${NC}"
    echo -e "${GREEN}==========================================${NC}"
    echo ""
    echo "Repository URL:"
    echo "   https://github.com/Sandhya3004/AI-Powered-Smart-Expense-Tracker"
    echo ""
    echo -e "${BLUE}Backup location:${NC}"
    echo "   $BACKUP_DIR"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo "   1. Verify on GitHub that files are uploaded"
    echo "   2. Proceed to deployment on Render/Vercel"
    echo "   3. Remove backup when confirmed working: rm -rf $BACKUP_DIR"
else
    echo ""
    echo -e "${RED}==========================================${NC}"
    echo -e "${RED}  ❌ Push failed${NC}"
    echo -e "${RED}==========================================${NC}"
    echo ""
    echo "Your backup is at: $BACKUP_DIR"
fi

echo ""
echo "Done!"
