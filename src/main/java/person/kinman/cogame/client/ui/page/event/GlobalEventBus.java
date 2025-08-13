package person.kinman.cogame.client.ui.page.event;

import com.google.common.eventbus.EventBus;

/**
 * 全局事件总线：单例模式，供所有组件共享
 */
public class GlobalEventBus {
    // 单例实例（Guava的EventBus线程安全）
    private static final EventBus INSTANCE = new EventBus();

    private GlobalEventBus() {} // 禁止外部实例化

    public static EventBus getInstance() {
        return INSTANCE;
    }
}