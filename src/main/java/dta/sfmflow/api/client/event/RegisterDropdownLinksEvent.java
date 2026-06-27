package dta.sfmflow.api.client.event;

import dta.sfmflow.api.component.AbstractFlowComponent;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Public clientbound API event posted on the {@code NeoForge.EVENT_BUS} when a node's
 * dropdown configuration menu is being constructed [3].
 * Allows third-party integration add-ons to dynamically register custom text links [3].
 */
public class RegisterDropdownLinksEvent extends Event {
    private final AbstractFlowComponent component;
    private final List<LinkEntry> links = new ArrayList<>();

    /**
     * Initializes a new RegisterDropdownLinksEvent instance [3].
     *
     * @param component the logical flowchart component being targeted by the dropdown [3]
     */
    public RegisterDropdownLinksEvent(AbstractFlowComponent component) {
        this.component = component;
    }

    /**
     * Retrieves the flowchart component associated with this dropdown sequence [3].
     *
     * @return the targeted AbstractFlowComponent [3]
     */
    public AbstractFlowComponent getComponent() { 
        return this.component; 
    }

    /**
     * Registers a new custom text link option to append to the active dropdown menu [3].
     *
     * @param label the display chat Component title [3]
     * @param onClickAction the execution callback run when the link is left-clicked [3]
     */
    public void addLink(Component label, Runnable onClickAction) {
        if (label != null && onClickAction != null) {
            this.links.add(new LinkEntry(label, onClickAction));
        }
    }

    /**
     * Returns an unmodifiable list containing all compiled dropdown links [3].
     *
     * @return unmodifiable list of LinkEntry records [3]
     */
    public List<LinkEntry> getLinks() { 
        return Collections.unmodifiableList(this.links); 
    }

    /**
     * Data record pairing a localized display label to an executable action callback [3].
     *
     * @param label localized display text [3]
     * @param action executable callback [3]
     */
    public record LinkEntry(Component label, Runnable action) {}
}