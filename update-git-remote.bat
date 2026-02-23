@echo off
echo Checking git remote...
git remote -v

echo.
echo Updating remote to new repository...
git remote set-url origin https://github.com/ramniks05/rccmsch-backend.git

echo.
echo Verifying new remote...
git remote -v

echo.
echo Checking git status...
git status

echo.
echo Adding all files...
git add .

echo.
echo Committing changes...
git commit -m "Initial commit: RCCMS Chandigarh Backend - Migrated from Manipur implementation"

echo.
echo Pushing to new repository...
echo Note: You may need to authenticate with GitHub
git push -u origin main

echo.
echo Done!
