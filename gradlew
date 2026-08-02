#!/bin/sh
set -eu

GRADLE_VERSION=9.5.0
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DIST_DIR="$APP_HOME/.gradle-dist"
GRADLE_HOME="$DIST_DIR/gradle-$GRADLE_VERSION"
ZIP_FILE="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    mkdir -p "$DIST_DIR"
    if [ ! -f "$ZIP_FILE" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl -fL "$URL" -o "$ZIP_FILE"
        elif command -v wget >/dev/null 2>&1; then
            wget "$URL" -O "$ZIP_FILE"
        else
            echo "curl or wget is required to download Gradle" >&2
            exit 1
        fi
    fi
    command -v unzip >/dev/null 2>&1 || {
        echo "unzip is required to extract Gradle" >&2
        exit 1
    }
    unzip -q -o "$ZIP_FILE" -d "$DIST_DIR"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
