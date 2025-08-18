package person.kinman.cogame.client.ui;


import person.kinman.cogame.client.contract.IController;
import person.kinman.cogame.client.eventBus.GlobalEventBus;

/**
 * @author KinMan谨漫
 * @date 2025/8/13 18:02
 */
public abstract class BaseController implements IController {
    public BaseController(){
        GlobalEventBus.getBusinessBus().register(this);
        GlobalEventBus.getUiBus().register(this);
    }
}
