package person.kinman.cogame.client.ui.page.room;

import person.kinman.cogame.client.ui.page.event.GlobalEventBus;
import person.kinman.cogame.client.contract.Communicable;
import person.kinman.cogame.client.event.SwitchToPanelEvent;
import person.kinman.cogame.client.ui.BaseController;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 房间控制器 - 处理所有房间相关的业务逻辑，不包含任何UI代码
 */
public class RoomPanelController extends BaseController implements IRoomPanelController{
    private final Communicable networkProxy;
    private String currentRoomId; // 当前所在房间ID
    private List<String> roomList = new ArrayList<>(); // 房间列表缓存

    // 房间相关回调（通知UI更新房间列表、准备状态等）
    private Consumer<List<String>> roomListUpdater; // 房间列表更新
    private Consumer<String> statusUpdater; // 房间状态文本（如“已加入房间”）
    private Consumer<Boolean> readyButtonVisibility; // 准备按钮显示/隐藏
    private Consumer<Boolean> readyStatusUpdater; // 准备状态更新（已准备/未准备）

    public RoomPanelController(Communicable networkProxy) {
        this.networkProxy = networkProxy;
    }

    /**
     * 处理服务器消息（仅关注房间相关消息）
     */
    public void handleServerMessage(String message) {
        // 1. 房间列表更新消息
        if (message.startsWith("ROOM_LIST:")) {
            updateRoomList(message.substring("ROOM_LIST:".length()));
            return;
        }

        // 2. 加入房间成功消息
        if (message.startsWith("JOINED_ROOM:")) {
            currentRoomId = message.substring("JOINED_ROOM:".length());
            statusUpdater.accept("已加入房间: " + currentRoomId);
            readyButtonVisibility.accept(true); // 显示准备按钮
            return;
        }

        // 3. 游戏开始消息
        if (message.contains("游戏开始")) {
            statusUpdater.accept("游戏开始！");
            GlobalEventBus.getInstance().post(new SwitchToPanelEvent());
            return;
        }

        // 4. 其他房间相关消息（如玩家加入/离开房间）
        statusUpdater.accept(message);
    }

    /**
     * 创建新房间
     */
    public void createNewRoom() {
        networkProxy.sendMessage("CREATE_ROOM");
        statusUpdater.accept("正在创建房间...");
    }

    /**
     * 加入指定房间
     */
    public void joinRoom(String roomId) {
        networkProxy.sendMessage("JOIN_ROOM:" + roomId);
        statusUpdater.accept("正在加入房间 " + roomId + "...");
    }

    /**
     * 发送准备状态
     */
    public void sendReadyStatus() {
        if (currentRoomId == null) {
            statusUpdater.accept("未加入房间，无法准备");
            return;
        }
        networkProxy.sendMessage("READY");
        readyStatusUpdater.accept(true); // 更新为“已准备”
        statusUpdater.accept("已准备，等待其他玩家...");
    }

    /**
     * 刷新房间列表（主动请求服务器更新）
     */
    public void refreshRoomList() {
        networkProxy.sendMessage("REQUEST_ROOM_LIST"); // 向服务器请求最新房间列表
        statusUpdater.accept("正在刷新房间列表...");
    }

    /**
     * 解析并更新房间列表
     */
    private void updateRoomList(String roomStr) {
        String[] roomIds = roomStr.split(",");
        roomList.clear();
        for (String roomId : roomIds) {
            if (!roomId.trim().isEmpty()) {
                roomList.add(roomId);
            }
        }
        roomListUpdater.accept(new ArrayList<>(roomList)); // 通知UI更新列表
        statusUpdater.accept("房间列表已更新（共" + roomList.size() + "个房间）");
    }

    // 回调设置方法
    @Override
    public void setRoomListUpdater(Consumer<List<String>> updater) {
        this.roomListUpdater = updater;
    }

    @Override
    public void setStatusUpdater(Consumer<String> updater) {
        this.statusUpdater = updater;
    }

    @Override
    public void setReadyButtonVisibility(Consumer<Boolean> visibility) {
        this.readyButtonVisibility = visibility;
    }

    @Override
    public void setReadyStatusUpdater(Consumer<Boolean> updater) {
        this.readyStatusUpdater = updater;
    }
}