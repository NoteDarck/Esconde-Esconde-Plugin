package br.com.notedarck.esconde;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class EscondeCommand implements CommandExecutor, TabCompleter {
    private final EscondeEscondePlugin plugin;
    public EscondeCommand(EscondeEscondePlugin plugin) { this.plugin = plugin; }
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) { help(s); return true; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("entrar") || sub.equals("join")) { Player p = player(s); if (p != null && !plugin.games().join(p)) p.sendMessage("§cNão foi possível entrar: a partida pode estar cheia ou em andamento."); return true; }
        if (sub.equals("sair") || sub.equals("leave")) { Player p = player(s); if (p != null && !plugin.games().leave(p)) p.sendMessage("§cVocê não está na partida."); return true; }
        if (sub.equals("status")) { s.sendMessage("§6Esconde §7| Estado: §f" + plugin.games().state() + " §7| Jogadores: §f" + plugin.games().players()); return true; }
        if (!s.hasPermission("esconde.admin")) { plugin.msg(s, "no-permission", Map.of()); return true; }
        if (sub.equals("iniciar") || sub.equals("start")) { if (!plugin.arena().ready()) { plugin.msg(s,"no-arena",Map.of()); return true; } if (plugin.games().players() < plugin.arena().minPlayers()) { plugin.msg(s,"not-enough",Map.of("min",String.valueOf(plugin.arena().minPlayers()))); return true; } plugin.games().start(); return true; }
        if (sub.equals("parar") || sub.equals("stop")) { plugin.games().stop(); return true; }
        if (sub.equals("reload")) { if (plugin.games().state() != GameManager.State.WAITING) { s.sendMessage("§cPare a partida antes de recarregar."); return true; } plugin.reloadConfig(); plugin.reloadArena(); s.sendMessage("§aConfiguração recarregada."); return true; }
        if (sub.equals("set")) { Player p = player(s); if (p == null || a.length < 2) { s.sendMessage("§cUso: /esconde set <lobby|hider|hunter|spectator>"); return true; } String key = switch (a[1].toLowerCase()) { case "hider", "escondido", "escondidos" -> "hider-spawn"; case "hunter", "pegador", "pegadores" -> "hunter-spawn"; case "spectator", "espectador" -> "spectator-spawn"; case "lobby" -> "lobby"; default -> null; }; if (key == null) { s.sendMessage("§cPonto inválido."); return true; } plugin.arena().set(key, p.getLocation()); plugin.saveConfig(); s.sendMessage("§aPonto §f" + a[1] + " §asalvo."); return true; }
        help(s); return true;
    }
    private void help(CommandSender s) { s.sendMessage("§6§lEsconde-Esconde §7- §f/esconde entrar, sair, status"); if (s.hasPermission("esconde.admin")) s.sendMessage("§7Admin: §f/esconde iniciar | parar | set <ponto> | reload"); }
    private Player player(CommandSender s) { if (s instanceof Player p) return p; s.sendMessage("§cEste comando precisa ser executado por um jogador."); return null; }
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) { if (args.length == 1) return List.of("entrar","sair","status","iniciar","parar","set","reload").stream().filter(x -> x.startsWith(args[0].toLowerCase())).toList(); if (args.length == 2 && args[0].equalsIgnoreCase("set")) return List.of("lobby","hider","hunter","spectator"); return List.of(); }
}
