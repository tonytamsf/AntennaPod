#!/bin/bash
set -euo pipefail

# Only run in remote Claude Code environments
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/root/android-sdk}"
CMDLINE_TOOLS_DIR="$ANDROID_SDK_ROOT/cmdline-tools/latest"

export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export PATH="$CMDLINE_TOOLS_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

# Persist environment variables for the session
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export ANDROID_HOME=$ANDROID_SDK_ROOT" >> "$CLAUDE_ENV_FILE"
  echo "export ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT" >> "$CLAUDE_ENV_FILE"
  echo "export PATH=$CMDLINE_TOOLS_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:\$PATH" >> "$CLAUDE_ENV_FILE"
fi

# Install Android SDK command-line tools if not already present
if [ ! -f "$CMDLINE_TOOLS_DIR/bin/sdkmanager" ]; then
  echo "==> Downloading Android command-line tools..."
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  TMP_ZIP="/tmp/cmdline-tools.zip"

  CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-12266719_latest.zip"
  if curl -fsSL "$CMDLINE_TOOLS_URL" -o "$TMP_ZIP" 2>/dev/null; then
    unzip -q "$TMP_ZIP" -d /tmp/cmdline-tools-extract
    mv /tmp/cmdline-tools-extract/cmdline-tools "$CMDLINE_TOOLS_DIR"
    rm -rf "$TMP_ZIP" /tmp/cmdline-tools-extract
    echo "==> Android command-line tools installed."
  else
    echo "WARNING: Could not download Android command-line tools from $CMDLINE_TOOLS_URL" >&2
    echo "WARNING: Android SDK setup skipped. Run 'sdkmanager' manually to install." >&2
  fi
fi

# Install required SDK components via sdkmanager if available
if [ -f "$CMDLINE_TOOLS_DIR/bin/sdkmanager" ]; then
  echo "==> Installing SDK components (platform-tools, build-tools:36.0.0, android-36)..."
  yes | "$CMDLINE_TOOLS_DIR/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" --licenses > /dev/null 2>&1 || true
  "$CMDLINE_TOOLS_DIR/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" \
    "platform-tools" \
    "build-tools;36.0.0" \
    "platforms;android-36" || {
    echo "WARNING: SDK component installation failed. Some build tasks may not work." >&2
  }
  echo "==> SDK components installed."
fi

# Warm up the Gradle wrapper (downloads from services.gradle.org)
cd "$CLAUDE_PROJECT_DIR"
echo "==> Warming up Gradle wrapper..."
./gradlew --version --quiet 2>&1 | tail -3 || true

# Pre-fetch Gradle build dependencies
echo "==> Pre-fetching Gradle dependencies..."
./gradlew dependencies --quiet 2>&1 | tail -5 || true

echo "==> Session start setup complete."
