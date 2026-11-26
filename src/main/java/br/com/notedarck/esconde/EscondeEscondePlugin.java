package br.com.notedarck.esconde;

import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class EscondeEscondePlugin extends JavaPlugin implements Listener {
    private ArenaManager arenas;
    private GameManager games;
    private ItemManager items;

    @Override public void onEnable() {
        saveDefaultConfig();
        items = new ItemManager(this);
        disableLocatorBars();
        arenas = new ArenaManager(this);
        games = new GameManager(this);
        EscondeCommand c = new EscondeCommand(this);
        getCommand("esconde").setExecutor(c);
        getCommand("esconde").setTabCompleter(c);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Esconde-Esconde 1.3.0 ativado com items.yml.");
    }

    private void disableLocatorBars() { for (World world : getServer().getWorlds()) world.setGameRule(GameRule.LOCATOR_BAR, false); }
    public ArenaManager arenas() { return arenas; }
    public GameManager games() { return games; }
    public ItemManager items() { return items; }

    public void reloadAll() {
        reloadConfig();
        items.reload();
        disableLocatorBars();
        arenas.load();
        games.reload();
    }

    public String message(String key, Map<String, String> vars) {
        String text = getConfig().getString("messages." + key, key);
        for (Map.Entry<String, String> e : vars.entrySet()) text = text.replace("%" + e.getKey() + "%", e.getValue());
        return color(getConfig().getString("messages.prefix", "") + text);
    }
    public void msg(CommandSender s, String key, Map<String, String> vars) { s.sendMessage(message(key, vars)); }
    public void broadcast(String text) { getServer().broadcastMessage(color(text)); }
    public String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    public void runItemActions(Player player, String itemId) {
        ArenaGame game = games.gameOf(player.getUniqueId());
        for (String raw : items.actions(itemId)) {
            String action = raw.trim();
            String upper = action.toUpperCase();
            if (upper.equals("[LEAVE]")) {
                games.leave(player);
            } else if (upper.equals("[RETURN]")) {
                if (game != null) game.returnToExit(player);
            } else if (upper.startsWith("[COMMAND] ")) {
                dispatchPlayerCommand(player, replacePlaceholders(action.substring(10), player, game));
            } else if (upper.startsWith("[CONSOLE] ")) {
                getServer().dispatchCommand(getServer().getConsoleSender(), replacePlaceholders(action.substring(10), player, game));
            } else if (upper.startsWith("[MESSAGE] ")) {
                player.sendMessage(color(replacePlaceholders(action.substring(10), player, game)));
            }
        }
    }

    private void dispatchPlayerCommand(Player player, String command) {
        String value = command.startsWith("/") ? command.substring(1) : command;
        player.performCommand(value);
    }
    private String replacePlaceholders(String text, Player player, ArenaGame game) {
        return text.replace("%player%", player.getName()).replace("%arena%", game == null ? "" : game.id());
    }

    @EventHandler public void quit(PlayerQuitEvent e) { games.onQuit(e.getPlayer()); }
    @EventHandler public void damage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        ArenaGame g = games.gameOf(victim.getUniqueId());
        if (g == null || g.state() == ArenaGame.State.WAITING) return;
        if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player attacker
                && g.role(attacker.getUniqueId()) != null
                && g.role(victim.getUniqueId()) != null
                && g.state() == ArenaGame.State.HUNTING) {
            // PvP real: o escondedor precisa perder toda a vida para ser capturado.
            return;
        }
        // Durante a preparação e contra entidades/danos inválidos, não há dano.
        e.setCancelled(true);
    }
    @EventHandler public void regain(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player p) { ArenaGame g = games.gameOf(p.getUniqueId()); if (g != null && g.active()) e.setCancelled(true); }
    }
    @EventHandler public void death(PlayerDeathEvent e) {
        ArenaGame g = games.gameOf(e.getEntity().getUniqueId());
        if (g != null && g.role(e.getEntity().getUniqueId()) == ArenaGame.Role.HIDER) { e.setKeepInventory(true); e.getDrops().clear(); e.setDeathMessage(null); g.onDeath(e.getEntity()); }
    }
    @EventHandler public void respawn(PlayerRespawnEvent e) {
        ArenaGame g = games.gameOf(e.getPlayer().getUniqueId());
        if (g != null) g.onRespawn(e.getPlayer(), e);
    }
    @EventHandler public void interact(PlayerInteractEvent e) {
        ArenaGame g = games.gameOf(e.getPlayer().getUniqueId());
        String itemId = items.id(e.getItem());
        if (itemId != null && !items.actions(itemId).isEmpty()) {
            e.setCancelled(true);
            runItemActions(e.getPlayer(), itemId);
            return;
        }
        if (e.getItem() == null || e.getItem().getType() != Material.ENDER_PEARL) return;
        if (g != null) { e.setCancelled(true); g.translocate(e.getPlayer()); }
    }
    @EventHandler public void move(PlayerMoveEvent e) {
        ArenaGame g = games.gameOf(e.getPlayer().getUniqueId()); if (g == null) return;
        if (g.state() == ArenaGame.State.HIDING && g.role(e.getPlayer().getUniqueId()) == ArenaGame.Role.HUNTER && e.getTo() != null
                && (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ())) e.setTo(e.getFrom());
        if ((g.state() == ArenaGame.State.HIDING || g.state() == ArenaGame.State.HUNTING) && g.role(e.getPlayer().getUniqueId()) == ArenaGame.Role.HIDER
                && e.getTo() != null && !g.arenaInside(e.getTo())) e.setTo(e.getFrom());
    }
    @EventHandler public void drop(PlayerDropItemEvent e) {
        String id = items.id(e.getItemDrop().getItemStack());
        if (id != null && !items.allowDrop(id) && games.gameOf(e.getPlayer().getUniqueId()) != null) e.setCancelled(true);
    }
    @EventHandler public void inventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String id = items.id(e.getCurrentItem());
        if (id != null && !items.allowMove(id) && games.gameOf(p.getUniqueId()) != null) e.setCancelled(true);
    }
    @EventHandler public void inventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        for (var item : e.getOldCursor() == null ? List.<org.bukkit.inventory.ItemStack>of() : List.of(e.getOldCursor())) {
            String id = items.id(item);
            if (id != null && !items.allowMove(id) && games.gameOf(p.getUniqueId()) != null) { e.setCancelled(true); return; }
        }
    }
    @EventHandler public void item(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != Material.FIREWORK_ROCKET) return;
        ArenaGame g = games.gameOf(e.getPlayer().getUniqueId());
        if (g != null && g.state() == ArenaGame.State.HUNTING && g.role(e.getPlayer().getUniqueId()) == ArenaGame.Role.HIDER)
            e.getPlayer().getWorld().spawn(e.getPlayer().getLocation(), org.bukkit.entity.Firework.class);
    }
}
