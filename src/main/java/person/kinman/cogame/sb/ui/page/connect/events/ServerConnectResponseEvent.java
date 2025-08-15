package person.kinman.cogame.sb.ui.page.connect.events;

import person.kinman.cogame.sb.contract.PanelId;
import person.kinman.cogame.sb.ui.page.connect.enums.ConnectionButtonStatus;

/**
 * @author KinMan谨漫
 * @date 2025/8/15 15:57
 */
public record ServerConnectResponseEvent(ConnectionButtonStatus connectionButtonStatus) {
}
