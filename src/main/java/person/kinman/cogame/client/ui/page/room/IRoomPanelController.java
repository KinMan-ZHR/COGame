package person.kinman.cogame.client.ui.page.room;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author KinMan谨漫
 * @date 2025/8/18 09:59
 */
public interface IRoomPanelController {
    void createNewRoom();

    void joinRoom(String selectedValue);

    void sendReadyStatus();

    // 回调设置方法
    void setRoomListUpdater(Consumer<List<String>> updater);

    void setStatusUpdater(Consumer<String> updater);

    void setReadyButtonVisibility(Consumer<Boolean> visibility);

    void setReadyStatusUpdater(Consumer<Boolean> updater);
}
