#!/usr/bin/env python3
import subprocess
import os
import re

os.chdir(r"D:\TAI VE\ai")

# Step 1: Remove real credentials from git history
print("🔄 Removing real credentials from git history...")
result = subprocess.run([
    'git', 'filter-branch', '-f', '--tree-filter',
    '''
python3 -c "
import re
if os.path.exists('.env.example'):
    with open('.env.example', 'r') as f:
        content = f.read()
    content = re.sub(r'779295627515-[^\\n]*', 'your-google-client-id.apps.googleusercontent.com', content)
    content = re.sub(r'GOCSPX-[^\\n]*', 'your-google-client-secret-here', content)
    with open('.env.example', 'w') as f:
        f.write(content)
"
    ''',
    '--', '--all'
], capture_output=True, text=True)

print(result.stdout)
if result.returncode != 0:
    print("Error:", result.stderr)

# Step 2: Clean reflogs
print("\n🧹 Cleaning reflogs...")
subprocess.run(['git', 'reflog', 'expire', '--expire=now', '--all'])
subprocess.run(['git', 'gc', '--prune=now', '--aggressive'])

print("\n✅ Git history cleaned!")
print("⚠️  Next steps:")
print("   1. Regenerate Google OAuth credentials at https://console.cloud.google.com/")
print("   2. Run: git push origin --force-with-lease --all")
print("   3. All team members run: git fetch && git reset --hard origin/main")

