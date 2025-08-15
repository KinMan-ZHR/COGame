package person.kinman.cogame.sb.ui;

import com.google.common.eventbus.Subscribe;
import person.kinman.cogame.sb.contract.IUIManager;
import person.kinman.cogame.sb.event.ConnectionStatusEvent;
import person.kinman.cogame.sb.event.SwitchToPanelEvent;
import person.kinman.cogame.sb.event.SystemErrorDialogEvent;


/**
 * @author KinMan谨漫
 * @date 2025/8/15 11:20
 */
public class UIController extends BaseController {
    private final IUIManager uiManager;
    public UIController(IUIManager uiManager) {
        this.uiManager = uiManager;
    }

    @Subscribe
    public void handleSwitchToPanelEvent(SwitchToPanelEvent event) {
        uiManager.showPanel(event.panelId());
    }

    @Subscribe
    public void handleSystemErrorDialogEvent(SystemErrorDialogEvent event) {
        uiManager.showErrorDialog(event.message());
    }

    @Subscribe
    public void handleConnectStatusEvent(ConnectionStatusEvent event) {
        uiManager.showErrorDialog(event.message());
    }
}
