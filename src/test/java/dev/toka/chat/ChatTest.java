package dev.toka.chat;

import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeSet;
import static org.junit.Assert.*;

public class ChatTest {
    @Test public void leavingReasonsAreSortedByTheCoresOwnWords() {
        assertEquals(QuitReason.DISCONNECT, QuitReason.of("Disconnected from Server"));
        assertEquals(QuitReason.TIMEOUT, QuitReason.of("Timed out"));
        assertEquals(QuitReason.TRANSFER, QuitReason.of("Transferred to 1.2.3.4:19132"));
        assertEquals(QuitReason.KICKED, QuitReason.of("Kicked by admin Reason: spam"));
        assertEquals(QuitReason.BANNED, QuitReason.of("Banned by admin Reason: cheating"));
        assertEquals(QuitReason.BANNED_LOGIN, QuitReason.of("You are banned Reason: x"));
        assertEquals(QuitReason.OTHER, QuitReason.of("anything else"));
        assertEquals(QuitReason.OTHER, QuitReason.of(null));
    }

    @Test public void theAdministratorsReasonAndTheTransferTargetAreExtracted() {
        assertEquals("spam", QuitReason.detail("Kicked by admin Reason: spam"));
        assertEquals("", QuitReason.detail("Disconnected from Server"));
        assertEquals("1.2.3.4:19132", QuitReason.target("Transferred to 1.2.3.4:19132"));
        assertEquals("", QuitReason.target("Timed out"));
    }

    @Test public void deathsAreSortedByCauseAndWhoWasBehindThem() {
        assertEquals(DeathKind.PLAYER_WEAPON, DeathKind.of(DamageCause.ENTITY_ATTACK, true, true, true));
        assertEquals(DeathKind.PLAYER_BARE, DeathKind.of(DamageCause.ENTITY_ATTACK, true, true, false));
        assertEquals(DeathKind.PLAYER_SHOT, DeathKind.of(DamageCause.PROJECTILE, true, true, true));
        assertEquals(DeathKind.MOB_SHOT, DeathKind.of(DamageCause.PROJECTILE, true, false, false));
        assertEquals(DeathKind.MOB_ATTACK, DeathKind.of(DamageCause.ENTITY_ATTACK, true, false, false));
        assertEquals(DeathKind.EXPLOSION_BY, DeathKind.of(DamageCause.ENTITY_EXPLOSION, true, false, false));
        assertEquals(DeathKind.FALL, DeathKind.of(DamageCause.FALL, false, false, false));
        assertEquals(DeathKind.FIRE, DeathKind.of(DamageCause.FIRE_TICK, false, false, false));
        assertEquals(DeathKind.OTHER, DeathKind.of(null, false, false, false));
    }

    private static TreeSet<String> keys(String file) throws Exception {
        try (InputStream in = ChatTest.class.getResourceAsStream("/lang/" + file)) {
            TreeSet<String> out = new TreeSet<>();
            flatten("", new Yaml().load(in), out);
            return out;
        }
    }

    private static void flatten(String prefix, Object node, TreeSet<String> out) {
        if (node instanceof Map<?, ?> map) map.forEach((k, v) -> flatten(prefix.isEmpty() ? k.toString() : prefix + "." + k, v, out));
        else out.add(prefix);
    }

    @Test public void bothLanguagesAgreeAndEveryKindHasWording() throws Exception {
        assertEquals(keys("zh-TW.yml"), keys("en-US.yml"));
        var keys = keys("zh-TW.yml");
        for (DeathKind kind : DeathKind.values()) assertTrue(kind.name(), keys.contains(kind.langKey()));
        for (QuitReason reason : QuitReason.values()) assertTrue(reason.name(), keys.contains(reason.langKey()));
    }
}
