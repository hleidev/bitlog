#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_JAR="$PROJECT_ROOT/bitlog-server/target/bitlog-server-0.0.1-SNAPSHOT.jar"

require_command() {
    local command_name="$1"
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "错误：未找到命令 $command_name" >&2
        exit 1
    fi
}

require_command mvn
require_command java

echo "==> 构建 BitLog Backend（跳过测试）"
(
    cd "$PROJECT_ROOT"
    mvn -q package -DskipTests
)

if [[ ! -f "$SERVER_JAR" ]]; then
    echo "错误：构建完成，但未找到 $SERVER_JAR" >&2
    exit 1
fi

echo "==> 启动 BitLog Backend：http://localhost:12301"
cd "$PROJECT_ROOT"
exec java -jar "$SERVER_JAR"
