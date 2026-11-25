package br.com.notedarck.esconde;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.stream.Collectors;

public final class GameManager {
    public enum State { WAITING, HIDING, PLAYING, ENDING }
    public enum Role { HIDER, HUNTER, SPECTATOR }
    private final EscondeEscondePlugin plugin;
    private final Set<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Map<UUID, SavedPlayer> saved = new HashMap<>();
    private State state = State.WAITING;
    private int remaining;
    private BukkitTask task;
    private final Random random = new Random();

    public GameManager(EscondeEscondePlugin plugin) { this.plugin = plugin; }
    public State state() { return state; }
    public int players() { return queue.size(); }
    public boolean isPlaying(UUID id) { return roles.containsKey(id); }
    public Role role(UUID id) { return roles.get(id); }
    public Set<UUID> queue() { return Collections.unmodifiableSet(queue); }

    public boolean join(Player player) {
        if (state != State.WAITING || !plugin.arena().ready() || queue.size() >= plugin.arena().maxPlayers()) return false;
        if (!queue.add(player.getUniqueId())) return false;
        saved.putIfAbsent(player.getUniqueId(), new SavedPlayer(player));
        if (plugin.arena().clearOnJoin()) player.getInventory().clear();
        player.teleport(plugin.arena().get("lobby"));
        plugin.msg(player, "joined", Map.of("players", String.valueOf(queue.size())));
        return true;
    }

    public boolean leave(Player player) {
        UUID id = player.getUniqueId();
        if (!queue.remove(id) && !roles.containsKey(id)) return false;
        roles.remove(id);
        restore(player);
        player.teleport(plugin.arena().get("lobby"));
        plugin.msg(player, "left", Map.of());
        if (state != State.WAITING && hiders() == 0) finish(true);
        return true;
    }

    public void start() {
        if (state != State.WAITING || queue.size() < plugin.arena().minPlayers() || !plugin.arena().ready()) return;
        state = State.HIDING;
        List<UUID> ids = new ArrayList<>(queue);
        Collections.shuffle(ids, random);
        int hunters = Math.max(plugin.arena().minHunters(), Math.max(1, ids.size() / 5));
        hunters = Math.min(hunters, ids.size() - 1);
        for (int i = 0; i < ids.size(); i++) roles.put(ids.get(i), i < hunters ? Role.HUNTER : Role.HIDER);
        for (UUID id : ids) {
            Player p = Bukkit.getPlayer(id); if (p == null) continue;
            prepare(p, roles.get(id));
            p.teleport(plugin.arena().get(roles.get(id) == Role.HUNTER ? "hunter-spawn" : "hider-spawn"));
        }
        remaining = plugin.arena().hideSeconds();
        broadcast("started", Map.of("time", String.valueOf(remaining)));
        tick();
    }

    public void stop() { if (state != State.WAITING) { broadcast("stopped", Map.of()); finish(false); } }

