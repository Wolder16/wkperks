package div.wkp.perk;

import div.wkp.perk.perks.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PerkRegistry {
    // LinkedHashMap сохраняет порядок добавления перков
    private static final Map<String, Perk> PERKS = new LinkedHashMap<>();

    /** Регистрирует все перки. Вызывается один раз при запуске мода. */
    public static void init() {
        register(new VelocityAugments());
        register(new PulseOrgan());
        register(new UnstoppablePerk());
        register(new RhoGracePerk());
        register(new ElasticLimbsPerk());
        register(new HeavyStrikePerk());
        register(new ConsumptiveReflexPerk());
        // >>> Чтобы добавить новый перк — просто добавь строку сюда <<<
    }

    public static void register(Perk perk) {
        PERKS.put(perk.getId(), perk);
    }

    /** Возвращает перк по ID (или null, если такого нет) */
    public static Perk get(String id) {
        return PERKS.get(id);
    }

    /** Существует ли такой перк */
    public static boolean exists(String id) {
        return PERKS.containsKey(id);
    }

    /** Все зарегистрированные перки */
    public static Collection<Perk> getAll() {
        return PERKS.values();
    }
}