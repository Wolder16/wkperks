package div.wkp.component;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.Map;

public interface PerkComponent extends Component, AutoSyncedComponent {
    boolean hasPerk(String perkId);
    int getPerkLevel(String perkId);
    void addPerk(String perkId);
    void removePerk(String perkId);
    void clearPerk(String perkId);
    Map<String, Integer> getPerks();

    // Временные прыжки (от Unstoppable)
    int getTempJumps();
    void addTempJumps(int amount);
    void useTempJump();
    void clearTempJumps();
}