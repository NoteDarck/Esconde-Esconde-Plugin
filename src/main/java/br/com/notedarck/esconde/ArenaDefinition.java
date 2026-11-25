package br.com.notedarck.esconde;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class ArenaDefinition {
    private final String id;
    private final FileConfiguration config;
    ArenaDefinition(String id, FileConfiguration config) { this.id = id; this.config = config; }
    public String id() { return id; }
    private String p(String key) { return "arenas." + id + "." + key; }
    public boolean enabled() { return config.getBoolean(p("enabled"), true); }
    public int minPlayers() { return config.getInt(p("min-players"), 2); }
    public int maxPlayers() { return config.getInt(p("max-players"), 16); }
    public int hunters() { return config.getInt(p("hunters"), 1); }
    public boolean autoStart() { return config.getBoolean(p("auto-start.enabled"), true); }
    public int autoStartSeconds() { return Math.max(1, config.getInt(p("auto-start.countdown-seconds"), 10)); }
    public int rounds() { return Math.max(1, config.getInt(p("rounds"), 3)); }
    public int preparationSeconds() { return Math.max(1, config.getInt(p("preparation-seconds"), 30)); }
    public int roundSeconds() { return Math.max(10, config.getInt(p("round-seconds"), 600)); }
    public int betweenRoundsSeconds() { return Math.max(1, config.getInt(p("between-rounds-seconds"), 8)); }
    public int hiderHits() { return Math.max(1, config.getInt(p("hider-hits"), 5)); }
    public boolean translocation() { return config.getBoolean(p("mechanics.translocation"), false); }
    public boolean fireworks() { return config.getBoolean(p("mechanics.fireworks"), false); }
    public boolean catchByDamage() { return config.getBoolean(p("mechanics.catch-by-damage"), false); }
    public boolean disableLocatorBar() { return config.getBoolean(p("mechanics.disable-locator-bar"), true); }
    public Location location(String name) { return read(p(name)); }
    public Location lobby() { return location("lobby"); }
    public Location hiderSpawn() { return location("hider-spawn"); }
    public Location hunterSpawn() { return location("hunter-spawn"); }
    public Location spectatorSpawn() { return location("spectator-spawn"); }
    public Location pos1() { return location("pos1"); }
    public Location pos2() { return location("pos2"); }
    public boolean ready() { return enabled() && lobby() != null && hiderSpawn() != null && hunterSpawn() != null && spectatorSpawn() != null; }
    public void setLocation(String name, Location location) { write(p(name), location); }
    private Location read(String path) { ConfigurationSection s = config.getConfigurationSection(path); if (s == null) return null; String worldName = s.getString("world"); World world = worldName == null ? null : Bukkit.getWorld(worldName); if (world == null) return null; return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"), (float)s.getDouble("yaw"), (float)s.getDouble("pitch")); }
    private void write(String path, Location l) { config.set(path + ".world", l.getWorld().getName()); config.set(path + ".x", l.getX()); config.set(path + ".y", l.getY()); config.set(path + ".z", l.getZ()); config.set(path + ".yaw", l.getYaw()); config.set(path + ".pitch", l.getPitch()); }
    public boolean inside(Location l) { Location a = pos1(), b = pos2(); if (a == null || b == null || l.getWorld() == null || !l.getWorld().equals(a.getWorld())) return true; return l.getX() >= Math.min(a.getX(),b.getX()) && l.getX() <= Math.max(a.getX(),b.getX()) && l.getY() >= Math.min(a.getY(),b.getY()) && l.getY() <= Math.max(a.getY(),b.getY()) && l.getZ() >= Math.min(a.getZ(),b.getZ()) && l.getZ() <= Math.max(a.getZ(),b.getZ()); }
}
