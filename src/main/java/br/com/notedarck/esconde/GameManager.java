package br.com.notedarck.esconde;

import org.bukkit.entity.Player;

import java.util.*;

/** Coordena as sessões globais; todas as alterações de membership ocorrem na thread principal. */
public final class GameManager {
    private final EscondeEscondePlugin plugin;
    private final Map<String, ArenaGame> games = new LinkedHashMap<>();
    private final Map<UUID, String> membership = new HashMap<>();
    private final Set<UUID> joining = new HashSet<>();

    GameManager(EscondeEscondePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (!membership.isEmpty() || games.values().stream().anyMatch(ArenaGame::active)) {
            plugin.getLogger().warning("Reload ignorado enquanto existem sessões ativas; use /esconde parar antes de recarregar.");
            return;
        }
        membership.clear();
        joining.clear();
        games.clear();
        for (ArenaDefinition arena : plugin.arenas().all()) {
            games.put(key(arena.id()), new ArenaGame(plugin, arena));
        }
    }

    public void syncArenas() {
        for (ArenaDefinition arena : plugin.arenas().all()) {
            games.putIfAbsent(key(arena.id()), new ArenaGame(plugin, arena));
        }
        games.entrySet().removeIf(entry -> plugin.arenas().get(entry.getKey()) == null && !entry.getValue().active());
    }

    public ArenaGame game(String id) { return id == null ? null : games.get(key(id)); }
    public Collection<ArenaGame> all() { return Collections.unmodifiableCollection(games.values()); }
    public String membership(UUID id) { return membership.get(id); }
    public boolean isActive(String id) { ArenaGame g = game(id); return g != null && g.active(); }

    /** Entrada transacional: reserva o jogador antes de consultar/mutacionar a arena. */
    public boolean join(Player player, String id) {
        if (player == null || id == null) return false;
        UUID uuid = player.getUniqueId();
        if (membership.containsKey(uuid) || !joining.add(uuid)) return false;
        try {
            ArenaGame game = game(id);
            if (game == null || !game.canJoin()) return false;
            if (!game.join(player)) return false;
            membership.put(uuid, key(game.id()));
            return true;
        } finally {
            joining.remove(uuid);
        }
    }

    public boolean joinSpectator(Player player, String id) {
        if (player == null || id == null) return false;
        UUID uuid = player.getUniqueId();
        if (membership.containsKey(uuid) || !joining.add(uuid)) return false;
        try {
            ArenaGame game = game(id);
            if (game == null || !game.joinSpectator(player)) return false;
            membership.put(uuid, key(game.id()));
            return true;
        } finally {
            joining.remove(uuid);
        }
    }

    public boolean leave(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        String id = membership.get(uuid);
        ArenaGame game = id == null ? null : game(id);
        if (game == null) return false;
        boolean left = game.contains(uuid) ? game.leave(player) : game.leaveSpectator(player);
        if (!left) return false;
        membership.remove(uuid);
        return true;
    }

    public void start(String id) { ArenaGame g = game(id); if (g != null) g.start(); }
    public void stop(String id) { ArenaGame g = game(id); if (g != null) g.stop(); }

    public void onQuit(Player player) {
        UUID uuid = player.getUniqueId();
        String id = membership.remove(uuid);
        ArenaGame game = id == null ? null : game(id);
        if (game != null) game.onQuit(player);
        joining.remove(uuid);
    }

    public ArenaGame gameOf(UUID id) {
        String name = membership.get(id);
        return name == null ? null : game(name);
    }

    public boolean anyActive() { return games.values().stream().anyMatch(ArenaGame::active); }
    void clearMembership(UUID id) { membership.remove(id); joining.remove(id); }
    public void shutdown() { for (ArenaGame game : new ArrayList<>(games.values())) game.shutdown(); membership.clear(); joining.clear(); }
    private static String key(String id) { return id.toLowerCase(Locale.ROOT); }
}
