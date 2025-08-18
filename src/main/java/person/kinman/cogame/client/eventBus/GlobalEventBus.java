package person.kinman.cogame.client.eventBus;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.Executors;

/**
 * 全局事件总线：单例模式，供所有组件共享
 */
public class GlobalEventBus {
    // 单例实例（Guava的EventBus线程安全）
    // UI事件总线（同步，在EDT线程处理）
    private static final EventBus UI_BUS = new EventBus();
    // 业务事件总线（异步，通过线程池处理）
    private static final EventBus BUSINESS_BUS = new AsyncEventBus(Executors.newCachedThreadPool());

    private GlobalEventBus() {}
    public static EventBus getUiBus() { return UI_BUS; }
    public static EventBus getBusinessBus() { return BUSINESS_BUS; }
}