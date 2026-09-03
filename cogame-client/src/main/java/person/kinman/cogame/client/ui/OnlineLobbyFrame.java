package person.kinman.cogame.client.ui;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.core.net.RoomSummaryDto;
import person.kinman.cogame.core.net.WsMessage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 联机对战大厅：房间列表浏览、密码保护、创建房间与快速随机匹配
 */
public class OnlineLobbyFrame extends JFrame {
    private final String serverUrl;
    private final String playerName;

    private DefaultTableModel tableModel;
    private JTable roomTable;
    private JLabel statusLabel;
    private WebSocketClient lobbyWsClient;
    private final List<RoomSummaryDto> currentRooms = new ArrayList<>();

    public OnlineLobbyFrame(String serverUrl, String playerName) {
        this.serverUrl = serverUrl;
        this.playerName = playerName;

        this.setTitle("COGame 联机对战大厅 - 玩家: " + playerName);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(920, 600);
        this.setLocationRelativeTo(null);

        initUI();
        initLobbyConnection();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(new Color(15, 23, 42)); // 深夜蓝底色
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // 1. 顶部状态与信息栏
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 4));
        infoPanel.setOpaque(false);

        JLabel userBadge = new JLabel("👤 玩家: " + playerName);
        userBadge.setFont(new Font("SansSerif", Font.BOLD, 14));
        userBadge.setForeground(new Color(56, 189, 248)); // 电光青

        JLabel serverBadge = new JLabel("🌐 服务器: " + serverUrl);
        serverBadge.setFont(new Font("SansSerif", Font.PLAIN, 12));
        serverBadge.setForeground(new Color(148, 163, 184));

        statusLabel = new JLabel("● 正在连接大厅...");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(new Color(251, 191, 36));

        infoPanel.add(userBadge);
        infoPanel.add(serverBadge);
        infoPanel.add(statusLabel);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        topActions.setOpaque(false);

        JButton btnRefresh = createStyledButton("🔄 刷新大厅", new Color(30, 41, 59), new Color(51, 65, 85), Color.WHITE);
        btnRefresh.addActionListener(e -> requestRoomList());
        topActions.add(btnRefresh);

        topBar.add(infoPanel, BorderLayout.WEST);
        topBar.add(topActions, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // 2. 中部：房间列表表格
        String[] columns = {"房间号", "房主", "棋盘规格", "人数", "密码保护", "对战状态", "快速操作"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 只读
            }
        };

        roomTable = new JTable(tableModel);
        roomTable.setRowHeight(36);
        roomTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        roomTable.setBackground(new Color(30, 41, 59));
        roomTable.setForeground(new Color(241, 245, 249));
        roomTable.setSelectionBackground(new Color(2, 132, 199));
        roomTable.setSelectionForeground(Color.WHITE);
        roomTable.setGridColor(new Color(51, 65, 85));
        roomTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        roomTable.getTableHeader().setBackground(new Color(21, 32, 54));
        roomTable.getTableHeader().setForeground(new Color(226, 232, 240));
        roomTable.getTableHeader().setReorderingAllowed(false);

        // 居中渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < roomTable.getColumnCount(); i++) {
            roomTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.getViewport().setBackground(new Color(30, 41, 59));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1));
        root.add(scrollPane, BorderLayout.CENTER);

        // 3. 底部操作按钮栏
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
        bottomBar.setOpaque(false);

        JButton btnQuickMatch = createStyledButton("🎲 快速随机匹配", new Color(16, 185, 129), new Color(5, 150, 105), Color.WHITE);
        btnQuickMatch.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnQuickMatch.addActionListener(e -> handleRandomJoin());

        JButton btnCreateRoom = createStyledButton("➕ 创建新房间", new Color(2, 132, 199), new Color(3, 105, 161), Color.WHITE);
        btnCreateRoom.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCreateRoom.addActionListener(e -> showCreateRoomDialog());

        JButton btnJoinSelected = createStyledButton("🔑 加入所选房间", new Color(124, 58, 237), new Color(109, 40, 217), Color.WHITE);
        btnJoinSelected.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnJoinSelected.addActionListener(e -> handleJoinSelectedRoom());

        JButton btnJoinById = createStyledButton("🔍 输入房号加入", new Color(71, 85, 105), new Color(51, 65, 85), Color.WHITE);
        btnJoinById.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnJoinById.addActionListener(e -> showJoinByIdDialog());

        bottomBar.add(btnQuickMatch);
        bottomBar.add(btnCreateRoom);
        bottomBar.add(btnJoinSelected);
        bottomBar.add(btnJoinById);

        root.add(bottomBar, BorderLayout.SOUTH);

        this.add(root);
    }

    private void initLobbyConnection() {
        try {
            URI uri = new URI(serverUrl);
            lobbyWsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("● 大厅已连接");
                        statusLabel.setForeground(new Color(34, 197, 94)); // 绿灯
                    });
                    // 发送大厅列表拉取请求
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
                        statusLabel.setText("● 已断开连接");
                        statusLabel.setForeground(new Color(239, 68, 68)); // 红灯
                    });
                }

                @Override
                public void onError(Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("● 连接错误: " + ex.getMessage());
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
                String actionText = "WAITING".equals(r.getStatus()) ? (r.isHasPassword() ? "点击输入密码加入" : "点击直接加入") : "观战 (2.5版本)";

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

    private void showCreateRoomDialog() {
        JTextField roomIdField = new JTextField(String.valueOf((int) (Math.random() * 9000 + 1000)));
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> sizeBox = new JComboBox<>(new String[]{
                "6 × 6 (经典原版)", "7 × 7 (战术进阶)", "8 × 8 (战略纵深)",
                "9 × 9 (九宫迷阵)", "10 × 10 (双位矩阵)", "11 × 11 (广袤对决)",
                "12 × 12 (宏大博弈)", "13 × 13 (终极拓扑)"
        });

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("房间编号 (可自定义或保持随机):"));
        panel.add(roomIdField);
        panel.add(new JLabel("棋盘规格:"));
        panel.add(sizeBox);
        panel.add(new JLabel("房间密码 (可选，留空表示公开房间):"));
        panel.add(passwordField);

        int res = JOptionPane.showConfirmDialog(this, panel, "创建对战房间", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String roomId = roomIdField.getText().trim();
            if (roomId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "房间号不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int size = sizeBox.getSelectedIndex() + 6;
            String pwd = new String(passwordField.getPassword()).trim();
            enterGameRoom(roomId, size, pwd.isEmpty() ? null : pwd);
        }
    }

    private void handleJoinSelectedRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow < 0) {
            showJoinByIdDialog();
            return;
        }

        RoomSummaryDto room = currentRooms.get(selectedRow);
        if ("PLAYING".equals(room.getStatus()) || room.getPlayerCount() >= 2) {
            JOptionPane.showMessageDialog(this, "该房间正在激烈对战中！\n提示：实时观战系统将于 v2.5 版本正式开放！", "房间已满", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String pwd = null;
        if (room.isHasPassword()) {
            JPasswordField pwdField = new JPasswordField();
            int res = JOptionPane.showConfirmDialog(this, new Object[]{"该房间已设置密码，请输入：", pwdField},
                    "输入房间密码", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            pwd = new String(pwdField.getPassword()).trim();
        }

        enterGameRoom(room.getRoomId(), room.getBoardSize(), pwd);
    }

    private void showJoinByIdDialog() {
        JTextField roomIdField = new JTextField("1001");
        JPasswordField passwordField = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("请输入房间号:"));
        panel.add(roomIdField);
        panel.add(new JLabel("房间密码 (若房间无密码请留空):"));
        panel.add(passwordField);

        int res = JOptionPane.showConfirmDialog(this, panel, "精确输入房号加入", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String roomId = roomIdField.getText().trim();
            if (roomId.isEmpty()) return;
            String pwd = new String(passwordField.getPassword()).trim();
            enterGameRoom(roomId, 6, pwd.isEmpty() ? null : pwd);
        }
    }

    private void handleRandomJoin() {
        // 寻找列表中第一个未满且无密码的房间
        for (RoomSummaryDto r : currentRooms) {
            if ("WAITING".equals(r.getStatus()) && !r.isHasPassword()) {
                enterGameRoom(r.getRoomId(), r.getBoardSize(), null);
                return;
            }
        }
        // 若无现存，直接发起随机加入请求，服务端若也没有则提示创建
        int opt = JOptionPane.showConfirmDialog(this,
                "当前大厅暂无等待中的公开房间，是否立即创建一个新房间？",
                "快速匹配提示", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            showCreateRoomDialog();
        }
    }

    private void enterGameRoom(String roomId, int boardSize, String password) {
        // 关闭大厅专属 WebSocket 会话，释放资源
        if (lobbyWsClient != null && lobbyWsClient.isOpen()) {
            lobbyWsClient.close();
        }
        this.dispose();

        // 启动联机对战主窗口
        OnlineController controller = new OnlineController(serverUrl, roomId, playerName, boardSize, password);
        new GameFrame(controller).display();
    }

    private JButton createStyledButton(String text, Color bg, Color hoverBg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
