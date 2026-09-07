# COGame 开发与发布规范 (Agent Guidelines)

本项目（COGame）遵循严格的代码同步与发布规范，所有 AI 助手及协同开发者必须遵守以下规则：

## 1. 代码推送规则 (Push to GitHub)
- 本地所有经过测试验证的提交，应当同步推送到 GitHub 远端仓库：
  - Remote: `origin` (`https://github.com/KinMan-ZHR/COGame.git` 或 `git@github.com:KinMan-ZHR/COGame.git`)
  - 主分支: `master`
- 完成重要功能开发或修复后，不可仅留在本地，需及时推送到 GitHub。

## 2. 版本发布与打 Tag 规范 (Release & Tagging)
- **发布任何 Release 版本前，必须先在 Git 中为该代码版本打上语义化标签（Tag）**。
- Tag 格式标准：`v<MAJOR>.<MINOR>.<PATCH>`（例如：`v2.6.0`）。
- 打 Tag 时必须使用带附注的注解标签（Annotated Tag）：
  ```bash
  git tag -a v2.6.0 -m "Release v2.6.0"
  ```
- **发布流程清单**：
  1. 确认所有修改已编译并通过全量测试（`JAVA_HOME=~/.local/opt/temurin-17 mvn test`）。
  2. 确认 `pom.xml` 及 `distribution/index.html` 中的版本号统一一致。
  3. 提交代码更改至本地 Git。
  4. 为当前提交打上对应版本的 Git Tag。
  5. 将分支代码及 Tag 一同推送到 GitHub：
     ```bash
     git push origin master
     git push origin <TAG_NAME>
     # 或一次性推送分支和标签：
     git push origin master --tags
     ```
  6. 执行打包构建：`mvn clean package`，确保 `distribution/` 下的各端安装包已同步更新。

## 3. 自动化发布工具
项目提供了一键自动化发布脚本：
```bash
./scripts/release.sh [version]
```
该脚本会自动校验 Java 17、执行测试、打包全平台发行物、同步 `distribution/` 目录、打 Git Tag 并推送到 GitHub。
