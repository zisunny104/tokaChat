package dev.toka.chat;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import dev.toka.i18n.Messages;
import java.util.HashMap;
import java.util.Map;

/**
 * Chat, join and leave lines and death messages. All wording comes from the language file; the name tag, the place name and
 * the Discord relay come from other plugins when they are there. Every handler is guarded: a fault in a message never stops
 * the game's own chat from working.
 */
public final class Main extends PluginBase implements Listener {
    private Messages messages;
    private Links links;
    private String serverName;
    private boolean relayChat, relayPresence, relayDeaths;
    private final Map<String, String> transfers = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        var config = getConfig();
        messages = new Messages(getClass().getClassLoader(), getDataFolder().toPath(), config.getString("language", "zh-TW"), line -> getLogger().warning(line));
        links = new Links(getServer());
        serverName = config.getString("server-name", getServer().getName());
        relayChat = config.getBoolean("discord.chat", true);
        relayPresence = config.getBoolean("discord.presence", true);
        relayDeaths = config.getBoolean("discord.deaths", true);
        var section = config.getSection("transfers");
        for (String address : section.getKeys(false)) transfers.put(address, section.getString(address));
        getServer().getPluginManager().registerEvents(this, this);
        instance = this;
        listenDiscord();
    }

    private boolean listeningDiscord;
    private static volatile Main instance;

    /** Whether what Discord users write is shown in the game from here. For zero's hand-over check, by reflection. */
    public static boolean listensDiscord() {
        Main main = instance;
        return main != null && main.listeningDiscord;
    }

    /** What a linked Discord user writes in the chat channel is shown in the game, and repeated in the channel in the game's style. */
    private void listenDiscord() {
        if (listeningDiscord || !getConfig().getBoolean("discord.inbound", true)) return;
        listeningDiscord = links.listenDiscord(getName(), payload -> getServer().getScheduler().scheduleTask(this, () -> fromDiscord(payload)));
    }

    private void fromDiscord(String[] payload) {
        if (payload == null || payload.length < 3 || payload[0].isBlank()) return;
        java.util.UUID id;
        try { id = java.util.UUID.fromString(payload[0]); } catch (IllegalArgumentException notAPlayer) { return; }
        Player online = getServer().getOnlinePlayers().get(id);
        String name = online != null ? online.getName() : getServer().getOfflinePlayer(id).getName();
        if (name == null) return;
        String line = text("discord.inbound", links.nameTag(id, name, online), payload[1]);
        getServer().broadcastMessage(line);
        relay(true, line);
        links.deleteDiscord(payload[2]);
    }

    @EventHandler public void onPluginEnabled(cn.nukkit.event.plugin.PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("tokaDiscordLink")) { listeningDiscord = false; getServer().getScheduler().scheduleTask(this, this::listenDiscord); }
    }

    @Override public void onDisable() {
        instance = null;
        links.stopListeningDiscord(getName());
    }

    private String text(String key, Object... args) { return messages.text(key, args); }

    private void relay(boolean enabled, String line) {
        if (enabled) links.discord(TextFormat.clean(line));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        try {
            if (!player.hasPermission("tokachat.speak")) {
                event.setCancelled();
                player.sendMessage(text("chat.muted"));
                return;
            }
            String place = links.regionName(player);
            if (place == null) place = text("chat.outside", player.getLevel().getName());
            String line = text("chat.format", place, links.nameTag(player.getUniqueId(), player.getName(), player), TextFormat.colorize(event.getMessage()));
            event.setFormat(line);
            relay(relayChat, line);
        } catch (RuntimeException | LinkageError fault) {
            getLogger().warning("聊天格式化出錯，改用遊戲預設格式：" + fault);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            String tag = links.nameTag(player.getUniqueId(), player.getName(), player);
            String line = text(player.hasPlayedBefore() ? "join.again" : "join.first", tag, serverName);
            event.setJoinMessage(line);
            relay(relayPresence, line);
        } catch (RuntimeException | LinkageError fault) {
            getLogger().warning("加入訊息出錯：" + fault);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            String raw = event.getReason();
            QuitReason kind = QuitReason.of(raw);
            String target = kind == QuitReason.TRANSFER ? transfers.getOrDefault(QuitReason.target(raw), QuitReason.target(raw)) : "";
            String reason = text(kind.langKey(), serverName, QuitReason.detail(raw), target);
            String line = text("quit.line", links.nameTag(player.getUniqueId(), player.getName(), player), reason);
            event.setQuitMessage(line);
            relay(relayPresence, line);
        } catch (RuntimeException | LinkageError fault) {
            getLogger().warning("離開訊息出錯：" + fault);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        try {
            Player victim = event.getEntity();
            EntityDamageEvent last = victim.getLastDamageCause();
            if (last == null) return;
            boolean byEntity = last instanceof EntityDamageByEntityEvent;
            var damager = byEntity ? ((EntityDamageByEntityEvent) last).getDamager() : null;
            boolean byPlayer = damager instanceof Player;
            var item = byPlayer ? ((Player) damager).getInventory().getItemInHand() : null;
            DeathKind kind = DeathKind.of(last.getCause(), byEntity, byPlayer, item != null && !item.isNull());
            String attacker = damager == null ? "" : byPlayer ? links.nameTag(((Player) damager).getUniqueId(), damager.getName(), (Player) damager) : damager.getName();
            String line = text("death.line", text(kind.langKey(), links.nameTag(victim.getUniqueId(), victim.getName(), victim), attacker, item == null ? "" : item.getName()));
            event.setDeathMessage(line);
            relay(relayDeaths, line);
        } catch (RuntimeException | LinkageError fault) {
            getLogger().warning("死亡訊息出錯：" + fault);
        }
    }
}
