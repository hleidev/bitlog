#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DESKTOP_DIR="$SCRIPT_DIR/apps/desktop"
APP_BUNDLE="$DESKTOP_DIR/src-tauri/target/release/bundle/macos/Markwright.app"

# ── Build ────────────────────────────────────────────────────────────────────
echo "==> Building Markwright (this takes a while)..."
cd "$DESKTOP_DIR"
npm run tauri build

# ── Launch ───────────────────────────────────────────────────────────────────
if [ ! -d "$APP_BUNDLE" ]; then
  echo "ERROR: App bundle not found at $APP_BUNDLE" >&2
  exit 1
fi

echo "==> Launching Markwright..."
open "$APP_BUNDLE"
