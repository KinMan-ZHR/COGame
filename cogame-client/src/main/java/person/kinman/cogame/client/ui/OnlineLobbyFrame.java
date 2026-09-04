package person.kinman.cogame.client.ui;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.core.net.RoomSummaryDto;
import person.kinman.cogame.core.net.WsMessage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 联机对战大厅：
 * 1. 彻底解决 Windows/Linux/macOS 平台原生组件发白、亮片不可读问题 (全量自绘制深色高对比组件)
 * 2. 完善房主创建房间后「等待对手加入」的交互全流程 (RoomWaitingDialog)
 * 3. 房间密码保护、大厅动态刷新与一键快速匹配
 */
public class OnlineLobbyFrame extends JFrame {
    private final String serverUrl;
    private final String playerName;

    private DefaultTableModel tableModel;
    private JTable roomTable;
    private JLabel statusLabel;
    private WebSocketClient lobbyWsClient;
    private final List<RoomSummaryDto> currentRooms = new ArrayList<>();
    private person.kinman.cogame.core.model.TurnOrderPreference lobbyTurnPreference = person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;

    public OnlineLobbyFrame(String serverUrl, String playerName) {
        this.serverUrl = serverUrl;
        this.playerName = playerName;

        this.setTitle("COGame 联机对战大厅 - 玩家: " + playerName);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(940, 620);
        this.setLocationRelativeTo(null);

        initUI();
        initLobbyConnection();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(DarkThemeHelper.COLOR_BG_DARKEST);
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // 1. 顶部状态与信息栏
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 4));
        infoPanel.setOpaque(false);

        JLabel userBadge = new JLabel("👤 玩家: " + playerName);
        userBadge.setFont(new Font("SansSerif", Font.BOLD, 14));
        userBadge.setForeground(DarkThemeHelper.COLOR_BORDER_FOCUS);

        JLabel serverBadge = new JLabel("🌐 服务器: " + serverUrl);
        serverBadge.setFont(new Font("SansSerif", Font.PLAIN, 12));
        serverBadge.setForeground(DarkThemeHelper.COLOR_TEXT_MUTED);

        statusLabel = new JLabel("● 正在连接大厅...");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(new Color(251, 191, 36));

        infoPanel.add(userBadge);
        infoPanel.add(serverBadge);
        infoPanel.add(statusLabel);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        topActions.setOpaque(false);

        DarkThemeHelper.DarkButton btnRefresh = new DarkThemeHelper.DarkButton(
                "🔄 刷新大厅",
                new Color(30, 41, 59), new Color(51, 65, 85), DarkThemeHelper.COLOR_BORDER
        );
        btnRefresh.setPreferredSize(new Dimension(110, 32));
        btnRefresh.addActionListener(e -> requestRoomList());
        topActions.add(btnRefresh);

        topBar.add(infoPanel, BorderLayout.WEST);
        topBar.add(topActions, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // 2. 中部：高对比深色房间列表表格
        String[] columns = {"房间号", "房主", "棋盘规格", "人数", "密码保护", "对战状态", "快速操作"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        roomTable = new JTable(tableModel);
        DarkThemeHelper.styleDarkTable(roomTable);

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        scrollPane.getViewport().setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        scrollPane.setBorder(BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER, 1));
        root.add(scrollPane, BorderLayout.CENTER);

        // 3. 底部操作按钮栏与分先偏好栏 (全自绘高对比度，杜绝亮片发白)
        JPanel bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
        bottomContainer.setOpaque(false);

        JPanel prefBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        prefBar.setOpaque(false);

        JLabel prefLabel = new JLabel("🎯 我的分先意愿 (匹配与加入时生效):");
        prefLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        prefLabel.setForeground(new Color(226, 232, 240));
        prefBar.add(prefLabel);

        JComboBox<String> lobbyPrefBox = new JComboBox<>(new String[]{
                "🎲 随机分先 (双方同选先手则随机掷骰 · 推荐)",
                "🔵 执先 (先手 P1 · 进击进攻)",
                "🔴 执后 (后手 P2 · 稳守反击)"
        });
        DarkThemeHelper.styleDarkComboBox(lobbyPrefBox);
        lobbyPrefBox.addActionListener(e -> {
            lobbyTurnPreference = switch (lobbyPrefBox.getSelectedIndex()) {
                case 1 -> person.kinman.cogame.core.model.TurnOrderPreference.FIRST;
                case 2 -> person.kinman.cogame.core.model.TurnOrderPreference.SECOND;
                default -> person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
            };
        });
        prefBar.add(lobbyPrefBox);
        bottomContainer.add(prefBar);
        bottomContainer.add(Box.createRigidArea(new Dimension(0, 4)));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        bottomBar.setOpaque(false);

        DarkThemeHelper.DarkButton btnQuickMatch = new DarkThemeHelper.DarkButton(
                "🎲 快速随机匹配",
                new Color(5, 150, 105), new Color(16, 185, 129), new Color(52, 211, 153)
        );
        btnQuickMatch.setPreferredSize(new Dimension(160, 40));
        btnQuickMatch.addActionListener(e -> handleRandomJoin());

        DarkThemeHelper.DarkButton btnCreateRoom = new DarkThemeHelper.DarkButton(
                "➕ 创建新房间",
                new Color(2, 132, 199), new Color(14, 165, 233), new Color(56, 189, 248)
        );
        btnCreateRoom.setPreferredSize(new Dimension(150, 40));
        btnCreateRoom.addActionListener(e -> showCreateRoomDialog());

        DarkThemeHelper.DarkButton btnJoinSelected = new DarkThemeHelper.DarkButton(
                "🔑 加入所选房间",
                new Color(109, 40, 217), new Color(124, 58, 237), new Color(192, 132, 252)
        );
        btnJoinSelected.setPreferredSize(new Dimension(150, 40));
        btnJoinSelected.addActionListener(e -> handleJoinSelectedRoom());

        DarkThemeHelper.DarkButton btnJoinById = new DarkThemeHelper.DarkButton(
                "🔍 输入房号加入",
                new Color(51, 65, 85), new Color(71, 85, 105), new Color(148, 163, 184)
        );
        btnJoinById.setPreferredSize(new Dimension(150, 40));
        btnJoinById.addActionListener(e -> showJoinByIdDialog());

        bottomBar.add(btnQuickMatch);
        bottomBar.add(btnCreateRoom);
        bottomBar.add(btnJoinSelected);
        bottomBar.add(btnJoinById);

        bottomContainer.add(bottomBar);
        root.add(bottomContainer, BorderLayout.SOUTH);
        this.add(root);
    }

    private void initLobbyConnection() {
        try {
            URI uri = new URI(serverUrl);
            lobbyWsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("● 大厅已连接 (在线)");
                        statusLabel.setForeground(new Color(34, 197, 94));
                    });
                    send(WsMessage.listRooms().toJson());
                }

                @Override
                public void onMessage(String message) {
                    try {
                        WsMessage msg = WsMessage.fromJson(message);
                        if (msg != null && WsMessage.TYPE_ROOMS_LIST.equals(msg.getType())) {
                            SwingUtilities.invokeLater(() -> updateRoomsList(msg.getRooms()));
                        }
                    } catch (Exception ignored) {}
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("● 大厅连接已断开");
                        statusLabel.setForeground(new Color(239, 68, 68));
                    });
                }

                @Override
                public void onError(Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("● 连接错误: " + (ex != null ? ex.getMessage() : "未知"));
                        statusLabel.setForeground(new Color(239, 68, 68));
                    });
                }
            };
            lobbyWsClient.connect();
        } catch (Exception e) {
            statusLabel.setText("● 连接失败: " + e.getMessage());
            statusLabel.setForeground(new Color(239, 68, 68));
        }
    }

    private void requestRoomList() {
        if (lobbyWsClient != null && lobbyWsClient.isOpen()) {
            lobbyWsClient.send(WsMessage.listRooms().toJson());
        }
    }

    private void updateRoomsList(List<RoomSummaryDto> rooms) {
        currentRooms.clear();
        tableModel.setRowCount(0);

        if (rooms != null) {
            currentRooms.addAll(rooms);
            for (RoomSummaryDto r : rooms) {
                String lockText = r.isHasPassword() ? "🔒 需密码" : "🔓 公开";
                String statusText = "WAITING".equals(r.getStatus()) ? "⏳ 等待对手 (1/2)" : "⚔️ 对战中 (2/2)";
                String actionText = "WAITING".equals(r.getStatus()) ? (r.isHasPassword() ? "密码加入" : "直接加入") : "对局中";

                tableModel.addRow(new Object[]{
                        r.getRoomId(),
                        r.getHostName(),
                        r.getBoardSize() + " × " + r.getBoardSize(),
                        r.getPlayerCount() + " / 2",
                        lockText,
                        statusText,
                        actionText
                });
            }
        }
    }

    /**
     * 创建房间弹窗：全自绘深色，杜绝任何白底发白
     */
    private void showCreateRoomDialog() {
        JDialog dialog = new JDialog(this, "创建对战房间", true);
        dialog.setSize(440, 420);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER_FOCUS, 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));

        JLabel l1 = createDarkLabel("房间编号 (可自定义或保持随机):");
        JTextField roomIdField = new JTextField(String.valueOf((int) (Math.random() * 9000 + 1000)));
        DarkThemeHelper.styleDarkTextField(roomIdField);

        JLabel l2 = createDarkLabel("棋盘规格 (Board Size):");
        JComboBox<String> sizeBox = new JComboBox<>(new String[]{
                "🐣 经典小盘 (6 × 6 - 36格 · 纯净对决)",
                "⚔️ 战术中盘 (9 × 9 - 81格 · 要塞废墟)",
                "👑 战略大盘 (12 × 12 - 144格 · 迷宫战场)"
        });
        DarkThemeHelper.styleDarkComboBox(sizeBox);

        JLabel l3 = createDarkLabel("房间密码 (可选，留空表示公开无密码):");
        JPasswordField passwordField = new JPasswordField();
        DarkThemeHelper.styleDarkPasswordField(passwordField);

        JLabel l4 = createDarkLabel("分先意愿 (谁先手):");
        JComboBox<String> turnBox = new JComboBox<>(new String[]{
                "🎲 随机分先 (双方同选先手则随机掷骰 · 推荐)",
                "🔵 执先 (先手 P1 · 进击进攻)",
                "🔴 执后 (后手 P2 · 稳守反击)"
        });
        DarkThemeHelper.styleDarkComboBox(turnBox);
        if (lobbyTurnPreference == person.kinman.cogame.core.model.TurnOrderPreference.FIRST) turnBox.setSelectedIndex(1);
        else if (lobbyTurnPreference == person.kinman.cogame.core.model.TurnOrderPreference.SECOND) turnBox.setSelectedIndex(2);
        else turnBox.setSelectedIndex(0);

        panel.add(l1);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(roomIdField);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(l2);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(sizeBox);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(l3);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(passwordField);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(l4);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(turnBox);
        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        DarkThemeHelper.DarkButton btnCancel = new DarkThemeHelper.DarkButton("取消",
                new Color(51, 65, 85), new Color(71, 85, 105), DarkThemeHelper.COLOR_BORDER);
        btnCancel.setPreferredSize(new Dimension(85, 34));
        btnCancel.addActionListener(e -> dialog.dispose());

        DarkThemeHelper.DarkButton btnConfirm = new DarkThemeHelper.DarkButton("立即创建",
                new Color(2, 132, 199), new Color(14, 165, 233), DarkThemeHelper.COLOR_BORDER_FOCUS);
        btnConfirm.setPreferredSize(new Dimension(100, 34));
        btnConfirm.addActionListener(e -> {
            String roomId = roomIdField.getText().trim();
            if (roomId.isEmpty()) {
                showDarkAlert("提示", "房间号不能为空！");
                return;
            }
            int size = switch (sizeBox.getSelectedIndex()) {
                case 1 -> 9;
                case 2 -> 12;
                default -> 6;
            };
            String pwd = new String(passwordField.getPassword()).trim();
            person.kinman.cogame.core.model.TurnOrderPreference pref = switch (turnBox.getSelectedIndex()) {
                case 1 -> person.kinman.cogame.core.model.TurnOrderPreference.FIRST;
                case 2 -> person.kinman.cogame.core.model.TurnOrderPreference.SECOND;
                default -> person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
            };
            dialog.dispose();
            // 房主建房：isHost = true
            enterGameRoom(roomId, size, pwd.isEmpty() ? null : pwd, true, pref);
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);
        panel.add(btnPanel);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void handleJoinSelectedRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow < 0) {
            showJoinByIdDialog();
            return;
        }

        RoomSummaryDto room = currentRooms.get(selectedRow);
        if ("PLAYING".equals(room.getStatus()) || room.getPlayerCount() >= 2) {
            showDarkAlert("房间已满", "该房间正在激烈对战中！\n提示：实时观战系统将于 v2.5 版本正式开放！");
            return;
        }

        if (room.isHasPassword()) {
            showPasswordInputDialog(room.getRoomId(), room.getBoardSize());
        } else {
            enterGameRoom(room.getRoomId(), room.getBoardSize(), null, false);
        }
    }

    private void showPasswordInputDialog(String roomId, int boardSize) {
        JDialog dialog = new JDialog(this, "输入房间密码", true);
        dialog.setSize(380, 220);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER_FOCUS, 1),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        JLabel l1 = createDarkLabel("该房间已上锁，请输入访问密码：");
        JPasswordField pwdField = new JPasswordField();
        DarkThemeHelper.styleDarkPasswordField(pwdField);

        panel.add(l1);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(pwdField);
        panel.add(Box.createRigidArea(new Dimension(0, 18)));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        DarkThemeHelper.DarkButton btnCancel = new DarkThemeHelper.DarkButton("取消",
                new Color(51, 65, 85), new Color(71, 85, 105), DarkThemeHelper.COLOR_BORDER);
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.addActionListener(e -> dialog.dispose());

        DarkThemeHelper.DarkButton btnJoin = new DarkThemeHelper.DarkButton("确认加入",
                new Color(109, 40, 217), new Color(124, 58, 237), DarkThemeHelper.COLOR_BORDER_FOCUS);
        btnJoin.setPreferredSize(new Dimension(95, 32));
        btnJoin.addActionListener(e -> {
            String pwd = new String(pwdField.getPassword()).trim();
            dialog.dispose();
            enterGameRoom(roomId, boardSize, pwd.isEmpty() ? null : pwd, false);
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnJoin);
        panel.add(btnPanel);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void showJoinByIdDialog() {
        JDialog dialog = new JDialog(this, "精确输入房号加入", true);
        dialog.setSize(400, 270);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER_FOCUS, 1),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        JLabel l1 = createDarkLabel("请输入房间号:");
        JTextField roomIdField = new JTextField("1001");
        DarkThemeHelper.styleDarkTextField(roomIdField);

        JLabel l2 = createDarkLabel("房间密码 (若房间无密码请留空):");
        JPasswordField pwdField = new JPasswordField();
        DarkThemeHelper.styleDarkPasswordField(pwdField);

        panel.add(l1);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(roomIdField);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(l2);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(pwdField);
        panel.add(Box.createRigidArea(new Dimension(0, 18)));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        DarkThemeHelper.DarkButton btnCancel = new DarkThemeHelper.DarkButton("取消",
                new Color(51, 65, 85), new Color(71, 85, 105), DarkThemeHelper.COLOR_BORDER);
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.addActionListener(e -> dialog.dispose());

        DarkThemeHelper.DarkButton btnJoin = new DarkThemeHelper.DarkButton("加入对局",
                new Color(109, 40, 217), new Color(124, 58, 237), DarkThemeHelper.COLOR_BORDER_FOCUS);
        btnJoin.setPreferredSize(new Dimension(95, 32));
        btnJoin.addActionListener(e -> {
            String roomId = roomIdField.getText().trim();
            if (roomId.isEmpty()) return;
            String pwd = new String(pwdField.getPassword()).trim();
            dialog.dispose();
            enterGameRoom(roomId, 6, pwd.isEmpty() ? null : pwd, false);
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnJoin);
        panel.add(btnPanel);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void handleRandomJoin() {
        for (RoomSummaryDto r : currentRooms) {
            if ("WAITING".equals(r.getStatus()) && !r.isHasPassword()) {
                enterGameRoom(r.getRoomId(), r.getBoardSize(), null, false);
                return;
            }
        }
        showCreateRoomDialog();
    }

    private void enterGameRoom(String roomId, int boardSize, String password, boolean isHost) {
        enterGameRoom(roomId, boardSize, password, isHost, lobbyTurnPreference);
    }

    private void enterGameRoom(String roomId, int boardSize, String password, boolean isHost, person.kinman.cogame.core.model.TurnOrderPreference turnPref) {
        if (lobbyWsClient != null && lobbyWsClient.isOpen()) {
            lobbyWsClient.close();
        }
        this.dispose();

        OnlineController controller = new OnlineController(serverUrl, roomId, playerName, boardSize, password, turnPref);
        GameFrame gameFrame = new GameFrame(controller);
        gameFrame.display();

        // 如果是房主创建房间，立即弹出「等待对手加入」专属等待室
        if (isHost) {
            RoomWaitingDialog waitingDialog = new RoomWaitingDialog(gameFrame, controller, serverUrl, playerName, turnPref);
            waitingDialog.setVisible(true);
        }
    }

    private JLabel createDarkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(DarkThemeHelper.COLOR_TEXT_PRIMARY);
        return l;
    }

    private void showDarkAlert(String title, String message) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(380, 180);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER_FOCUS, 1),
                BorderFactory.createEmptyBorder(20, 20, 16, 20)
        ));

        JLabel msgLabel = new JLabel("<html>" + message.replace("\n", "<br/>") + "</html>");
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        msgLabel.setForeground(DarkThemeHelper.COLOR_TEXT_PRIMARY);
        panel.add(msgLabel, BorderLayout.CENTER);

        DarkThemeHelper.DarkButton btnOk = new DarkThemeHelper.DarkButton("知道了",
                new Color(2, 132, 199), new Color(14, 165, 233), DarkThemeHelper.COLOR_BORDER_FOCUS);
        btnOk.setPreferredSize(new Dimension(90, 32));
        btnOk.addActionListener(e -> dialog.dispose());

        JPanel bPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bPanel.setOpaque(false);
        bPanel.add(btnOk);
        panel.add(bPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }
}
