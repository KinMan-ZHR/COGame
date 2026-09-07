#!/bin/bash
# ==============================================================================
# COGame 自动化 Release 发布脚本
# 规范：发布 release 版本前，代码版本要打 tag，并推送到 GitHub
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${PROJECT_ROOT}"

# 1. 确保 Java 17 环境
if ! java -version 2>&1 | grep -q '17\.\([0-9]\+\)'; then
    if [ -d "/home/kinman/.local/opt/temurin-17" ]; then
        export JAVA_HOME="/home/kinman/.local/opt/temurin-17"
        export PATH="${JAVA_HOME}/bin:${PATH}"
        echo "🔧 自动激活 Java 17 环境: ${JAVA_HOME}"
    else
        echo "❌ 错误: 未检测到 Java 17 环境，请配置 JAVA_HOME 为 Java 17"
        exit 1
    fi
fi

# 2. 获取目标版本号
if [ -n "$1" ]; then
    VERSION="$1"
    VERSION="${VERSION#v}"
else
    VERSION=$(grep -m1 '<version>' pom.xml | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')
fi

TAG_NAME="v${VERSION}"
echo "=================================================="
echo "🚀 开始 COGame 版本发布流程: ${TAG_NAME}"
echo "=================================================="

# 3. 运行全量单元测试与集成测试
echo "🧪 [1/5] 执行全量单元测试与集成测试 (mvn test)..."
mvn test

# 4. 构建全平台发行包
echo "📦 [2/5] 编译并打包客户端与服务端 (mvn clean package)..."
mvn clean package -DskipTests

# 5. 同步与更新 distribution/ 发行目录
echo "📂 [3/5] 同步构建产物至 distribution/ 目录..."
CLIENT_JAR="${PROJECT_ROOT}/cogame-client/target/cogame-client-${VERSION}.jar"
CLIENT_EXE="${PROJECT_ROOT}/cogame-client/target/COGame-Client.exe"
SERVER_JAR="${PROJECT_ROOT}/cogame-server/target/cogame-server-${VERSION}.jar"
MAC_ZIP="${PROJECT_ROOT}/distribution/COGame-macOS.zip"

if [ -f "${CLIENT_JAR}" ]; then
    cp "${CLIENT_JAR}" "${PROJECT_ROOT}/distribution/COGame-Client.jar"
fi
if [ -f "${CLIENT_EXE}" ]; then
    cp "${CLIENT_EXE}" "${PROJECT_ROOT}/distribution/COGame-Client.exe"
fi
if [ -f "${SERVER_JAR}" ]; then
    cp "${SERVER_JAR}" "${PROJECT_ROOT}/distribution/COGame-Server.jar"
fi

# 更新 macOS zip 内部的 Client.jar
if [ -f "${MAC_ZIP}" ] && [ -f "${CLIENT_JAR}" ]; then
    echo "🍏 更新 macOS 客户端压缩包内部 jar..."
    python3 -c "
import zipfile, shutil
zip_path = '${MAC_ZIP}'
new_jar = '${CLIENT_JAR}'
temp_zip = zip_path + '.tmp'
with zipfile.ZipFile(zip_path, 'r') as zin, zipfile.ZipFile(temp_zip, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if item.filename == 'COGame.app/Contents/Resources/COGame-Client.jar':
            with open(new_jar, 'rb') as f:
                zout.writestr(item, f.read())
        else:
            zout.writestr(item, zin.read(item.filename))
shutil.move(temp_zip, zip_path)
"
fi

# 6. Git 标签处理
echo "🏷️  [4/5] 检查并创建 Git Tag: ${TAG_NAME}..."
if git rev-parse "${TAG_NAME}" >/dev/null 2>&1; then
    echo "⚠️  Tag ${TAG_NAME} 已存在，跳过打 tag 操作"
else
    git tag -a "${TAG_NAME}" -m "Release ${TAG_NAME}"
    echo "✅ 成功创建 Git Tag: ${TAG_NAME}"
fi

# 7. 推送至 GitHub
echo "🌐 [5/5] 推送代码分支与 Tag 到 GitHub (origin/master & ${TAG_NAME})..."
if git push origin master && git push origin "${TAG_NAME}"; then
    echo "=================================================="
    echo "🎉 Release ${TAG_NAME} 发布完成并已推送到 GitHub！"
    echo "=================================================="
else
    echo "⚠️  推送未完全成功: 请检查 GitHub 认证凭据 (SSH Key 或 Personal Access Token)。"
    echo "提示: 可运行 'git push origin master --tags' 手动重试推送。"
fi
