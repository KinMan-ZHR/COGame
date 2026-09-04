package person.kinman.cogame.client.ui;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import person.kinman.cogame.client.controller.AiController;
import person.kinman.cogame.client.controller.LocalController;
import person.kinman.cogame.client.profile.ProfileManager;
import person.kinman.cogame.core.net.WsMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 启动主菜单：高对比度深邃科技风界面，支持 6x6~13x13 棋盘规格选择与自绘高可见度模式按钮
 */
public class MainMenuFrame extends JFrame {
    private final JComboBox<String> boardSizeComboBox;

    public MainMenuFrame() {
        this.setTitle("COGame - 《端脑》隔断棋盘博弈 (Ver 2.5)");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(580, 580);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(11, 17, 32)); // 深邃墨蓝黑底色
        mainPanel.setBorder(BorderFactory.createEmptyBorder(26, 36, 26, 36));

        // 1. 标题与副标题
        JLabel titleLabel = new JLabel("端 脑 · 封 锁 博 弈");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(248, 250, 252));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("TOPOLOGICAL BLOCKADE STRATEGY");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subLabel.setForeground(new Color(56, 189, 248)); // 电光青色
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(subLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // 2. 棋盘规格选择面板 (BorderLayout 单行完整展示，绝不换行遮挡)
        JPanel sizePanel = new JPanel(new BorderLayout(14, 0));
        sizePanel.setBackground(new Color(21, 32, 54));
        sizePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        sizePanel.setMaximumSize(new Dimension(500, 48));
        sizePanel.setPreferredSize(new Dimension(500, 48));

        JLabel sizeLabel = new JLabel("棋盘规格 (Board Size):");
        sizeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sizeLabel.setForeground(new Color(226, 232, 240));

        String[] sizeOptions = {
                "🐣 6 × 6 经典小盘 (36格 · 纯净对决)",
                "⚔️ 9 × 9 战术中盘 (81格 · 要塞废墟)",
                "👑 12 × 12 战略大盘 (144格 · 迷宫战场)"
        };
        boardSizeComboBox = new JComboBox<>(sizeOptions);
        styleDarkComboBox(boardSizeComboBox);
        boardSizeComboBox.setSelectedIndex(0);

        sizePanel.add(sizeLabel, BorderLayout.WEST);
        sizePanel.add(boardSizeComboBox, BorderLayout.CENTER);
        mainPanel.add(sizePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // 3. 模式选择按钮 (全自绘高对比度，彻底杜绝系统默认浅色按钮发白)
        ModernMenuButton btnLocal = new ModernMenuButton(
                "① 单机双人对战 (Local 2P)",
                "同屏双人 · 同一键盘交替轮流封锁",
                new Color(2, 132, 199),
                new Color(14, 165, 233),
                new Color(3, 105, 161),
                new Color(56, 189, 248)
        );
        btnLocal.addActionListener(e -> {
            int size = getSelectedBoardSize();
            new GameFrame(new LocalController(size)).display();
        });

        ModernMenuButton btnAi = new ModernMenuButton(
                "② 人机流派对决 (vs 智械AI)",
                "多流派棋风 · 莫衡(控盘大师) · 荆刺(破局猎手) · 玄岳(铁壁守卫)",
                new Color(5, 150, 105),
                new Color(16, 185, 129),
                new Color(4, 120, 87),
                new Color(52, 211, 153)
        );
        btnAi.addActionListener(e -> {
            int size = getSelectedBoardSize();
            new AiPlaystyleDialog(this, selectedStyle -> {
                new GameFrame(new AiController(size, selectedStyle)).display();
            }).setVisible(true);
        });

        ModernMenuButton btnOnline = new ModernMenuButton(
                "③ 网络联机对战 (Online Lobby)",
                "前置登录认证 · 房间列表 · 密码房 · 随机匹配",
                new Color(109, 40, 217),
                new Color(124, 58, 237),
                new Color(91, 33, 182),
                new Color(192, 132, 252)
        );
        btnOnline.addActionListener(e -> {
            showLoginAndLobbyFlow();
        });

        mainPanel.add(btnLocal);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 14)));
        mainPanel.add(btnAi);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 14)));
        mainPanel.add(btnOnline);

        // 4. 底部全屏与快捷键提示
        mainPanel.add(Box.createRigidArea(new Dimension(0, 22)));
        JLabel tipLabel = new JLabel("★ 对局中支持按 F11 一键切换自适应全屏 | P 键开关连通路径高亮");
        tipLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tipLabel.setForeground(new Color(100, 116, 139));
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(tipLabel);

        this.add(mainPanel);
    }

    private int getSelectedBoardSize() {
        return switch (boardSizeComboBox.getSelectedIndex()) {
            case 1 -> 9;
            case 2 -> 12;
            default -> 6;
        };
    }

    private void showLoginAndLobbyFlow() {
        ProfileManager.Profile profile = ProfileManager.loadProfile();
        String defaultServer = (profile.lastServerUrl != null && !profile.lastServerUrl.isEmpty())
                ? profile.lastServerUrl : "ws://127.0.0.1:8088";
        String defaultNick = (profile.hasLoggedInOnline && profile.nickname != null && !"我".equals(profile.nickname))
                ? profile.nickname : "玩家_" + (int) (Math.random() * 900 + 100);

        JDialog loginDialog = new JDialog(this, "玩家前置登录与在线认证", true);
        loginDialog.setSize(440, 290);
        loginDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER_FOCUS, 1),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));

        JLabel l1 = new JLabel("对战服务器 WebSocket 地址:");
        l1.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l1.setForeground(DarkThemeHelper.COLOR_TEXT_PRIMARY);

        JTextField serverField = new JTextField(defaultServer);
        DarkThemeHelper.styleDarkTextField(serverField);

        JLabel hint = new JLabel("说明: 本机测试填 ws://127.0.0.1:8088；远程对战请填实际IP");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(DarkThemeHelper.COLOR_TEXT_MUTED);

        JLabel l2 = new JLabel("我的独立玩家昵称 (全服唯一):");
        l2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l2.setForeground(DarkThemeHelper.COLOR_TEXT_PRIMARY);

        JTextField nameField = new JTextField(defaultNick);
        DarkThemeHelper.styleDarkTextField(nameField);

        panel.add(l1);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(serverField);
        panel.add(Box.createRigidArea(new Dimension(0, 2)));
        panel.add(hint);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(l2);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(nameField);
        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        DarkThemeHelper.DarkButton btnCancel = new DarkThemeHelper.DarkButton("取消",
                new Color(51, 65, 85), new Color(71, 85, 105), DarkThemeHelper.COLOR_BORDER);
        btnCancel.setPreferredSize(new Dimension(80, 34));
        btnCancel.addActionListener(e -> loginDialog.dispose());

        DarkThemeHelper.DarkButton btnLogin = new DarkThemeHelper.DarkButton("登录大厅",
                new Color(109, 40, 217), new Color(124, 58, 237), DarkThemeHelper.COLOR_BORDER_FOCUS);
        btnLogin.setPreferredSize(new Dimension(95, 34));
        btnLogin.addActionListener(e -> {
            String server = serverField.getText().trim();
            String nickname = nameField.getText().trim();

            if (server.isEmpty() || nickname.isEmpty()) {
                showDarkMessageDialog("提示", "服务器地址与昵称均不能为空！");
                return;
            }

            loginDialog.dispose();
            performLogin(server, nickname);
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnLogin);
        panel.add(btnPanel);

        loginDialog.add(panel);
        loginDialog.setVisible(true);
    }

    private void performLogin(String serverUrl, String nickname) {
        JDialog waitDialog = new JDialog(this, "登录认证中", true);
        waitDialog.setSize(380, 120);
        waitDialog.setLocationRelativeTo(this);

        JPanel wPanel = new JPanel(new BorderLayout());
        wPanel.setBackground(DarkThemeHelper.COLOR_BG_PANEL);
        wPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkThemeHelper.COLOR_BORDER_FOCUS, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel wLabel = new JLabel("正在连接服务器并校验唯一昵称，请稍候...");
        wLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        wLabel.setForeground(DarkThemeHelper.COLOR_TEXT_PRIMARY);
        wLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wPanel.add(wLabel, BorderLayout.CENTER);
        waitDialog.add(wPanel);

        final String[] loginError = {null};
        final boolean[] loginSuccess = {false};

        Thread loginThread = new Thread(() -> {
            WebSocketClient testClient = null;
            try {
                CountDownLatch latch = new CountDownLatch(1);
                testClient = new WebSocketClient(new URI(serverUrl)) {
                    @Override
                    public void onOpen(ServerHandshake handshakedata) {
                        send(WsMessage.login(nickname).toJson());
                    }

                    @Override
                    public void onMessage(String message) {
                        try {
                            WsMessage resp = WsMessage.fromJson(message);
                            if (resp != null) {
                                if (WsMessage.TYPE_LOGIN_SUCCESS.equals(resp.getType())) {
                                    loginSuccess[0] = true;
                                    latch.countDown();
                                } else if (WsMessage.TYPE_LOGIN_FAIL.equals(resp.getType())) {
                                    loginError[0] = resp.getMessage();
                                    latch.countDown();
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        if (!loginSuccess[0] && loginError[0] == null) {
                            loginError[0] = "与服务器断开: " + reason;
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception ex) {
                        loginError[0] = (ex != null) ? ex.getMessage() : "网络连接失败";
                        latch.countDown();
                    }
                };

                testClient.connect();
                boolean finished = latch.await(4, TimeUnit.SECONDS);
                if (!finished && !loginSuccess[0]) {
                    loginError[0] = "连接服务器超时 (4秒未响应)";
                }
            } catch (Exception e) {
                loginError[0] = e.getMessage();
            } finally {
                if (testClient != null && testClient.isOpen()) {
                    testClient.close();
                }
                SwingUtilities.invokeLater(waitDialog::dispose);
            }
        });

        loginThread.start();
        waitDialog.setVisible(true);

        if (loginSuccess[0]) {
            ProfileManager.saveProfile(nickname, serverUrl);
            new OnlineLobbyFrame(serverUrl, nickname).setVisible(true);
        } else {
            String reason = (loginError[0] != null) ? loginError[0] : "连接被拒绝";
            showDarkMessageDialog("连接与登录失败",
                    "❌ 登录认证失败: " + reason + "\n\n排查建议：\n1. 若服务端运行在局域网/云服务器，请勿使用 127.0.0.1，请填写服务器实际 IP（如 172.16.24.127）\n2. 确保服务端的 8088 端口已被放行\n3. 若提示昵称已被占用，请更换独一无二的昵称");
        }
    }

    private void showDarkMessageDialog(String title, String message) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(440, 220);
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

        DarkThemeHelper.DarkButton btnOk = new DarkThemeHelper.DarkButton("确认",
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

    private void styleDarkComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBackground(new Color(15, 23, 42));
        combo.setForeground(Color.WHITE);
        combo.setFocusable(false);
        combo.setOpaque(true);
        combo.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true));

        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(21, 32, 54));
                        g2.fillRect(0, 0, getWidth(), getHeight());

                        g2.setColor(new Color(56, 189, 248));
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        int[] xPoints = {cx - 4, cx + 4, cx};
                        int[] yPoints = {cy - 2, cy - 2, cy + 3};
                        g2.fillPolygon(xPoints, yPoints, 3);
                        g2.dispose();
                    }
                };
                btn.setContentAreaFilled(false);
                btn.setOpaque(false);
                btn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                btn.setFocusPainted(false);
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(new Color(15, 23, 42));
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    label.setBackground(new Color(2, 132, 199));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(15, 23, 42));
                    label.setForeground(new Color(241, 245, 249));
                }
                return label;
            }
        });
    }

    /**
     * 自绘制高对比度现代化模式选择按钮（杜绝各操作系统原生白底遮蔽）
     */
    private static class ModernMenuButton extends JButton {
        private final String subtitle;
        private final Color normalBg;
        private final Color hoverBg;
        private final Color pressedBg;
        private final Color borderColor;

        public ModernMenuButton(String title, String subtitle, Color normalBg, Color hoverBg, Color pressedBg, Color borderColor) {
            super(title);
            this.subtitle = subtitle;
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.pressedBg = pressedBg;
            this.borderColor = borderColor;

            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setMaximumSize(new Dimension(450, 56));
            setPreferredSize(new Dimension(450, 56));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            Color currentBg = normalBg;
            if (getModel().isPressed()) {
                currentBg = pressedBg;
            } else if (getModel().isRollover()) {
                currentBg = hoverBg;
            }

            // 1. 绘制渐变科技底色
            GradientPaint gp = new GradientPaint(0, 0, currentBg, 0, h, currentBg.darker());
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

            // 2. 绘制醒目外轮廓荧光边框
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));

            // 3. 绘制标题（纯白超高对比度粗体）
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            int titleX = 22;
            int titleY = 24;
            g2.drawString(getText(), titleX, titleY);

            // 4. 绘制说明副标题（浅蓝高可读字体）
            g2.setColor(new Color(224, 242, 254));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString(subtitle, titleX, titleY + 19);

            // 5. 右侧醒目箭头
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.drawString("➔", w - 30, h / 2 + 5);

            g2.dispose();
        }
    }
}
