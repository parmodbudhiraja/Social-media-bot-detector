import sys
import re

msg = sys.stdin.read()

# remove prefixes like "feat(ml): " or "fix: "
# we'll use a regex to match the conventional commit format at the start
new_msg = re.sub(r'^(feat|fix|docs|style|refactor|test|chore|perf|build|ci|revert)(\([^\)]+\))?:\s*', '', msg, flags=re.IGNORECASE)

# capitalize the first letter
if len(new_msg) > 0:
    new_msg = new_msg[0].upper() + new_msg[1:]

sys.stdout.write(new_msg)
