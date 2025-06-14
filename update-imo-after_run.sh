#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo "Removing existing directory..."
rm -rf app/src/main/java/imo/after_run

echo "Cloning source repository..."
git clone --depth 1 https://github.com/IMOitself/AfterRun.git temp_AfterRun

echo "Adding temp_AfterRun dir as safe directory..."
FULL_PATH_TO_TEMP_DIR=$(readlink -f "temp_AfterRun")
git config --global --add safe.directory "$FULL_PATH_TO_TEMP_DIR"

echo "Getting latest commit hash..."
LATEST_COMMIT=$(git -C temp_AfterRun rev-parse HEAD)

echo "Copying directory..."
mkdir -p app/src/main/java/imo/after_run
cp -r temp_AfterRun/app/src/main/java/imo/after_run/. app/src/main/java/imo/after_run/

echo "Removing MainActivity.java..."
# This step might be redundant if MainActivity.java is not in the source,
# but it is kept here to match the original script's logic.
if [ -f "app/src/main/java/imo/after_run/MainActivity.java" ]; then
    rm app/src/main/java/imo/after_run/MainActivity.java
fi

echo "Cleaning up temporary clone..."
rm -rf temp_AfterRun

echo "Staging changes..."
git add .

# Check if there are any changes to commit
if git diff-index --quiet HEAD --; then
    echo "No changes to commit. Exiting."
    exit 0
fi

echo "Committing changes..."
git commit -m "Update IMOitself/AfterRun java files" -m "last commit: https://github.com/IMOitself/AfterRun/commits/$LATEST_COMMIT"

echo "Update and commit complete."

