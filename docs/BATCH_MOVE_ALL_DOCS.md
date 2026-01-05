# Batch Move All Remaining Documentation Files to docs/

## How to Use This Script

### Option 1: Run Directly in PowerShell
```powershell
# Open PowerShell in C:\development\fincal_projects\portfoliomanager\
# Copy-paste the commands below and run them:

cd C:\development\fincal_projects\portfoliomanager

# Move all .md and .txt files to docs/ folder
Get-ChildItem -File -Include *.md,*.txt | Where-Object {$_.Name -ne "README.md"} | ForEach-Object { Move-Item -Path $_.FullName -Destination "docs\$($_.Name)" -Force }

# This single command will:
# - Find all .md and .txt files in root
# - Skip the main README.md file
# - Move them all to docs/ folder
```

### Option 2: Run This Batch File
Create a file named `move_docs.bat` in the root folder with:
```batch
@echo off
cd /d C:\development\fincal_projects\portfoliomanager\

REM Move all .md and .txt files to docs folder
for %%F in (*.md *.txt) do (
    if not "%%F"=="README.md" (
        move "%%F" "docs\%%F"
    )
)

echo All files moved successfully!
pause
```

Then double-click `move_docs.bat` to run it.

### Option 3: Manual Method
Just drag and drop all .md and .txt files (except README.md) from the root folder into docs/ using Windows Explorer.

---

## What This Does

✅ Moves ALL .md files from root to docs/
✅ Moves ALL .txt files from root to docs/
✅ Keeps the main README.md in root
✅ Keeps all other essential files in root
✅ Creates a clean project root

---

## After Running

- ✅ Project root will be clean
- ✅ All documentation in docs/
- ✅ Ready for later organization into subfolders