    private void tick() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateBoards();
            if (state == State.HIDING && remaining <= 0) {
                state = State.PLAYING; remaining = plugin.arena().gameSeconds();
                for (UUID id : roles.keySet()) { Player p = Bukkit.getPlayer(id); if (p != null && roles.get(id) == Role.HUNTER) { p.removePotionEffect(PotionEffectType.BLINDNESS); p.sendTitle("§cPEGADORES LIBERADOS", "§fEncontre todos!", 5, 30, 5); } }
                plugin.broadcast("§cOs pegadores foram liberados!");
            } else if (state == State.PLAYING && (remaining <= 0 || hiders() == 0)) finish(hiders() == 0);
            else if (state == State.ENDING && remaining <= 0) finish(false);
            else remaining--;
        }, 0L, 20L);
    }

    public void catchPlayer(Player hunter, Player hider) {
        if (state != State.PLAYING || role(hunter.getUniqueId()) != Role.HUNTER || role(hider.getUniqueId()) != Role.HIDER) return;
        roles.put(hider.getUniqueId(), Role.HUNTER);
        prepare(hider, Role.HUNTER); hider.teleport(plugin.arena().get("hunter-spawn"));
        plugin.broadcast(plugin.message("caught", Map.of("player", hider.getName())));
        if (hiders() == 0) finish(true);
    }

    private void finish(boolean huntersWon) {
        if (task != null) { task.cancel(); task = null; }
        if (state == State.WAITING) return;
        plugin.broadcast(plugin.message(huntersWon ? "hunters-win" : "hiders-win", Map.of()));
        state = State.ENDING; remaining = plugin.arena().endSeconds();
        for (UUID id : roles.keySet()) { Player p = Bukkit.getPlayer(id); if (p != null) { p.sendTitle(huntersWon ? "§cPEGADORES VENCERAM" : "§aESCONDIDOS VENCERAM", "", 5, 50, 10); } }
        Bukkit.getScheduler().runTaskLater(plugin, () -> { for (UUID id : new ArrayList<>(roles.keySet())) { Player p = Bukkit.getPlayer(id); if (p != null) restore(p); } roles.clear(); queue.clear(); state = State.WAITING; updateBoards(); }, Math.max(1, plugin.arena().endSeconds()) * 20L);
    }

    private void prepare(Player p, Role role) {
        p.getInventory().clear(); p.setGameMode(GameMode.ADVENTURE); p.setHealth(p.getMaxHealth()); p.setFoodLevel(20); p.setFireTicks(0);
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        if (role == Role.HUNTER) { p.getInventory().addItem(item(Material.IRON_SWORD, "§cEspada do Pegador"), item(Material.COMPASS, "§eBússola dos Escondidos")); p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * plugin.arena().hideSeconds(), 1, false, false)); }
        else if (role == Role.HIDER && plugin.arena().leatherArmor()) { p.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET)); p.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE)); p.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS)); p.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS)); }
    }
    private ItemStack item(Material material, String name) { ItemStack i = new ItemStack(material); ItemMeta m = i.getItemMeta(); m.setDisplayName(name); i.setItemMeta(m); return i; }
    private int hiders() { return (int) roles.values().stream().filter(r -> r == Role.HIDER).count(); }
    private int hunters() { return (int) roles.values().stream().filter(r -> r == Role.HUNTER).count(); }

    public void updateBoards() {
        ScoreboardManager sm = Bukkit.getScoreboardManager(); if (sm == null) return;
        for (UUID id : new ArrayList<>(queue)) { Player p = Bukkit.getPlayer(id); if (p == null) continue; Scoreboard board = sm.getNewScoreboard(); Objective o = board.registerNewObjective("esconde", "dummy", "§6§lESCONDE"); o.setDisplaySlot(DisplaySlot.SIDEBAR); int line = 8; o.getScore("§7----------------").setScore(line--); o.getScore("§fEstado: §e" + stateName()).setScore(line--); o.getScore("§fTempo: §e" + format(remaining)).setScore(line--); o.getScore("§cPegadores: §f" + hunters()).setScore(line--); o.getScore("§aEscondidos: §f" + hiders()).setScore(line--); o.getScore("§7Jogadores: §f" + queue.size()).setScore(line--); o.getScore("§7----------------").setScore(line--); p.setScoreboard(board); }
    }
    private String stateName() { return switch (state) { case WAITING -> "Aguardando"; case HIDING -> "Esconder"; case PLAYING -> "Valendo"; case ENDING -> "Finalizando"; }; }
    private String format(int seconds) { return String.format("%02d:%02d", Math.max(0, seconds) / 60, Math.max(0, seconds) % 60); }
    private void broadcast(String key, Map<String,String> vars) { plugin.broadcast(plugin.message(key, vars)); }
    private void restore(Player p) { SavedPlayer s = saved.remove(p.getUniqueId()); if (s != null && plugin.arena().restorePlayers()) s.restore(p); else { p.getInventory().clear(); p.setGameMode(GameMode.SURVIVAL); } p.removePotionEffect(PotionEffectType.BLINDNESS); p.teleport(plugin.arena().get("lobby")); }
    public void onQuit(Player p) {
        if (state == State.WAITING) { queue.remove(p.getUniqueId()); saved.remove(p.getUniqueId()); return; }
        roles.remove(p.getUniqueId()); queue.remove(p.getUniqueId()); saved.remove(p.getUniqueId());
        if (hiders() == 0) finish(true);
    }

    private static final class SavedPlayer {
        private final Location location; private final ItemStack[] contents, armor; private final GameMode mode; private final int food, level; private final float exp;
        SavedPlayer(Player p) { location = p.getLocation(); contents = p.getInventory().getContents().clone(); armor = p.getInventory().getArmorContents().clone(); mode = p.getGameMode(); food = p.getFoodLevel(); level = p.getLevel(); exp = p.getExp(); }
        void restore(Player p) { p.getInventory().setContents(contents); p.getInventory().setArmorContents(armor); p.setGameMode(mode); p.setFoodLevel(food); p.setLevel(level); p.setExp(exp); p.setFireTicks(0); }
    }
}
