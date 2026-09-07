# COGame v2.0 - 《端脑》隔断棋盘博弈 (C/S 联机 & 人机重构版)

本项目模拟经典脑力博弈国漫《端脑》中**6×6 格子双方互相封锁边、以连通性中断决出胜负**的对战棋盘游戏。

在 2.0 版本中，项目已完成重大架构演化：从早期的单机单体程序，重构升级为**多模块 Maven 工程**，全面支持 **单机双人对战、人机对战（AI）、跨网络 WebSocket 联机双人对战**，并在服务端实现了常驻运行与客户端一键 Web 分发。

---

## 一、 架构模块划分

```text
COGame/
├── pom.xml                 # 父 POM (统一依赖版本管理，Java 17)
├── cogame-core/            # 领域核心规则引擎 (纯 POJO，无 UI/音频依赖)
│   ├── model/              # Board(6x6/边状态)、PlayerState、GameState
│   ├── action/             # MOVE, ROTATE, CHANGE_DIR_MOVE, LOCK, RESET
│   ├── rule/               # GameEngine(状态转移)、GameEvaluator(BFS/连通分量/胜负判定)
│   └── net/                # WsMessage(跨端统一长连接数据包)
│
├── cogame-ai/              # 人机对战决策引擎
│   ├── HeuristicAi.java    # 基于领地势能、最短路径阻断与割边分析的启发式算法
│   └── AiDecision.java     # 动作序列决策载荷
│
├── cogame-server/          # 联机对战服务端 (WebSocket 协议)
│   ├── room/               # GameRoom(房间生命周期)、RoomManager(自动匹配/房间管理)
│   ├── net/                # CoGameWebSocketServer
│   └── ServerMain.java     # 服务端独立启动入口 (默认端口: 8088)
│
├── cogame-client/          # 跨平台桌面客户端 (Swing)
│   ├── controller/         # LocalController, AiController, OnlineController
│   ├── ui/                 # MainMenuFrame(模式选择), GameCanvas(对战面板), GameFrame
│   └── ClientMain.java     # 客户端启动入口 (支持全量 Fat JAR 打包)
│
├── distribution/           # 客户端 Web 分发目录 (一键下载网页与可执行 JAR)
│   ├── index.html          # 极简现代化下载与规则说明页
│   └── COGame-Client.jar   # 打包生成的免安装全量客户端 (约 63MB)
│
└── scripts/                # 服务运维管理脚本
    ├── start-server.sh     # 启动 WebSocket 联机服务端 (tmux 守护进程)
    ├── stop-server.sh      # 停止 WebSocket 联机服务端
    ├── status-server.sh    # 查询联机服务端运行状态
    ├── start-web.sh        # 启动客户端 Web 分发站点 (8080端口)
    └── stop-web.sh         # 停止客户端 Web 分发站点
```

---

## 二、 游戏模式与玩法

1. **单机双人对战 (Local 2P)**：
   - 两人共用同一台设备和键盘，交替移动与封锁边，纯离线原版体验。
2. **人机挑战模式 (vs 端脑AI)**：
   - 玩家作为先手（P1），由内置启发式算法扮演后手（P2）。
   - AI 会根据连通分量大小、双方最短路径距离及剩余边数综合决策，智能进行走位并执行致命封锁。
3. **网络双人联机 (Online PvP)**：
   - 客户端通过 WebSocket 长连接与服务端通信。
   - 输入相同房间号（如 `1001`）自动进入房间匹配，先到为 P1，后到为 P2。
   - 服务端作为权威裁判（Authoritative），实时广播双方状态变化并自动裁决胜负。

---

## 三、 游戏操作说明

| 按键 | 说明 |
| :--- | :--- |
| **WASD / 方向键** | 移动当前行动角色并改变朝向 |
| **空格键 (SPACE)** | 沿当前角色朝向直接前进一步 |
| **R 键** | 角色顺时针旋转 90 度 |
| **L 键** | **封锁当前朝向的边（关键动作）**，成功后自动切换至对手回合 |
| **P 键** | 开启 / 关闭显示双方当前的最短连通路径（蓝色高亮） |
| **+ 键 / = 键** | 重置棋局重新开始 |

---

## 四、 本地与服务端运维命令

### 1. 服务端管理
```bash
# 启动联机服务端 (端口: 8088)
./scripts/start-server.sh

# 查看服务端状态
./scripts/status-server.sh

# 停止服务端
./scripts/stop-server.sh
```

### 2. 客户端分发站点管理
```bash
# 启动客户端分发 Web 站点 (端口: 8080)
./scripts/start-web.sh

# 停止 Web 站点
./scripts/stop-web.sh
```

用户访问 `http://<服务器IP>:8080/` 即可直接查看游戏介绍并一键下载 `COGame-Client.jar`。

### 3. 项目重新编译与测试
```bash
# 执行全部单元测试与联机端到端集成测试
mvn test

# 打包所有模块与全量 Fat JAR
mvn clean package
```

---

## 五、 GitHub 同步与 Release 发布规范

本项目统一托管于 GitHub：[COGame 仓库](https://github.com/KinMan-ZHR/COGame.git)

### 1. 发布流程规范
- **代码推送**：本地开发与功能验证通过后，代码应及时推送到 GitHub 远端仓库（`origin/master`）。
- **版本打 Tag**：在正式发布任何 Release 版本前，**代码必须打上对应的语义化 Tag**（如 `v2.6.0`）。
- **Tag 推送**：Tag 必须同步推送到 GitHub，以驱动 GitHub Releases 及版本归档。

### 2. 一键自动化发布
项目内置了一键 Release 发布脚本：
```bash
# 自动执行测试、打包、同步 distribution、打 Tag 并推送到 GitHub
./scripts/release.sh 2.6.0
```

手动发布标准流程：
```bash
# 1. 运行测试
mvn test

# 2. 创建附注标签
git tag -a v2.6.0 -m "Release v2.6.0"

# 3. 推送代码与标签到 GitHub
git push origin master
git push origin v2.6.0
```

