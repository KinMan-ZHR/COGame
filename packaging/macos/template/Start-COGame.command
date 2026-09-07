#!/bin/bash
cd "$(dirname "$0")"

echo "================================================="
echo "   COGame - 《端脑》隔断棋盘博弈 (macOS 启动引导器)"
echo "================================================="
echo ""

# 1. 自动解除 macOS Gatekeeper 隔离限制 (针对网页下载的未签名 app)
echo "🛡️  正在检测并解除 macOS 隔离与安全限制 (Gatekeeper)..."
if [ -d "COGame.app" ]; then
    xattr -cr "COGame.app" 2>/dev/null || true
    chmod -R +x "COGame.app/Contents/MacOS" 2>/dev/null || true
    echo "✅ 已完成安全隔离解除 (xattr -cr)"
fi

# 2. 检查 Java 环境
JAVA_CMD=""

check_java() {
    local j="$1"
    [ -x "$j" ] || return 1
    local v
    v=$("$j" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    local m
    m=$(echo "$v" | awk -F '.' '{print ($1 == 1 ? $2 : $1)}')
    [ -n "$m" ] && [ "$m" -ge 17 ] 2>/dev/null
}

if [ -n "${JAVA_HOME:-}" ] && check_java "$JAVA_HOME/bin/java"; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif [ -x "/usr/libexec/java_home" ]; then
    JH=$(/usr/libexec/java_home -v 17+ 2>/dev/null || true)
    if [ -n "$JH" ] && check_java "$JH/bin/java"; then
        JAVA_CMD="$JH/bin/java"
    fi
fi

if [ -z "$JAVA_CMD" ]; then
    for p in \
        "/opt/homebrew/opt/openjdk@17/bin/java" \
        "/opt/homebrew/opt/openjdk@21/bin/java" \
        "/opt/homebrew/opt/openjdk/bin/java" \
        "/usr/local/opt/openjdk@17/bin/java" \
        "/usr/local/opt/openjdk@21/bin/java" \
        "/Library/Java/JavaVirtualMachines"/*/Contents/Home/bin/java \
        "$HOME/.sdkman/candidates/java/current/bin/java"; do
        if check_java "$p"; then
            JAVA_CMD="$p"
            break
        fi
    done
fi

if [ -z "$JAVA_CMD" ]; then
    SYS_JAVA=$(command -v java 2>/dev/null || true)
    if [ -n "$SYS_JAVA" ] && check_java "$SYS_JAVA"; then
        JAVA_CMD="$SYS_JAVA"
    fi
fi

if [ -z "$JAVA_CMD" ]; then
    echo "❌ 未检测到 Java 17 或更高版本！"
    echo "   请通过 Homebrew 安装：brew install openjdk@17"
    echo "   或前往 Adoptium 官网下载：https://adoptium.net/temurin/releases/?version=17"
    echo ""
    read -p "按回车键前往官网下载..."
    open "https://adoptium.net/temurin/releases/?version=17"
    exit 1
fi

echo "☕ 检测到可用 Java: $JAVA_CMD"
echo "🎮 正在启动 COGame..."
echo ""

JAR_PATH="COGame.app/Contents/Resources/COGame-Client.jar"
if [ ! -f "$JAR_PATH" ] && [ -f "COGame-Client.jar" ]; then
    JAR_PATH="COGame-Client.jar"
fi

exec "$JAVA_CMD" \
    -Xdock:name="COGame" \
    -Dapple.awt.application.name="COGame" \
    -Dapple.laf.useScreenMenuBar=true \
    -Dfile.encoding=UTF-8 \
    -jar "$JAR_PATH" "$@"
