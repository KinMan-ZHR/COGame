package person.kinman.cogame.client.ui;

import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.core.model.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.RoundRectangle2D;

/**
 * 房间等待室：房主建房后展示等待对手加入的全流程界面
 * 包含房间号一键复制、密码状态、席位卡片及开局自动过渡
 */
public class RoomWaitingDialog extends JDialog {
    private final OnlineController controller;
    private final String serverUrl;
    private final String playerName;
    private final Frame parentFrame;

    private JLabel statusNoticeLabel;
    private JLabel p2NameLabel;
    private JLabel p2StatusLabel;
    private DarkThemeHelper.DarkButton btnCopy;
    private javax.swing.Timer dotTimer;
    private int dotCount = 0;
    private person.kinman.cogame.core.model.TurnOrderPreference hostPreference;
    private JLabel p1RoleLabel;

    public RoomWaitingDialog(Frame parent, OnlineController controller, String serverUrl, String playerName) {
        this(parent, controller, serverUrl, playerName, person.kinman.cogame.core.model.TurnOrderPreference.RANDOM);
    }

    public RoomWaitingDialog(Frame parent, OnlineController controller, String serverUrl, String playerName, person.kinman.cogame.core.model.TurnOrderPreference initialPref) {
        super(parent, "COGame 对战房间等待室", false);
        this.parentFrame = parent;
        this.controller = controller;
        this.serverUrl = serverUrl;
        this.playerName = playerName;
        this.hostPreference = (initialPref != null) ? initialPref : person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;

        this.setSize(560, 485);
        this.setLocationRelativeTo(parent);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setResizable(false);

        initUI();
        setupListeners();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(DarkThemeHelper.COLOR_BG_DARKEST);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER_FOCUS, 2),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));

        // 1. 顶部：房间核心卡片 (房号 + 规格 + 密码)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel title = new JLabel("⚔️ 对战房间已成功建立");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(DarkThemeHelper.COLOR_TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(title);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 房号与复制按钮栏
        JPanel roomInfoBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        roomInfoBar.setOpaque(false);

        String displayRoomId = (controller.getRoomId() != null) ? controller.getRoomId() : "---";
        JLabel roomLabel = new JLabel("房间号: " + displayRoomId);
        roomLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        roomLabel.setForeground(DarkThemeHelper.COLOR_BORDER_FOCUS);

        btnCopy = new DarkThemeHelper.DarkButton("📋 一键复制", new Color(30, 41, 59), new Color(51, 65, 85), DarkThemeHelper.COLOR_BORDER);
        btnCopy.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCopy.setPreferredSize(new Dimension(110, 32));
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(displayRoomId), null);
            btnCopy.setText("✅ 已复制！");
            Timer t = new Timer(2000, evt -> btnCopy.setText("📋 一键复制"));
            t.setRepeats(false);
            t.start();
        });

        roomInfoBar.add(roomLabel);
        roomInfoBar.add(btnCopy);
        topPanel.add(roomInfoBar);
        topPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        // 规格与密码
        String pwdText = (controller.getPassword() != null && !controller.getPassword().isEmpty())
                ? "🔒 私密密码: " + controller.getPassword() : "🔓 公开房间 (大厅可见)";
        JLabel metaLabel = new JLabel("棋盘规格: " + controller.getBoardSize() + " × " + controller.getBoardSize() + "   |   " + pwdText);
        metaLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        metaLabel.setForeground(DarkThemeHelper.COLOR_TEXT_MUTED);
        metaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(metaLabel);

        root.add(topPanel, BorderLayout.NORTH);

        // 2. 中部：对战双方席位卡片与分先设置
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JPanel seatsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        seatsPanel.setOpaque(false);

        // 左席位：房主
        JPanel p1Card = createPlayerCard("👑 房主 (等待仲裁)", playerName, "🟢 已就绪", new Color(14, 116, 144), new Color(56, 189, 248));

        // 右席位：P2 对手 (等待中)
        JPanel p2Card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 41, 59));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(245, 158, 11)); // 琥珀金边框
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 12, 12));
                g2.dispose();
            }
        };
        p2Card.setLayout(new BoxLayout(p2Card, BoxLayout.Y_AXIS));
        p2Card.setOpaque(false);
        p2Card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel p2Role = new JLabel("⚔️ 挑战者席位");
        p2Role.setFont(new Font("SansSerif", Font.BOLD, 13));
        p2Role.setForeground(new Color(251, 191, 36));
        p2Role.setAlignmentX(Component.CENTER_ALIGNMENT);

        p2NameLabel = new JLabel("等待对手加入...");
        p2NameLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        p2NameLabel.setForeground(new Color(226, 232, 240));
        p2NameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        p2StatusLabel = new JLabel("⏳ 席位空闲 (1/2)");
        p2StatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        p2StatusLabel.setForeground(DarkThemeHelper.COLOR_TEXT_MUTED);
        p2StatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        p2Card.add(p2Role);
        p2Card.add(Box.createRigidArea(new Dimension(0, 8)));
        p2Card.add(p2NameLabel);
        p2Card.add(Box.createRigidArea(new Dimension(0, 6)));
        p2Card.add(p2StatusLabel);

        seatsPanel.add(p1Card);
        seatsPanel.add(p2Card);
        centerPanel.add(seatsPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 分先偏好调节栏
        JPanel turnOrderBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        turnOrderBar.setOpaque(false);

        JLabel prefLabel = new JLabel("🎯 房主分先意愿:");
        prefLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        prefLabel.setForeground(new Color(226, 232, 240));
        turnOrderBar.add(prefLabel);

        JComboBox<String> prefBox = new JComboBox<>(new String[]{
                "🎲 随机分先 (双方同选先手则随机掷骰)",
                "🔵 执先 (先手 P1)",
                "🔴 执后 (后手 P2)"
        });
        DarkThemeHelper.styleDarkComboBox(prefBox);
        if (hostPreference == person.kinman.cogame.core.model.TurnOrderPreference.FIRST) prefBox.setSelectedIndex(1);
        else if (hostPreference == person.kinman.cogame.core.model.TurnOrderPreference.SECOND) prefBox.setSelectedIndex(2);
        else prefBox.setSelectedIndex(0);

        prefBox.addActionListener(e -> {
            hostPreference = switch (prefBox.getSelectedIndex()) {
                case 1 -> person.kinman.cogame.core.model.TurnOrderPreference.FIRST;
                case 2 -> person.kinman.cogame.core.model.TurnOrderPreference.SECOND;
                default -> person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
            };
            controller.sendSetPreference(hostPreference);
        });
        turnOrderBar.add(prefBox);
        centerPanel.add(turnOrderBar);

        root.add(centerPanel, BorderLayout.CENTER);

        // 3. 底部：动态加载呼吸提示与退出按钮
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        statusNoticeLabel = new JLabel("● 正在等待对手加入中，若双方均选先手开局将公平随机分配...");
        statusNoticeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusNoticeLabel.setForeground(new Color(251, 191, 36));
        statusNoticeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        DarkThemeHelper.DarkButton btnLeave = new DarkThemeHelper.DarkButton("🚪 退出房间并返回大厅",
                new Color(220, 38, 38), new Color(239, 68, 68), new Color(185, 28, 28));
        btnLeave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLeave.setPreferredSize(new Dimension(220, 36));
        btnLeave.addActionListener(e -> handleLeaveRoom());

        bottomPanel.add(statusNoticeLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        bottomPanel.add(btnLeave);

        root.add(bottomPanel, BorderLayout.SOUTH);
        this.add(root);

        // 动态呼吸点动画
        dotTimer = new javax.swing.Timer(500, e -> {
            dotCount = (dotCount + 1) % 4;
            String dots = ".".repeat(dotCount);
            p2NameLabel.setText("等待对手加入" + dots);
        });
        dotTimer.start();
    }

    private JPanel createPlayerCard(String role, String name, String status, Color headerColor, Color borderColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 41, 59));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 12, 12));
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel roleLabel = new JLabel(role);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        roleLabel.setForeground(headerColor);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        nameLabel.setForeground(DarkThemeHelper.COLOR_TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLabel = new JLabel(status);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(34, 197, 94));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(roleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(nameLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(statusLabel);
        return card;
    }

    private void setupListeners() {
        controller.setOnGameStarted(state -> SwingUtilities.invokeLater(() -> onOpponentJoined(state)));
    }

    private void onOpponentJoined(GameState state) {
        if (dotTimer != null) {
            dotTimer.stop();
        }

        String opponentName = (state != null && state.getP2() != null) ? state.getP2().getName() : "挑战者";
        p2NameLabel.setText(opponentName);
        p2StatusLabel.setText("🟢 已就绪 (对局即将开始)");
        p2StatusLabel.setForeground(new Color(34, 197, 94));

        int myId = controller.getMyPlayerId();
        String roleText = (myId == 1) ? "您执先手 (P1 电光青)" : "对手执先手 (P1)，您执后手 (P2 炽金琥珀)";
        statusNoticeLabel.setText("🎉 对手已加入！分先判定：" + roleText + "，立即启动战场...");
        statusNoticeLabel.setForeground(new Color(34, 197, 94));

        // 延迟 800ms 自动关闭等待室，顺畅切入战场
        Timer closeTimer = new Timer(800, e -> {
            this.dispose();
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    private void handleLeaveRoom() {
        if (dotTimer != null) {
            dotTimer.stop();
        }
        this.dispose();
        controller.close();
        if (parentFrame != null) {
            parentFrame.dispose();
        }
        new OnlineLobbyFrame(serverUrl, playerName).setVisible(true);
    }
}
