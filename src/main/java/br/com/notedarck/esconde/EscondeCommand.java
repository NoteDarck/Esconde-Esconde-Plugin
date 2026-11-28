package br.com.notedarck.esconde;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class EscondeCommand implements CommandExecutor, TabCompleter {
    private final EscondeEscondePlugin plugin;
    EscondeCommand(EscondeEscondePlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("ajuda")) {
            help(sender); return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("entrar") || sub.equals("join")) return enter(sender, args);
        if (sub.equals("sair") || sub.equals("leave")) {
            Player player = player(sender);
            if (player != null && !plugin.games().leave(player)) sender.sendMessage("§cVocê não está em uma arena.");
            return true;
        }
        if (sub.equals("status")) {
            if (args.length > 1) {
                ArenaGame game = plugin.games().game(args[1]);
                if (game == null) { plugin.msg(sender, "no-arena", Map.of()); return true; }
                sender.sendMessage("§6" + game.id() + " §7| Estado: §f" + game.state() + " §7| jogadores: §f" + game.players()
                        + " §7| espectadores: §f" + game.spectators() + " §7| rodada: §f" + game.hiders() + " escondidos / " + game.hunters() + " procuradores");
            } else for (ArenaGame game : plugin.games().all())
                sender.sendMessage("§6" + game.id() + " §7| §f" + game.state() + " §7| jogadores: §f" + game.players() + " §7| espectadores: §f" + game.spectators());
            return true;
        }
        if (sub.equals("arena") || sub.equals("mapa")) return arenaCommand(sender, args);
        if (!sender.hasPermission("esconde.admin")) { plugin.msg(sender, "no-permission", Map.of()); return true; }
        if (sub.equals("iniciar") || sub.equals("start")) {
            if (!sender.hasPermission("esconde.admin.start")) { plugin.msg(sender, "no-permission", Map.of()); return true; }
            if (args.length < 2) { sender.sendMessage("§cUso: /esconde iniciar <arena>"); return true; }
            ArenaGame game = plugin.games().game(args[1]);
            if (game == null || game.players() < gDef(game).minPlayers()) { plugin.msg(sender, "no-arena", Map.of()); return true; }
            plugin.games().start(args[1]); return true;
        }
        if (sub.equals("parar") || sub.equals("stop")) {
            if (!sender.hasPermission("esconde.admin.stop")) { plugin.msg(sender, "no-permission", Map.of()); return true; }
            if (args.length < 2) { sender.sendMessage("§cUso: /esconde parar <arena>"); return true; }
            plugin.games().stop(args[1]); return true;
        }
        if (sub.equals("reload")) {
            if (plugin.games().anyActive()) { sender.sendMessage("§cPare as partidas antes de recarregar."); return true; }
            plugin.reloadAll(); sender.sendMessage("§aConfiguração e arenas recarregadas."); return true;
        }
        return help(sender);
    }

    private boolean enter(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) return true;
        if (!player.hasPermission("esconde.join")) { plugin.msg(sender, "no-permission", Map.of()); return true; }
        boolean explicit = args.length > 1 && !args[1].equalsIgnoreCase("sorteado") && !args[1].equalsIgnoreCase("random");
        String id = explicit ? args[1] : bestArena();
        ArenaDefinition definition = id == null ? null : plugin.arenas().get(id);
        if (definition == null) { plugin.msg(sender, "no-arena", Map.of()); return true; }
        if (!definition.ready()) {
            plugin.msg(sender, "arena-not-ready", Map.of("arena", definition.id(), "missing", String.join(", ", definition.missingPoints())));
            return true;
        }

        GameManager.EntryResult result = plugin.games().enterOrSwitch(player, id);
        switch (result) {
            case JOINED -> plugin.msg(sender, "joined", Map.of("arena", definition.id(), "players", String.valueOf(plugin.games().game(id).players())));
            case SPECTATOR -> sender.sendMessage("§aVocê entrou como espectador da arena §f" + definition.id() + "§a.");
            case SAME_ARENA -> sender.sendMessage("§eVocê já está na arena §f" + definition.id() + "§e.");
            case BUSY -> sender.sendMessage("§eNão foi possível trocar para §f" + definition.id() + "§e: arena cheia, limite de espectadores atingido ou outra entrada está em andamento.");
            case INVALID -> plugin.msg(sender, "no-arena", Map.of());
        }
        return true;
    }

    private boolean arenaCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("esconde.admin.maps")) { plugin.msg(sender, "no-permission", Map.of()); return true; }
        if (args.length < 2) { sender.sendMessage("§e/esconde arena listar|criar|remover|info|set"); return true; }
        String op = args[1].toLowerCase(Locale.ROOT);
        if (op.equals("listar")) {
            for (ArenaDefinition definition : plugin.arenas().all()) sender.sendMessage("§6" + definition.id() + " §7| habilitada: " + definition.enabled() + " §7| pronta: " + definition.ready());
            return true;
        }
        if (op.equals("criar") && args.length >= 3) {
            if (plugin.arenas().create(args[2])) { plugin.games().syncArenas(); plugin.msg(sender, "created", Map.of("arena", args[2])); }
            else sender.sendMessage("§cEssa arena já existe ou o nome é inválido.");
            return true;
        }
        if (op.equals("remover") && args.length >= 3) {
            if (plugin.arenas().remove(args[2])) { plugin.games().syncArenas(); plugin.msg(sender, "removed", Map.of("arena", args[2])); }
            else sender.sendMessage("§cNão é possível remover: arena inexistente ou em funcionamento.");
            return true;
        }
        if (op.equals("info") && args.length >= 3) {
            ArenaDefinition definition = plugin.arenas().get(args[2]);
            if (definition == null) { plugin.msg(sender, "no-arena", Map.of()); return true; }
            sender.sendMessage("§6Arena §f" + definition.id() + " §7| pronta: " + definition.ready() + " | jogadores: " + definition.minPlayers() + "-" + definition.maxPlayers() + " | rodadas: " + definition.rounds() + " | duração: " + definition.roundSeconds() + "s");
            return true;
        }
        if (op.equals("set") && args.length >= 4) {
            Player player = player(sender); ArenaDefinition definition = plugin.arenas().get(args[2]);
            if (player == null || definition == null) { plugin.msg(sender, "no-arena", Map.of()); return true; }
            String key = switch (args[3].toLowerCase(Locale.ROOT)) {
                case "lobby" -> "lobby";
                case "hider", "escondedor", "escondidos" -> "hider-spawn";
                case "hunter", "procurador", "procuradores" -> "hunter-spawn";
                case "spectator", "espectador" -> "spectator-spawn";
                case "exit", "saida", "spawn" -> "exit";
                case "pos1" -> "pos1";
                case "pos2" -> "pos2";
                default -> null;
            };
            if (key == null) { sender.sendMessage("§cPonto inválido: lobby, hider, hunter, spectator, exit, pos1 ou pos2."); return true; }
            definition.setLocation(key, player.getLocation()); plugin.saveConfig(); sender.sendMessage("§aPonto salvo na arena §f" + definition.id() + "§a."); return true;
        }
        sender.sendMessage("§cUso: /esconde arena listar|criar <nome>|remover <nome>|info <nome>|set <nome> <ponto>");
        return true;
    }

    private ArenaDefinition gDef(ArenaGame game) { return plugin.arenas().get(game.id()); }
    private String bestArena() {
        List<ArenaDefinition> choices = plugin.arenas().all().stream().filter(d -> d.enabled() && d.ready() && plugin.games().game(d.id()) != null && plugin.games().game(d.id()).canJoin()).toList();
        if (choices.isEmpty()) {
            List<ArenaDefinition> spectatorChoices = plugin.arenas().all().stream().filter(d -> d.enabled() && d.ready() && plugin.games().game(d.id()) != null && plugin.games().game(d.id()).canJoinSpectator()).toList();
            if (spectatorChoices.isEmpty()) return null;
            return spectatorChoices.get(ThreadLocalRandom.current().nextInt(spectatorChoices.size())).id();
        }
        List<ArenaDefinition> occupied = choices.stream().filter(d -> plugin.games().game(d.id()).players() > 0).toList();
        if (!occupied.isEmpty()) choices = occupied;
        int selectedCount = choices.stream().mapToInt(d -> plugin.games().game(d.id()).players()).max().orElse(0);
        List<ArenaDefinition> preferred = choices.stream().filter(d -> plugin.games().game(d.id()).players() == selectedCount).toList();
        return preferred.get(ThreadLocalRandom.current().nextInt(preferred.size())).id();
    }

    private Player player(CommandSender sender) { if (sender instanceof Player player) return player; sender.sendMessage("§cEste comando precisa ser executado por um jogador."); return null; }
    private boolean help(CommandSender sender) {
        sender.sendMessage("§6§lEsconde-Esconde §7| comandos:");
        sender.sendMessage("§f/esconde entrar [arena|sorteado] §7- entrar ou trocar de arena");
        sender.sendMessage("§f/esconde sair §7- sair da arena atual");
        sender.sendMessage("§f/esconde status [arena] §7- ver partidas");
        sender.sendMessage("§f/esconde ajuda §7- mostrar esta ajuda");
        if (sender.hasPermission("esconde.admin")) { sender.sendMessage("§e/esconde iniciar <arena> §7| §e/esconde parar <arena>"); sender.sendMessage("§e/esconde arena listar|criar|remover|info|set <arena> <ponto>"); sender.sendMessage("§e/esconde reload"); }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = List.of("entrar", "sorteado", "sair", "status", "ajuda", "arena", "iniciar", "parar", "reload");
        if (args.length == 1) return subs.stream().filter(x -> x.startsWith(args[0].toLowerCase())).toList();
        if (args[0].equalsIgnoreCase("arena")) {
            if (args.length == 2) return List.of("listar", "criar", "remover", "info", "set");
            if (args.length == 3 && List.of("remover", "info", "set").contains(args[1].toLowerCase())) return plugin.arenas().all().stream().map(ArenaDefinition::id).toList();
            if (args.length == 4 && args[1].equalsIgnoreCase("set")) return List.of("lobby", "hider", "hunter", "spectator", "exit", "pos1", "pos2");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("entrar") || args[0].equalsIgnoreCase("iniciar") || args[0].equalsIgnoreCase("parar") || args[0].equalsIgnoreCase("status"))) return plugin.arenas().all().stream().map(ArenaDefinition::id).toList();
        return List.of();
    }
}
