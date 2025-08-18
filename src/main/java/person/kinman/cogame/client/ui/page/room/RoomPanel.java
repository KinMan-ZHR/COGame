package person.kinman.cogame.client.ui.page.room;

import person.kinman.cogame.client.ui.BasePanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * 房间面板 - 仅负责房间相关的UI展示和用户交互
 */
public class RoomPanel extends BasePanel {
    private JList<String> roomList;
    private DefaultListModel<String> roomListModel;
    private JLabel statusLabel;
    private JButton createRoomBtn;
    private JButton joinRoomBtn;
    private JButton readyBtn;
    private final IRoomPanelController roomController;

    public RoomPanel(IRoomPanelController roomController) {
        this.roomController = roomController;
    }

    @Override
    protected void initListeners() {
        createRoomBtn.addActionListener(e ->{
            roomController.createNewRoom();
        });

        joinRoomBtn.addActionListener(e ->  {
            roomController.joinRoom(roomList.getSelectedValue());
        });
        readyBtn.addActionListener(e -> {
            roomController.sendReadyStatus();
        });
        roomController.setRoomListUpdater(this::updateRoomList);
        roomController.setStatusUpdater(this::updateStatus);
        roomController.setReadyButtonVisibility(this::showReadyButton);
        roomController.setReadyStatusUpdater(this::setReadyStatus);
    }

    protected void initUI() {
        setBackground(new Color(40, 20, 60));
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 房间列表区域
        JPanel roomListPanel = createRoomListPanel();
        add(roomListPanel, BorderLayout.CENTER);

        // 底部操作区域
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * 创建房间列表面板
     */
    private JPanel createRoomListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 20, 60));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                "可用房间",
                0, 0,
                new Font("微软雅黑", Font.PLAIN, 16),
                Color.WHITE
        ));

        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setBackground(new Color(60, 40, 80));
        roomList.setForeground(Color.WHITE);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建控制面板（按钮和状态）
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 20, 60));

        // 状态标签
        statusLabel = new JLabel("请先连接服务器");
        statusLabel.setForeground(Color.WHITE);
        panel.add(statusLabel, BorderLayout.WEST);

        // 按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(new Color(40, 20, 60));

//        // 操作按钮面板
//        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));
//        buttonPanel.setBackground(new Color(40, 20, 60));
//        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // 创建房间按钮
        createRoomBtn = createButton("创建房间", new Color(30, 144, 255));
        createRoomBtn.setEnabled(false);
        buttonPanel.add(createRoomBtn);

        // 加入房间按钮
        joinRoomBtn = createButton("加入选中房间", new Color(30, 144, 255));
        joinRoomBtn.setEnabled(false);
        buttonPanel.add(joinRoomBtn);

        // 准备按钮
        readyBtn = createButton("准备", new Color(255, 165, 0));
        readyBtn.setVisible(false);
        buttonPanel.add(readyBtn);

        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    /**
     * 更新房间列表
     */
    public void updateRoomList(List<String> roomIds) {
        SwingUtilities.invokeLater(() -> {
            roomListModel.clear();
            for (String roomId : roomIds) {
                if (!roomId.trim().isEmpty()) {
                    roomListModel.addElement(roomId + " (1/2人)");
                }
            }
            joinRoomBtn.setEnabled(!roomIds.isEmpty());
        });
    }

    /**
     * 更新状态文本
     */
    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    /**
     * 设置连接状态，更新按钮可用性
     */
    public void setConnected(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            createRoomBtn.setEnabled(connected);
            joinRoomBtn.setEnabled(connected && !roomListModel.isEmpty());
        });
    }

    /**
     * 显示或隐藏准备按钮
     */
    public void showReadyButton(boolean show) {
        SwingUtilities.invokeLater(() -> {
            readyBtn.setVisible(show);
            if (show) {
                readyBtn.setText("准备");
                readyBtn.setEnabled(true);
            }
        });
    }

    /**
     * 获取选中的房间ID
     */
    public String getSelectedRoomId() {
        String selected = roomList.getSelectedValue();
        if (selected == null) return null;
        // 提取房间ID（格式: "ROOM_1000 (1/2人)"）
        return selected.substring(0, selected.indexOf(" "));
    }

    /**
     * 注册创建房间按钮监听器
     */
    public void addCreateRoomListener(ActionListener listener) {
        createRoomBtn.addActionListener(listener);
    }

    /**
     * 注册加入房间按钮监听器
     */
    public void addJoinRoomListener(ActionListener listener) {
        joinRoomBtn.addActionListener(listener);
    }

    /**
     * 注册准备按钮监听器
     */
    public void addReadyListener(ActionListener listener) {
        readyBtn.addActionListener(listener);
    }

    /**
     * 创建按钮的工具方法
     */
    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        button.setPreferredSize(new Dimension(140, 40));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void setReadyButtonVisible(boolean visible) {
        readyBtn.setVisible(visible);
    }

    public void setRoomButtonsEnabled(boolean enabled) {
        createRoomBtn.setEnabled(enabled);
        joinRoomBtn.setEnabled(enabled && !roomListModel.isEmpty());
    }


    public void setReadyStatus(boolean isReady) {
        readyBtn.setText(isReady ? "已准备" : "准备");
        readyBtn.setEnabled(!isReady);
    }
}
    