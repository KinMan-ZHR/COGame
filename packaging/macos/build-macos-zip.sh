#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TEMPLATE_DIR="${SCRIPT_DIR}/template"
DIST_DIR="${PROJECT_ROOT}/distribution"
TARGET_CLIENT_JAR="${1:-${PROJECT_ROOT}/cogame-client/target/COGame-Client.jar}"

if [ ! -f "${TARGET_CLIENT_JAR}" ]; then
    # Try finding any shaded client jar in target
    TARGET_CLIENT_JAR=$(find "${PROJECT_ROOT}/cogame-client/target" -maxdepth 1 -name "cogame-client-*.jar" ! -name "original-*" | head -n 1)
fi

if [ -z "${TARGET_CLIENT_JAR}" ] || [ ! -f "${TARGET_CLIENT_JAR}" ]; then
    echo "❌ 找不到客户端 jar 文件，无法打包 macOS zip" >&2
    exit 1
fi

BUILD_TMP="$(mktemp -d)"
trap 'rm -rf "${BUILD_TMP}"' EXIT

echo "🍏 正在组装 macOS 应用包..."
cp -R "${TEMPLATE_DIR}"/* "${BUILD_TMP}/"
mkdir -p "${BUILD_TMP}/COGame.app/Contents/Resources"
cp "${TARGET_CLIENT_JAR}" "${BUILD_TMP}/COGame.app/Contents/Resources/COGame-Client.jar"

chmod +x "${BUILD_TMP}/COGame.app/Contents/MacOS/COGame" "${BUILD_TMP}/Start-COGame.command"

mkdir -p "${DIST_DIR}"
OUTPUT_ZIP="${DIST_DIR}/COGame-macOS.zip"
rm -f "${OUTPUT_ZIP}"

python3 -c "
import zipfile, os, sys

build_tmp = sys.argv[1]
output_zip = sys.argv[2]

with zipfile.ZipFile(output_zip, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for root, dirs, files in os.walk(build_tmp):
        for f in files:
            full_path = os.path.join(root, f)
            rel_path = os.path.relpath(full_path, build_tmp)
            zinfo = zipfile.ZipInfo.from_file(full_path, rel_path)
            # 保留执行权限
            if os.access(full_path, os.X_OK):
                zinfo.external_attr = 0o755 << 16
            else:
                zinfo.external_attr = 0o644 << 16
            with open(full_path, 'rb') as fp:
                zout.writestr(zinfo, fp.read())
print(f'✅ 成功构建 macOS 压缩包: {output_zip}')
" "${BUILD_TMP}" "${OUTPUT_ZIP}"
