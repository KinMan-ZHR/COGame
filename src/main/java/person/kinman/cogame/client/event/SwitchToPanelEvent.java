package person.kinman.cogame.client.event;

import person.kinman.cogame.client.contract.PanelId;

/**
 * @author KinMan谨漫
 * @date 2025/8/15 11:10
 */
public record SwitchToPanelEvent(PanelId panelId) {
    public SwitchToPanelEvent{
        if (panelId == null) {
            throw new IllegalArgumentException("panelId不能为null");
        }
    }
}
