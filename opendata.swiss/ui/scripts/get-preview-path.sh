#!/bin/bash

set -x

# Find the first changed file in opendata.swiss/ui/content/
# We use git diff with the common ancestor of the current branch and the base branch (usually master or main)
# In GH Actions, github.event.pull_request.base.sha is available, or we can use FETCH_HEAD

BASE_SHA=${1:-"origin/master"}
# Resolve directories to absolute paths to align git output (repo‑relative)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(git rev-parse --show-toplevel)"
CONTENT_DIR_ABS="$(cd "$SCRIPT_DIR/../content" && pwd)"
# Convert absolute content dir to repo‑relative path (matches git diff output)
CONTENT_DIR_REPO="${CONTENT_DIR_ABS#$REPO_ROOT/}"

# Get the list of changed Markdown files under content (paths are repo‑relative)
CHANGED_FILE=$(git -C "$REPO_ROOT" diff --name-only --diff-filter=d "$BASE_SHA" -- "$CONTENT_DIR_REPO" | grep -E '\.md$' | head -n 1)

if [ -z "$CHANGED_FILE" ]; then
    echo "/"
    exit 0
fi

# Remove the content directory prefix (repo‑relative)
REL_PATH=${CHANGED_FILE#"$CONTENT_DIR_REPO/"}

# Cases:
# 1. content/handbook/**/*.{lang}.md - the full path is determined by the path and permalink front matter variable
# 2. content/blog/**/{slug}.{lang}.md - the full path is path + /blog/{slug}
# 3. content/pages/{slug} - the path is /{slug}
# 4. content/showcases/{slug}.{lang}.md - the full path is /showase/{slug}

if [[ $REL_PATH == handbook/* ]]; then
    # Extract permalink from front matter
    PERMALINK=$(grep -E "^permalink:" "$REPO_ROOT/$CHANGED_FILE" | head -n 1 | sed 's/permalink: *//' | tr -d '\r')
    if [ -n "$PERMALINK" ]; then
        # Handbook paths seem to be /handbook/.../permalink
        # But wait, looking at the handbook structure: content/handbook/publizieren/für-erst-publizierende.de.md
        # If permalink is "erstpublizierende", does it become /handbook/publizieren/erstpublizierende?
        # Or just /handbook/erstpublizierende?
        # Let's assume it keeps the directory structure but replaces the filename with the permalink.
        DIR=$(dirname "$REL_PATH")
        echo "/$DIR/$PERMALINK"
    else
        # Fallback to filename without lang and extension
        BASENAME=$(basename "$REL_PATH")
        NAME_WITHOUT_LANG_EXT=$(echo "$BASENAME" | sed -E 's/\.[a-z]{2}\.md$//')
        DIR=$(dirname "$REL_PATH")
        echo "/$DIR/$NAME_WITHOUT_LANG_EXT"
    fi
elif [[ $REL_PATH == blog/* ]]; then
    # content/blog/**/{slug}.{lang}.md - the full path is path + /blog/{slug}
    # Wait, "the full path is path + /blog/{slug}" - but blog is already in REL_PATH
    # Example: content/blog/2023/my-post.de.md -> /blog/2023/my-post
    # Let's extract slug from front matter if it exists, otherwise use filename
    SLUG=$(grep -E "^slug:" "$REPO_ROOT/$CHANGED_FILE" | head -n 1 | sed 's/slug: *//' | tr -d '\r')
    if [ -z "$SLUG" ]; then
        BASENAME=$(basename "$REL_PATH")
        SLUG=$(echo "$BASENAME" | sed -E 's/\.[a-z]{2}\.md$//')
    fi
    # Extract date from front matter (e.g., 2025-07-31T09:53:00.000+02:00)
    DATE_STR=$(grep -E "^date:" "$REPO_ROOT/$CHANGED_FILE" | head -n 1 | sed 's/date: *//' | tr -d '\r')
    if [ -n "$DATE_STR" ]; then
        # Format date into YYYY-M (remove leading zeros from month)
        # 2025-07-31 -> 2025-7
        YEAR=$(echo "$DATE_STR" | cut -d'-' -f1)
        MONTH=$(echo "$DATE_STR" | cut -d'-' -f2 | sed 's/^0//')
        echo "/blog/$YEAR-$MONTH/$SLUG"
    else
        # Fallback to current directory structure if date is missing
        DIR=$(dirname "$REL_PATH")
        echo "/$DIR/$SLUG"
    fi
elif [[ $REL_PATH == pages/* ]]; then
    # content/pages/{slug} - the path is /{slug}
    # REL_PATH is pages/my-page.de.md
    BASENAME=$(basename "$REL_PATH")
    SLUG=$(echo "$BASENAME" | sed -E 's/\.[a-z]{2}\.md$//')
    # If it's index, it's just /
    if [ "$SLUG" == "index" ]; then
        echo "/"
    else
        echo "/$SLUG"
    fi
elif [[ $REL_PATH == showcases/* ]]; then
    # content/showcases/{slug}.{lang}.md - the full path is /showase/{slug}
    # Note the typo in the prompt: "showase" or "showcase"? I'll use "showcase" unless "showase" is really intended.
    # Looking at the existing pages: /opendata.swiss/ui/pages/showcases/index.vue
    # It should probably be /showcase/{slug} or /showcases/{slug}
    BASENAME=$(basename "$REL_PATH")
    SLUG=$(echo "$BASENAME" | sed -E 's/\.[a-z]{2}\.md$//')
    echo "/showcase/$SLUG"
else
    echo "/"
fi
