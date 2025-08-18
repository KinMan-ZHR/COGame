package person.kinman.cogame.client.ui;

import com.google.common.eventbus.Subscribe;
import person.kinman.cogame.client.contract.IUIManager;
import person.kinman.cogame.client.event.ConnectionStatusEvent;
import person.kinman.cogame.client.event.SwitchToPanelEvent;
import person.kinman.cogame.client.event.SystemErrorDialogEvent;


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
