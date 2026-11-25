package br.com.notedarck.esconde;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

public final class ArenaManager {
    private final EscondeEscondePlugin plugin;
    private final FileConfiguration config;
    private final Map<String, ArenaDefinition> arenas = new LinkedHashMap<>();
    ArenaManager(EscondeEscondePlugin plugin) { this.plugin = plugin; this.config = plugin.getConfig(); load(); }
    public void load() {
        migrateDefaults();
        arenas.clear(); ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null || section.getKeys(false).isEmpty()) migrateLegacy();
        section = config.getConfigurationSection("arenas");
        if (section != null) for (String id : section.getKeys(false)) arenas.put(id.toLowerCase(Locale.ROOT), new ArenaDefinition(id, config));
    }
    private void migrateDefaults() {
        if (config.getInt("settings.config-version", 1) >= 2) return;
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section != null) for (String id : section.getKeys(false)) {
            String base = "arenas." + id;
            config.set(base + ".hider-hits", config.getInt(base + ".hider-hits", 5));
            config.set(base + ".mechanics.translocation", false);
            config.set(base + ".mechanics.fireworks", false);
            config.set(base + ".mechanics.catch-by-damage", false);
            config.set(base + ".mechanics.disable-locator-bar", true);
        }
        config.set("settings.config-version", 2);
        plugin.saveConfig();
    }
    private void migrateLegacy() { config.set("arenas.principal.enabled", true); copy("arena.min-players", "arenas.principal.min-players", 2); copy("arena.max-players", "arenas.principal.max-players", 16); copy("arena.min-hunters", "arenas.principal.hunters", 1); copy("arena.hide-time-seconds", "arenas.principal.preparation-seconds", 30); copy("arena.game-time-seconds", "arenas.principal.round-seconds", 600); copy("arena.end-time-seconds", "arenas.principal.between-rounds-seconds", 8); copyLocation("lobby", "lobby"); copyLocation("hider-spawn", "hider-spawn"); copyLocation("hunter-spawn", "hunter-spawn"); copyLocation("spectator-spawn", "spectator-spawn"); config.set("arenas.principal.rounds", 3); config.set("arenas.principal.hider-hits", 5); config.set("arenas.principal.mechanics.translocation", false); config.set("arenas.principal.mechanics.fireworks", false); config.set("arenas.principal.mechanics.catch-by-damage", false); config.set("arenas.principal.mechanics.disable-locator-bar", true); plugin.saveConfig(); }
    private void copy(String from, String to, Object fallback) { config.set(to, config.get(from, fallback)); }
    private void copyLocation(String old, String modern) { String source = "arena." + old; String dest = "arenas.principal." + modern; for (String key : List.of("world","x","y","z","yaw","pitch")) if (config.contains(source+"."+key)) config.set(dest+"."+key, config.get(source+"."+key)); }
    public Collection<ArenaDefinition> all() { return Collections.unmodifiableCollection(arenas.values()); }
    public ArenaDefinition get(String id) { return id == null ? null : arenas.get(id.toLowerCase(Locale.ROOT)); }
    public boolean create(String id) { if (id == null || id.isBlank() || arenas.containsKey(id.toLowerCase(Locale.ROOT))) return false; String key = id.toLowerCase(Locale.ROOT); config.set("arenas."+key+".enabled", true); config.set("arenas."+key+".min-players", 2); config.set("arenas."+key+".max-players", 16); config.set("arenas."+key+".hunters", 1); config.set("arenas."+key+".rounds", 3); config.set("arenas."+key+".preparation-seconds", 30); config.set("arenas."+key+".round-seconds", 600); config.set("arenas."+key+".between-rounds-seconds", 8); config.set("arenas."+key+".hider-hits", 5); config.set("arenas."+key+".mechanics.translocation", false); config.set("arenas."+key+".mechanics.fireworks", false); config.set("arenas."+key+".mechanics.catch-by-damage", false); config.set("arenas."+key+".mechanics.disable-locator-bar", true); plugin.saveConfig(); load(); return true; }
    public boolean remove(String id) { ArenaDefinition a = get(id); if (a == null || plugin.games().isActive(a.id())) return false; config.set("arenas."+a.id(), null); plugin.saveConfig(); load(); return true; }
}
