package br.com.notedarck.esconde;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;

public final class EscondeEscondePlugin extends JavaPlugin implements Listener {
    private ArenaConfig arena; private GameManager games;
    @Override public void onEnable() { saveDefaultConfig(); arena = new ArenaConfig(getConfig()); games = new GameManager(this); EscondeCommand command = new EscondeCommand(this); getCommand("esconde").setExecutor(command); getCommand("esconde").setTabCompleter(command); getServer().getPluginManager().registerEvents(this, this); getLogger().info("Esconde-Esconde ativado."); }
    public ArenaConfig arena() { return arena; }
    public GameManager games() { return games; }
    public void reloadArena() { arena = new ArenaConfig(getConfig()); }
    public String message(String key, Map<String,String> vars) { String text = getConfig().getString("messages." + key, key); for (Map.Entry<String,String> e : vars.entrySet()) text = text.replace("%" + e.getKey() + "%", e.getValue()); return color(getConfig().getString("messages.prefix", "") + text); }
    public void msg(CommandSender s, String key, Map<String,String> vars) { s.sendMessage(message(key, vars)); }
    public void broadcast(String text) { getServer().broadcastMessage(color(text)); }
    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { games.onQuit(e.getPlayer()); }
    @EventHandler public void onDamage(EntityDamageByEntityEvent e) { if (!(e.getEntity() instanceof Player victim) || !(e.getDamager() instanceof Player attacker)) return; if (!games.isPlaying(victim.getUniqueId())) return; if (games.role(attacker.getUniqueId()) == GameManager.Role.HUNTER && games.role(victim.getUniqueId()) == GameManager.Role.HIDER && arena.catchByDamage()) { e.setCancelled(true); games.catchPlayer(attacker, victim); } else if (games.state() != GameManager.State.WAITING) e.setCancelled(true); }
    @EventHandler public void onMove(org.bukkit.event.player.PlayerMoveEvent e) { if (games.state() == GameManager.State.HIDING && games.role(e.getPlayer().getUniqueId()) == GameManager.Role.HUNTER && e.getTo() != null && (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ())) e.setTo(e.getFrom()); }
}
