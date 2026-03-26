#!/bin/bash

# Find the first changed file in opendata.swiss/ui/content/
# Prefer a base ref name (e.g. origin/main) over a raw SHA, and use merge-base via triple-dot

BASE_INPUT=${1:-"origin/main"}
# Resolve directories to absolute paths to align git output (repo‑relative)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(git rev-parse --show-toplevel)"
CONTENT_DIR_ABS="$(cd "$SCRIPT_DIR/../content" && pwd)"
# Convert absolute content dir to repo‑relative path (matches git diff output)
CONTENT_DIR_REPO="${CONTENT_DIR_ABS#$REPO_ROOT/}"

# Try to ensure the base is available locally (works when BASE_INPUT is a ref); ignore failures
(git -C "$REPO_ROOT" fetch --no-tags --prune --depth=50 origin "+${BASE_INPUT}:${BASE_INPUT}" || true) >/dev/null 2>&1

BASE_RANGE="${BASE_INPUT}...HEAD"

# Primary attempt: pathspec restricted to content dir, with non-ASCII paths unescaped
CHANGED_FILES=$(git -C "$REPO_ROOT" -c core.quotepath=false diff --name-only --diff-filter=d "$BASE_RANGE" -- "$CONTENT_DIR_REPO" | grep -E '\.md$')

# Fallback 1: diff all and grep the prefix (handles odd pathspec issues)
if [ -z "$CHANGED_FILES" ]; then
  CHANGED_FILES=$(git -C "$REPO_ROOT" -c core.quotepath=false diff --name-only --diff-filter=d "$BASE_RANGE" | grep -E "^${CONTENT_DIR_REPO}/.*\\.md$")
fi

# Fallback 2: use two-dot range
if [ -z "$CHANGED_FILES" ]; then
  CHANGED_FILES=$(git -C "$REPO_ROOT" -c core.quotepath=false diff --name-only --diff-filter=d "$BASE_INPUT" -- "$CONTENT_DIR_REPO" | grep -E '\.md$')
fi

# Fallback 3: two-dot range + grep
if [ -z "$CHANGED_FILES" ]; then
  CHANGED_FILES=$(git -C "$REPO_ROOT" -c core.quotepath=false diff --name-only --diff-filter=d "$BASE_INPUT" | grep -E "^${CONTENT_DIR_REPO}/.*\\.md$")
fi

if [ -z "$CHANGED_FILES" ]; then
    echo "/"
    exit 0
fi

# Function to URL-encode a string using perl
encode_segment() {
    echo -n "$1" | perl -MURI::Escape -ne 'print uri_escape($_)'
}

# Function to get path for a given file
get_handbook_path() {
    local file=$1
    local slug=$(grep -E "^slug:" "$file" | head -n 1 | sed 's/slug: *//' | tr -d '\r' | xargs)
    if [ -z "$slug" ]; then
        # Fallback to filename if slug is missing
        slug=$(basename "$file" | sed -E 's/\.[a-z]{2}\.md$//')
    fi
    local parent=$(grep -E "^parent:" "$file" | head -n 1 | sed 's/parent: *//' | tr -d '\r' | xargs)
    local lang=$(basename "$file" | sed -E 's/.*\.([a-z]{2})\.md$/\1/')

    if [ -n "$parent" ]; then
        # Find the parent file
        # parent field matches the path relative to content/handbook/ without the lang.md extension
        local parent_file="$REPO_ROOT/$CONTENT_DIR_REPO/handbook/${parent}.${lang}.md"

        # Robust lookup: if not found, search in handbook directory
        if [ ! -f "$parent_file" ]; then
            parent_file=$(find "$REPO_ROOT/$CONTENT_DIR_REPO/handbook" -name "${parent##*/}.${lang}.md" | grep "/${parent}.${lang}.md$" | head -n 1)
        fi

        # Second fallback: try default language (de) if translation is missing
        if [ ! -f "$parent_file" ] && [ "$lang" != "de" ]; then
            parent_file="$REPO_ROOT/$CONTENT_DIR_REPO/handbook/${parent}.de.md"
            if [ ! -f "$parent_file" ]; then
                parent_file=$(find "$REPO_ROOT/$CONTENT_DIR_REPO/handbook" -name "${parent##*/}.de.md" | grep "/${parent}.de.md$" | head -n 1)
            fi
        fi

        if [ -n "$parent_file" ] && [ -f "$parent_file" ]; then
            echo "$(get_handbook_path "$parent_file")/$(encode_segment "$slug")"
        else
            # If parent specified but not found, still return it as a flat path under /handbook/
            # but this shouldn't happen with correct metadata.
            echo "/handbook/$(encode_segment "$slug")"
        fi
    else
        echo "/handbook/$(encode_segment "$slug")"
    fi
}

for CHANGED_FILE in $CHANGED_FILES; do
    # Remove the content directory prefix (repo‑relative)
    REL_PATH=${CHANGED_FILE#"$CONTENT_DIR_REPO/"}

    # Cases:
    # 1. content/handbook/**/*.{lang}.md - the full path is determined by the slug and parent front matter variables
    # 2. content/blog/**/{slug}.{lang}.md - the full path is path + /blog/{slug}
    # 3. content/pages/{slug} - the path is /{slug}
    # 4. content/showcases/{slug}.{lang}.md - the full path is /showcase/{slug}

    case "$REL_PATH" in
        handbook/*)
            get_handbook_path "$REPO_ROOT/$CHANGED_FILE"
            ;;
        blog/*)
            # content/blog/**/{slug}.{lang}.md - the full path is path + /blog/{slug}
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
                YEAR=$(echo "$DATE_STR" | cut -d'-' -f1)
                MONTH=$(echo "$DATE_STR" | cut -d'-' -f2 | sed 's/^0//')
                ENCODED_SLUG=$(encode_segment "$SLUG")
                echo "/blog/$YEAR-$MONTH/$ENCODED_SLUG"
            else
                # Fallback to current directory structure if date is missing
                DIR=$(dirname "$REL_PATH")
                ENCODED_SLUG=$(encode_segment "$SLUG")
                echo "/$DIR/$ENCODED_SLUG"
            fi
            ;;
        pages/*)
            # content/pages/{slug} - the path is /{slug}
            BASENAME=$(basename "$REL_PATH")
            SLUG=$(echo "$BASENAME" | sed -E 's/\.[a-z]{2}\.md$//')
            if [ "$SLUG" == "index" ]; then
                echo "/"
            else
                ENCODED_SLUG=$(encode_segment "$SLUG")
                echo "/$ENCODED_SLUG"
            fi
            ;;
        showcases/*)
            # content/showcases/{slug}.{lang}.md - the full path is /showcase/{slug}
            BASENAME=$(basename "$REL_PATH")
            SLUG=$(echo "$BASENAME" | sed -E 's/\.[a-z]{2}\.md$//')
            ENCODED_SLUG=$(encode_segment "$SLUG")
            echo "/showcase/$ENCODED_SLUG"
            ;;
        *)
            echo "/"
            ;;
    esac
done | sort -u
