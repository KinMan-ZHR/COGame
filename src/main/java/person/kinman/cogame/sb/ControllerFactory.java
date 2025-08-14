package person.kinman.cogame.sb;

import person.kinman.cogame.sb.controller.StartPanelController;

/**
 * @author KinMan谨漫
 * @date 2025/8/14 17:43
 */
public class ControllerFactory {
    public static void createAllControllers()
    {
        new StartPanelController();
    }
}
