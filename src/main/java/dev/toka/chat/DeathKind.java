package dev.toka.chat;

import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * What kind of death it was, from the cause and whether a player or a mob was behind it. The wording is in the language
 * file under {@code death.<kind>}; this class only decides which line applies.
 */
public enum DeathKind {
    PLAYER_WEAPON, PLAYER_BARE, PLAYER_SHOT, MOB_SHOT, EXPLOSION_BY, LIGHTNING, MOB_ATTACK, SUFFOCATION, FALL, FIRE, LAVA,
    DROWNING, BLOCK_EXPLOSION, VOID, SUICIDE, MAGIC, OTHER;

    /**
     * @param cause   what the game says hurt the player last
     * @param byEntity whether another entity was involved
     * @param byPlayer whether that entity was a player
     * @param armed   whether a player attacker held something
     */
    public static DeathKind of(DamageCause cause, boolean byEntity, boolean byPlayer, boolean armed) {
        if (cause == null) return OTHER;
        if (byEntity) {
            if (cause == DamageCause.PROJECTILE) return byPlayer ? PLAYER_SHOT : MOB_SHOT;
            if (byPlayer) return armed ? PLAYER_WEAPON : PLAYER_BARE;
            return switch (cause) {
                case ENTITY_EXPLOSION -> EXPLOSION_BY;
                case LIGHTNING -> LIGHTNING;
                case ENTITY_ATTACK -> MOB_ATTACK;
                default -> OTHER;
            };
        }
        return switch (cause) {
            case SUFFOCATION -> SUFFOCATION;
            case FALL -> FALL;
            case FIRE, FIRE_TICK -> FIRE;
            case LAVA -> LAVA;
            case DROWNING -> DROWNING;
            case BLOCK_EXPLOSION -> BLOCK_EXPLOSION;
            case VOID -> VOID;
            case SUICIDE -> SUICIDE;
            case MAGIC -> MAGIC;
            case LIGHTNING -> LIGHTNING;
            default -> OTHER;
        };
    }

    public String langKey() { return "death." + name().toLowerCase(java.util.Locale.ROOT); }
}
