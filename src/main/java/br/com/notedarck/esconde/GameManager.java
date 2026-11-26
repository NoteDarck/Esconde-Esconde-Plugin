package br.com.notedarck.esconde;

import org.bukkit.entity.Player;
import java.util.*;

public final class GameManager {
    private final EscondeEscondePlugin plugin;
    private final Map<String, ArenaGame> games = new LinkedHashMap<>();
    private final Map<UUID, String> membership = new HashMap<>();
    GameManager(EscondeEscondePlugin plugin) { this.plugin = plugin; reload(); }
    public void reload() { membership.clear(); games.clear(); for (ArenaDefinition a : plugin.arenas().all()) games.put(a.id().toLowerCase(Locale.ROOT), new ArenaGame(plugin, a)); }
    public void syncArenas() { for (ArenaDefinition a : plugin.arenas().all()) games.putIfAbsent(a.id().toLowerCase(Locale.ROOT), new ArenaGame(plugin, a)); games.entrySet().removeIf(e -> plugin.arenas().get(e.getKey()) == null && !e.getValue().active()); }
    public ArenaGame game(String id) { return id == null ? null : games.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<ArenaGame> all() { return Collections.unmodifiableCollection(games.values()); }
    public String membership(UUID id) { return membership.get(id); }
    public boolean isActive(String id) { ArenaGame g=game(id); return g != null && g.active(); }
    public boolean join(Player p, String id) { ArenaGame g=game(id); if(g==null||!g.join(p))return false;membership.put(p.getUniqueId(),g.id().toLowerCase(Locale.ROOT));return true; }
    public boolean leave(Player p) { String id=membership.remove(p.getUniqueId()); ArenaGame g=id==null?null:game(id); return g!=null&&g.leave(p); }
    public void start(String id) { ArenaGame g=game(id); if(g!=null)g.start(); }
    public void stop(String id) { ArenaGame g=game(id); if(g!=null)g.stop(); }
    public void onQuit(Player p) { String id=membership.remove(p.getUniqueId()); ArenaGame g=id==null?null:game(id); if(g!=null)g.onQuit(p); }
    public ArenaGame gameOf(UUID id) { String name=membership.get(id); return name==null?null:game(name); }
    public boolean anyActive() { return games.values().stream().anyMatch(ArenaGame::active); }
    void clearMembership(UUID id) { membership.remove(id); }
}
