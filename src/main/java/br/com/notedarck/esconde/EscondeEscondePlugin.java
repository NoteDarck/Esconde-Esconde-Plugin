package br.com.notedarck.esconde;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;

public final class EscondeEscondePlugin extends JavaPlugin implements Listener {
    private ArenaManager arenas; private GameManager games;
    @Override public void onEnable(){saveDefaultConfig();arenas=new ArenaManager(this);games=new GameManager(this);EscondeCommand c=new EscondeCommand(this);getCommand("esconde").setExecutor(c);getCommand("esconde").setTabCompleter(c);getServer().getPluginManager().registerEvents(this,this);getLogger().info("Esconde-Esconde 1.2.0 ativado com arenas multiplas.");}
    public ArenaManager arenas(){return arenas;} public GameManager games(){return games;}
    public void reloadAll(){reloadConfig();arenas.load();games.reload();}
    public String message(String key, Map<String,String> vars){String text=getConfig().getString("messages."+key,key);for(Map.Entry<String,String>e:vars.entrySet())text=text.replace("%"+e.getKey()+"%",e.getValue());return color(getConfig().getString("messages.prefix","")+text);}
    public void msg(CommandSender s,String key,Map<String,String>vars){s.sendMessage(message(key,vars));}
    public void broadcast(String text){getServer().broadcastMessage(color(text));}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
    @EventHandler public void quit(PlayerQuitEvent e){games.onQuit(e.getPlayer());}
    @EventHandler public void damage(EntityDamageByEntityEvent e){if(!(e.getEntity() instanceof Player victim)||!(e.getDamager() instanceof Player attacker))return;ArenaGame g=games.gameOf(victim.getUniqueId());if(g==null)return;if(g.role(attacker.getUniqueId())==ArenaGame.Role.HUNTER&&g.role(victim.getUniqueId())==ArenaGame.Role.HIDER&&g.state()==ArenaGame.State.HUNTING){e.setCancelled(true);g.catchPlayer(attacker,victim);}else if(g.state()!=ArenaGame.State.WAITING)e.setCancelled(true);}
    @EventHandler public void death(PlayerDeathEvent e){ArenaGame g=games.gameOf(e.getEntity().getUniqueId());if(g!=null&&g.role(e.getEntity().getUniqueId())==ArenaGame.Role.HIDER){e.setKeepInventory(true);e.getDrops().clear();e.setCancelled(true);g.onDeath(e.getEntity());}}
    @EventHandler public void interact(PlayerInteractEvent e){if(e.getItem()==null||e.getItem().getType()!=Material.ENDER_PEARL)return;ArenaGame g=games.gameOf(e.getPlayer().getUniqueId());if(g!=null){e.setCancelled(true);g.translocate(e.getPlayer());}}
    @EventHandler public void move(PlayerMoveEvent e){ArenaGame g=games.gameOf(e.getPlayer().getUniqueId());if(g==null)return;if(g.state()==ArenaGame.State.HIDING&&g.role(e.getPlayer().getUniqueId())==ArenaGame.Role.HUNTER&&e.getTo()!=null&&(e.getFrom().getX()!=e.getTo().getX()||e.getFrom().getZ()!=e.getTo().getZ()))e.setTo(e.getFrom());if((g.state()==ArenaGame.State.HIDING||g.state()==ArenaGame.State.HUNTING)&&g.role(e.getPlayer().getUniqueId())==ArenaGame.Role.HIDER&&e.getTo()!=null&&!g.arenaInside(e.getTo()))e.setTo(e.getFrom());}
    @EventHandler public void item(PlayerInteractEvent e){if(e.getItem()==null||e.getItem().getType()!=Material.FIREWORK_ROCKET)return;ArenaGame g=games.gameOf(e.getPlayer().getUniqueId());if(g!=null&&g.state()==ArenaGame.State.HUNTING&&g.role(e.getPlayer().getUniqueId())==ArenaGame.Role.HIDER){e.getPlayer().getWorld().spawn(e.getPlayer().getLocation(),org.bukkit.entity.Firework.class);}}
}
