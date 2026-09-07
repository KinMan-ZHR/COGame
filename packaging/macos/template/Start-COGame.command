#!/bin/bash
cd "$(dirname "$0")"

echo "================================================="
echo "   COGame - 《端脑》隔断棋盘博弈 (macOS 启动器)"
echo "================================================="

if [ -d "COGame.app" ]; then
    echo "正在启动 COGame.app ..."
    open "COGame.app"
    exit 0
fi

if [ -f "COGame-Client.jar" ]; then
    java -Dfile.encoding=UTF-8 -jar "COGame-Client.jar"
fi
