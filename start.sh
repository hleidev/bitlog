#!/usr/bin/env bash

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    cat >&2 <<'EOF'
用法：./start.sh [目标]

  （无参数）    同时启动后端和前端，Ctrl-C 一并停止
  backend      只启动后端，http://localhost:12301
  frontend     只启动前端，开发服务器将 /api 代理到后端
  desktop      构建并启动 Markwright 桌面端

只起单个目标时等价于进对应目录执行该目录下的 start.sh。
EOF
    exit 1
}

case "${1-}" in
    backend) exec "$ROOT/bitlog-backend/start.sh" ;;
    frontend) exec "$ROOT/bitlog-frontend/start.sh" ;;
    desktop) exec "$ROOT/bitlog-frontend/start-desktop.sh" ;;
    "") ;;
    *) usage ;;
esac

# 后端放后台、前端占前台。两边日志会交织在同一个终端，需要分开看时用带参数的形式各开一个终端。
BACKEND_PID=""

cleanup() {
    if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo
        echo "==> 停止后端"
        kill "$BACKEND_PID" 2>/dev/null || true
        wait "$BACKEND_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

echo "==> 启动后端"
"$ROOT/bitlog-backend/start.sh" &
BACKEND_PID=$!

echo "==> 启动前端"
"$ROOT/bitlog-frontend/start.sh"
