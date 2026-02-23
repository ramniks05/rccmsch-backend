# Git Migration Instructions

## ✅ Step 1: Remote URL Updated
The git remote URL has been updated from:
- **Old:** `https://github.com/ramniks05/rccmsmp-backend.git`
- **New:** `https://github.com/ramniks05/rccmsch-backend.git`

## 📋 Step 2: Run These Commands

Open your terminal/command prompt in the project directory and run:

### 1. Verify Remote URL
```bash
git remote -v
```
**Expected output:**
```
origin  https://github.com/ramniks05/rccmsch-backend.git (fetch)
origin  https://github.com/ramniks05/rccmsch-backend.git (push)
```

### 2. Check Current Status
```bash
git status
```

### 3. Add All Files
```bash
git add .
```

### 4. Commit Changes
```bash
git commit -m "Initial commit: RCCMS Chandigarh Backend - Migrated from Manipur implementation"
```

### 5. Push to New Repository
```bash
git push -u origin main
```

**Note:** If the repository is empty, you may need to use:
```bash
git push -u origin main --force
```

Or if you're on a different branch:
```bash
git push -u origin <your-branch-name>
```

## 🔐 Authentication

If prompted for authentication:
- **Username:** Your GitHub username
- **Password:** Use a Personal Access Token (not your GitHub password)
  - Go to: GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
  - Generate a new token with `repo` permissions

## 🚨 Troubleshooting

### If you get "remote origin already exists" error:
```bash
git remote remove origin
git remote add origin https://github.com/ramniks05/rccmsch-backend.git
```

### If you get "refusing to merge unrelated histories":
```bash
git pull origin main --allow-unrelated-histories
```

### If you need to check current branch:
```bash
git branch
```

### If you need to switch to main branch:
```bash
git checkout main
# or
git checkout -b main
```

## ✅ Verification

After pushing, verify on GitHub:
1. Go to: https://github.com/ramniks05/rccmsch-backend
2. Check that all files are uploaded
3. Verify the commit message appears

---

**All done!** Your code is now in the new repository.
