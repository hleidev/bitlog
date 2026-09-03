#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v npm >/dev/null 2>&1; then
    echo "错误：未找到命令 npm" >&2
    exit 1
fi

if [[ ! -d "$PROJECT_ROOT/node_modules" ]]; then
    echo "错误：尚未安装前端依赖。" >&2
    echo "请先在 bitlog-frontend 下运行 npm ci。" >&2
    exit 1
fi

echo "==> 启动 BitLog Frontend"
cd "$PROJECT_ROOT"
exec npm run dev
