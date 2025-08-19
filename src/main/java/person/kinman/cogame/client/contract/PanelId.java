package person.kinman.cogame.client.contract;

// 面板ID枚举（类型安全）
// 业务标识而非具体实现类，也就是说同一个类的所创建的面板对象必须不同，需要在此处定义面板Id
public enum PanelId {
    START_PANEL,
    MODE_SELECT_PANEL,
    SERVER_CONNECT_PANEL,
    ROOM_LIST_PANEL,
    GAME_MAIN_PANEL,
    LOCAL_GAME_PANEL;
}