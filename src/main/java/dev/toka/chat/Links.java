package dev.toka.chat;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * The other plugins chat can use, reached reflectively so none of them is needed: tokaDisplay for the name tag, tokaRegions
 * for the place name, tokaDiscordLink for relaying. Each answer has a plain fallback, so chat works with none of them.
 */
final class Links {
    private final Server server;

    Links(Server server) { this.server = server; }

    private Method method(String plugin, String type, String name, Class<?>... parameters) {
        Plugin found = server.getPluginManager().getPlugin(plugin);
        if (found == null || !found.isEnabled()) return null;
        try { return Class.forName(type, true, found.getClass().getClassLoader()).getMethod(name, parameters); }
        catch (ReflectiveOperationException | LinkageError missing) { return null; }
    }

    /** The composed name tag from tokaDisplay, or just the player's name. */
    String nameTag(UUID id, String name, Player online) {
        Method compose = method("tokaDisplay", "dev.toka.display.Main", "nameTag", UUID.class, String.class, Player.class);
        if (compose != null) {
            try { String tag = (String) compose.invoke(null, id, name, online); if (tag != null && !tag.isBlank()) return tag; }
            catch (ReflectiveOperationException | LinkageError failed) { /* use the plain name */ }
        }
        return name;
    }

    /** The name of the region the player stands in, or null. */
    String regionName(Player player) {
        Method lookup = method("tokaRegions", "dev.toka.regions.Main", "regionNameAt", String.class, int.class, int.class, int.class);
        if (lookup == null) return null;
        try { return (String) lookup.invoke(null, player.getLevel().getFolderName(), player.getFloorX(), player.getFloorY(), player.getFloorZ()); }
        catch (ReflectiveOperationException | LinkageError failed) { return null; }
    }

    /** Sends a line to Discord if the link is up; otherwise nothing happens. */
    void discord(String text) {
        Method send = method("tokaDiscordLink", "dev.toka.discord.api.DiscordLink", "sendMessage", String.class);
        if (send == null) return;
        try { send.invoke(null, text); } catch (ReflectiveOperationException | LinkageError failed) { /* Discord is optional */ }
    }
}
