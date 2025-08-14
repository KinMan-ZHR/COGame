package person.kinman.cogame.sb.controller;


import person.kinman.cogame.sb.eventBus.GlobalEventBus;

/**
 * @author KinMan谨漫
 * @date 2025/8/13 18:02
 */
public abstract class BaseController {
    public BaseController(){
        GlobalEventBus.getBusinessBus().register(this);
        GlobalEventBus.getUiBus().register(this);
    }
}
