#!/usr/bin/env bash

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT/bitlog-backend"
FRONTEND_DIR="$ROOT/bitlog-frontend"
DESKTOP_DIR="$FRONTEND_DIR/apps/desktop"
SERVER_JAR="$BACKEND_DIR/bitlog-server/target/bitlog-server-0.0.1-SNAPSHOT.jar"
APP_BUNDLE="$DESKTOP_DIR/src-tauri/target/release/bundle/macos/Markwright.app"

usage() {
    cat >&2 <<'EOF'
用法：./start.sh [目标]

  （无参数）    同时启动后端和前端，Ctrl-C 一并停止
  backend      只启动后端，http://localhost:12301
  frontend     只启动前端，开发服务器将 /api 代理到后端
  desktop      构建并启动 Markwright 桌面端
EOF
    exit 1
}

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "错误：未找到命令 $1" >&2
        exit 1
    fi
}

check_frontend_deps() {
    require_command npm
    if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
        echo "错误：尚未安装前端依赖。" >&2
        echo "请先在 bitlog-frontend 下运行 npm ci。" >&2
        exit 1
    fi
}

build_backend() {
    require_command mvn
    require_command java
    echo "==> 构建 BitLog Backend（跳过测试）"
    (cd "$BACKEND_DIR" && mvn -q package -DskipTests)
    if [[ ! -f "$SERVER_JAR" ]]; then
        echo "错误：构建完成，但未找到 $SERVER_JAR" >&2
        exit 1
    fi
}

# 必须 exec：并行模式下要靠它让 $! 拿到 java 自己的 PID，否则退出时杀的是外层子壳，java 会被孤儿化
# Spring Boot 按进程工作目录读取 .env，所以先切到 bitlog-backend/
run_backend() {
    echo "==> 启动 BitLog Backend：http://localhost:12301"
    cd "$BACKEND_DIR"
    exec java -jar "$SERVER_JAR"
}

# 不能 exec：并行模式下前端占前台，exec 会顶掉本脚本连同 trap，Ctrl-C 就停不掉后端了
run_frontend() {
    echo "==> 启动 BitLog Frontend"
    cd "$FRONTEND_DIR"
    npm run dev
}

run_desktop() {
    require_command npm
    echo "==> 构建 Markwright（耗时较长）"
    (cd "$DESKTOP_DIR" && npm run tauri build)
    if [[ ! -d "$APP_BUNDLE" ]]; then
        echo "错误：未找到应用包 $APP_BUNDLE" >&2
        exit 1
    fi
    echo "==> 启动 Markwright"
    open "$APP_BUNDLE"
}

case "${1-}" in
    backend)
        build_backend
        run_backend
        ;;
    frontend)
        check_frontend_deps
        run_frontend
        ;;
    desktop)
        run_desktop
        ;;
    "")
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

        # 先验前端依赖再构建后端，免得后端起来了才发现前端根本跑不了
        check_frontend_deps
        build_backend

        run_backend &
        BACKEND_PID=$!

        run_frontend
        ;;
    *)
        usage
        ;;
esac
