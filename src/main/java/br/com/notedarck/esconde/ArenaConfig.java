package br.com.notedarck.esconde;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class ArenaConfig {
    private final FileConfiguration config;

    public ArenaConfig(FileConfiguration config) { this.config = config; }
    public int minPlayers() { return config.getInt("arena.min-players", 2); }
    public int maxPlayers() { return config.getInt("arena.max-players", 16); }
    public int minHunters() { return config.getInt("arena.min-hunters", 1); }
    public int hideSeconds() { return config.getInt("arena.hide-time-seconds", 30); }
    public int gameSeconds() { return config.getInt("arena.game-time-seconds", 300); }
    public int endSeconds() { return config.getInt("arena.end-time-seconds", 8); }
    public boolean catchByDamage() { return config.getBoolean("settings.hunter-catches-by-damage", true); }
    public boolean leatherArmor() { return config.getBoolean("settings.give-hiders-leather-armor", true); }
    public boolean clearOnJoin() { return config.getBoolean("settings.clear-inventory-on-join", true); }
    public boolean restorePlayers() { return config.getBoolean("settings.restore-players-after-game", true); }

    public Location get(String key) {
        ConfigurationSection s = config.getConfigurationSection("arena." + key);
        if (s == null || s.getString("world") == null) return null;
        World world = Bukkit.getWorld(s.getString("world"));
        if (world == null) return null;
        return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    public void set(String key, Location location) {
        String path = "arena." + key;
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    public boolean ready() { return get("lobby") != null && get("hider-spawn") != null && get("hunter-spawn") != null && get("spectator-spawn") != null; }
}
