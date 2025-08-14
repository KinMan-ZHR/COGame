package person.kinman.cogame.sb.contract;

// 面板ID枚举（类型安全）
// 业务标识而非具体实现类，也就是说同一个类的所创建的面板对象必须不同，需要在此处定义面板Id
public enum PanelId {
    START_PANEL("start_panel"),
    MODE_SELECT_PANEL("mode_select_panel"),
    SERVER_CONNECT_PANEL("server_connect_panel"),
    ROOM_LIST_PANEL("room_list_panel"),
    GAME_MAIN_PANEL("game_main_panel");

    private final String id;

    PanelId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}