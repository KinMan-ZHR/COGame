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
    from pathlib import Path
    build_path = Path(build_tmp)
    # 1. 递归写入目录元数据（对 macOS Package 至关重要）
    for root, dirs, files in os.walk(build_tmp):
        for d in dirs:
            dir_full = Path(root) / d
            dir_rel = dir_full.relative_to(build_path).as_posix() + '/'
            dinfo = zipfile.ZipInfo(dir_rel)
            dinfo.create_system = 3  # Unix
            dinfo.external_attr = (0o040755 << 16) | 0o20  # S_IFDIR | 0755 + MS-DOS dir bit
            zout.writestr(dinfo, b'')

        # 2. 写入文件元数据，严格赋予 S_IFREG 标志
        for f in files:
            full_path = Path(root) / f
            rel_path = full_path.relative_to(build_path).as_posix()
            zinfo = zipfile.ZipInfo.from_file(str(full_path), rel_path)
            zinfo.create_system = 3  # Unix
            if os.access(str(full_path), os.X_OK):
                zinfo.external_attr = 0o100755 << 16  # S_IFREG | 0755
            else:
                zinfo.external_attr = 0o100644 << 16  # S_IFREG | 0644
            with open(str(full_path), 'rb') as fp:
                zout.writestr(zinfo, fp.read())
print(f'✅ 成功构建标准 macOS 压缩包: {output_zip}')
" "${BUILD_TMP}" "${OUTPUT_ZIP}"
