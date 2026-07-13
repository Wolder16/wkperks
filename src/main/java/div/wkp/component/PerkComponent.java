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

    /**
     * Изменяет уровень перка на delta.
     * Например, delta = 10 добавляет 10 уровней,
     * delta = -20 отнимает 20 уровней.
     */
    void changePerkLevel(String perkId, int delta);

    Map<String, Integer> getPerks();

    int getTempJumps();

    void addTempJumps(int amount);

    void useTempJump();

    void clearTempJumps();
}