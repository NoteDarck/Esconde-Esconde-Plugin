package br.com.notedarck.esconde;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public final class ArenaGame {
    public enum State { WAITING, STARTING, ROLE_SELECTION, HIDING, HUNTING, ROUND_ENDING, GAME_ENDING }
    public enum Role { HIDER, HUNTER }
    private final EscondeEscondePlugin plugin;
    private final ArenaDefinition arena;
    private final Set<UUID> players = new LinkedHashSet<>();
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Map<UUID, SavedPlayer> saved = new HashMap<>();
    private final Map<UUID, Integer> translocations = new HashMap<>();
    private final Map<UUID, Integer> hiderHits = new HashMap<>();
    private final BossBar bossBar;
    private State state = State.WAITING;
    private BukkitTask task;
    private int remaining, round, hiderWins, hunterWins;
    private final Random random = new Random();
    ArenaGame(EscondeEscondePlugin plugin, ArenaDefinition arena) { this.plugin = plugin; this.arena = arena; bossBar = Bukkit.createBossBar("§6Esconde-Esconde", BarColor.YELLOW, BarStyle.SOLID); }
    public String id() { return arena.id(); }
    public State state() { return state; }
    public int players() { return players.size(); }
    public boolean active() { return state != State.WAITING; }
    public boolean contains(UUID id) { return players.contains(id); }
    public Role role(UUID id) { return roles.get(id); }
    public boolean arenaInside(Location location) { return arena.inside(location); }
    public int hiders() { return (int) roles.values().stream().filter(r -> r == Role.HIDER).count(); }
    public int hunters() { return (int) roles.values().stream().filter(r -> r == Role.HUNTER).count(); }
    public boolean join(Player p) { if (!arena.ready() || !arena.enabled() || (state != State.WAITING && state != State.STARTING) || players.size() >= arena.maxPlayers() || plugin.games().membership(p.getUniqueId()) != null) return false; players.add(p.getUniqueId()); saved.put(p.getUniqueId(), new SavedPlayer(p)); if (plugin.getConfig().getBoolean("settings.clear-inventory-on-join", true)) p.getInventory().clear(); p.teleport(arena.lobby()); plugin.msg(p, "joined", Map.of("arena", arena.id(), "players", String.valueOf(players.size()))); if (arena.autoStart() && players.size() >= arena.minPlayers()) { state = State.STARTING; remaining = arena.autoStartSeconds(); if (task == null) runTicker(); announceStarting(); } return true; }
    public boolean leave(Player p) { if (!players.remove(p.getUniqueId())) return false; roles.remove(p.getUniqueId()); translocations.remove(p.getUniqueId()); hiderHits.remove(p.getUniqueId()); restore(p); plugin.msg(p, "left", Map.of("arena", arena.id())); if (state == State.STARTING && players.size() < arena.minPlayers()) { state = State.WAITING; if (task != null) { task.cancel(); task = null; } } else if (active() && hiders() == 0) endRound(true); return true; }
    public void start() { if (active() || players.size() < arena.minPlayers() || !arena.ready()) return; state = State.STARTING; remaining = arena.autoStartSeconds(); announceStarting(); runTicker(); }
    public void stop() { if (state == State.WAITING) return; plugin.broadcast(plugin.message("stopped", Map.of("arena", arena.id()))); cleanup(); }
    private void announceStarting() { for (Player p : online()) { p.sendTitle("§6" + arena.id(), "§eComeçando em " + remaining + "s", 0, 25, 5); p.sendActionBar("§eA partida inicia em §f" + remaining + "s§e. Novas entradas reiniciam a contagem."); } }
    private void runTicker() { if (task != null) task.cancel(); task = Bukkit.getScheduler().runTaskTimer(plugin, () -> { updateUi(); switch (state) { case STARTING -> { if (remaining <= 0) beginRound(); else { if (remaining <= 5) announceStarting(); remaining--; } } case HIDING -> { if (remaining <= 0) releaseHunters(); else { importantTimeTitle(); remaining--; } } case HUNTING -> { if (remaining <= 0) endRound(false); else remaining--; } case ROUND_ENDING -> { if (remaining <= 0) beginRound(); else remaining--; } case GAME_ENDING -> { if (remaining <= 0) cleanup(); else remaining--; } default -> { } } }, 0L, 20L); }
    private void beginRound() { state = State.ROLE_SELECTION; round++; roles.clear(); translocations.clear(); hiderHits.clear(); List<UUID> ids = new ArrayList<>(players); ids.removeIf(id -> Bukkit.getPlayer(id) == null); Collections.shuffle(ids, random); int count = Math.min(Math.max(1, arena.hunters()), Math.max(1, ids.size()-1)); for (int i=0;i<ids.size();i++) roles.put(ids.get(i), i < count ? Role.HUNTER : Role.HIDER);
        for (UUID id : ids) if (roles.get(id) == Role.HIDER) hiderHits.put(id, arena.hiderHits()); for (UUID id : ids) { Player p=Bukkit.getPlayer(id); if(p==null) continue; prepare(p, roles.get(id)); p.teleport(roles.get(id)==Role.HUNTER ? arena.hunterSpawn() : arena.hiderSpawn()); p.sendTitle(roles.get(id)==Role.HUNTER ? "§cPROCURADOR" : "§aESCONDEDOR", roles.get(id)==Role.HUNTER ? "§fAguarde a liberação" : "§fVocê tem tempo para se esconder", 5, 35, 8); } state = State.HIDING; remaining = arena.preparationSeconds(); plugin.broadcast("§eArena §f"+arena.id()+" §e| Rodada §f"+round+"§e/§f"+arena.rounds()+" §einiciada."); }
    private void releaseHunters() { state = State.HUNTING; remaining = arena.roundSeconds(); for (Player p: online()) { p.removePotionEffect(PotionEffectType.BLINDNESS); if (role(p.getUniqueId()) == Role.HUNTER) p.sendTitle("§cPROCURADORES LIBERADOS", "§fEncontrem os escondedores!", 5, 35, 8); else p.sendTitle("§aCORRA!", "§fOs procuradores foram liberados", 5, 25, 5); } plugin.broadcast(plugin.message("released", Map.of())); }
    private void importantTimeTitle() { if (remaining == 30 || remaining == 10) for (Player p: online()) p.sendTitle("§e"+remaining+" segundos", "§fContinue escondido", 0, 25, 5); }
    public void onHunterHit(Player hunter, Player hider) {
        if (state != State.HUNTING || role(hunter.getUniqueId()) != Role.HUNTER || role(hider.getUniqueId()) != Role.HIDER) return;
        if (arena.catchByDamage()) { catchPlayer(hunter, hider); return; }
        int totalHits = arena.hiderHits();
        int remainingHits = hiderHits.merge(hider.getUniqueId(), -1, Integer::sum);
        double visibleHealth = hider.getMaxHealth() * Math.max(0, remainingHits) / (double) totalHits;
        if (remainingHits > 0) {
            hider.setHealth(Math.max(1.0, Math.min(hider.getMaxHealth(), visibleHealth)));
            hider.sendActionBar("§cVocê foi atingido! §fVida restante: §e" + Math.ceil(visibleHealth / 2.0) + "❤ §7(" + remainingHits + " golpes)");
            hunter.sendActionBar("§eAcerto! §fO escondedor ainda aguenta §c" + remainingHits + " golpes");
            return;
        }
        // O golpe final esvazia a resistência e transforma o escondedor sem abrir uma tela de morte.
        hider.setHealth(1.0);
        catchPlayer(hunter, hider);
    }
    public void catchPlayer(Player hunter, Player hider) { if (state != State.HUNTING || role(hunter.getUniqueId()) != Role.HUNTER || role(hider.getUniqueId()) != Role.HIDER) return; hiderHits.remove(hider.getUniqueId()); roles.put(hider.getUniqueId(), Role.HUNTER); prepare(hider, Role.HUNTER); hider.teleport(arena.hunterSpawn()); firework(hider, Color.RED); plugin.broadcast(plugin.message("caught", Map.of("player", hider.getName()))); if (hiders() == 0) endRound(true); }
    public void onDeath(Player hider) { if (state != State.HUNTING || role(hider.getUniqueId()) != Role.HIDER) return; hiderHits.remove(hider.getUniqueId()); roles.put(hider.getUniqueId(), Role.HUNTER); prepare(hider, Role.HUNTER); hider.teleport(arena.hunterSpawn()); plugin.broadcast(plugin.message("caught", Map.of("player", hider.getName()))); if (hiders() == 0) endRound(true); }
    private void endRound(boolean huntersWon) { if (state == State.ROUND_ENDING || state == State.GAME_ENDING || state == State.WAITING) return; if (huntersWon) hunterWins++; else hiderWins++; state = round >= arena.rounds() ? State.GAME_ENDING : State.ROUND_ENDING; remaining = round >= arena.rounds() ? 8 : arena.betweenRoundsSeconds(); plugin.broadcast(plugin.message(huntersWon ? "hunters-win" : "hiders-win", Map.of("round", String.valueOf(round)))); for (Player p: online()) p.sendTitle(huntersWon ? "§cPROCURADORES VENCERAM" : "§aESCONDEDORES VENCERAM", "§fPlacar " + hunterWins + " x " + hiderWins, 5, 45, 8); if (state == State.ROUND_ENDING) plugin.broadcast(plugin.message("next-round", Map.of("time", String.valueOf(remaining)))); else { boolean hidersWon = hiderWins >= hunterWins; plugin.broadcast(plugin.message(hidersWon ? "game-win-hiders" : "game-win-hunters", Map.of())); } }
    private void prepare(Player p, Role role) { p.getInventory().clear(); p.setGameMode(GameMode.ADVENTURE); p.setHealth(p.getMaxHealth()); p.setFoodLevel(20); p.setFireTicks(0); for (PotionEffect e:p.getActivePotionEffects()) p.removePotionEffect(e.getType()); p.setGlowing(false); if (role == Role.HUNTER) { armor(p, Color.RED); p.getInventory().addItem(enchantedItem(Material.BLAZE_ROD, "§cVara de Blaze do Procurador", Enchantment.KNOCKBACK, 2)); p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*arena.preparationSeconds(), 1, false, false)); } else { armor(p, Color.LIME); p.getInventory().addItem(enchantedItem(Material.STICK, "§aStick do Escondedor", Enchantment.KNOCKBACK, 10)); if (arena.translocation()) p.getInventory().addItem(item(Material.ENDER_PEARL,"§dTranslocação §7(1 uso)")); if (arena.fireworks()) p.getInventory().addItem(fireworkItem()); } }
    private void armor(Player p, Color color) { ItemStack h=colored(Material.LEATHER_HELMET,color), c=colored(Material.LEATHER_CHESTPLATE,color), l=colored(Material.LEATHER_LEGGINGS,color), b=colored(Material.LEATHER_BOOTS,color); p.getInventory().setHelmet(h);p.getInventory().setChestplate(c);p.getInventory().setLeggings(l);p.getInventory().setBoots(b); }
    private ItemStack colored(Material m, Color c) { ItemStack i=new ItemStack(m); if(i.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta){meta.setColor(c);i.setItemMeta(meta);} return i; }
    private ItemStack item(Material m,String name){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);i.setItemMeta(meta);return i;}
    private ItemStack enchantedItem(Material material, String name, Enchantment enchantment, int level) { ItemStack i = item(material, name); i.addUnsafeEnchantment(enchantment, level); return i; }
    private ItemStack fireworkItem(){ItemStack i=new ItemStack(Material.FIREWORK_ROCKET,2);ItemMeta m=i.getItemMeta();m.setDisplayName("§cFogos do Escondedor");i.setItemMeta(m);return i;}
    private void firework(Player p, Color color){if(!arena.fireworks()||p.getWorld()==null)return;Firework f=p.getWorld().spawn(p.getLocation(),Firework.class);FireworkMeta m=f.getFireworkMeta();m.addEffect(FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.BALL_LARGE).flicker(true).trail(true).build());m.setPower(0);f.setFireworkMeta(m);}
    public boolean translocate(Player p){ if(state != State.HIDING && state != State.HUNTING || role(p.getUniqueId()) != Role.HIDER || !arena.translocation() || translocations.getOrDefault(p.getUniqueId(),0) >= 1) return false; Location a=arena.pos1(),b=arena.pos2(); if(a==null||b==null) return false; double x=Math.min(a.getX(),b.getX())+1+random.nextDouble()*Math.max(1,Math.abs(a.getX()-b.getX())-2); double z=Math.min(a.getZ(),b.getZ())+1+random.nextDouble()*Math.max(1,Math.abs(a.getZ()-b.getZ())-2); Location target=new Location(a.getWorld(),x,a.getY()+1,z,p.getLocation().getYaw(),p.getLocation().getPitch()); p.teleport(target); translocations.put(p.getUniqueId(),1); p.getInventory().remove(Material.ENDER_PEARL); p.sendTitle("§dTRANSLOCAÇÃO","§fVocê mudou de esconderijo",2,25,5); firework(p,Color.FUCHSIA); return true; }
    private void updateUi(){ double progress=remaining<=0?0:Math.min(1.0,remaining/(double)Math.max(1, state==State.HIDING?arena.preparationSeconds():arena.roundSeconds())); bossBar.setProgress(progress); bossBar.setTitle("§6"+arena.id()+" §8| §f"+stateName()+" §8| §e"+format(remaining)); for(Player p:online()){if(!bossBar.getPlayers().contains(p))bossBar.addPlayer(p);p.sendActionBar("§e"+format(remaining)+" §8| §aEscondidos: §f"+hiders()+" §8| §cProcuradores: §f"+hunters());ScoreboardManager sm=Bukkit.getScoreboardManager();if(sm==null)continue;Scoreboard board=sm.getNewScoreboard();
        if (arena.hideNametags()) { Team team = board.registerNewTeam("esconde_names"); team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER); for (Player target : online()) team.addEntry(target.getName()); }
        Objective o=board.registerNewObjective("esconde","dummy","§6§l"+arena.id().toUpperCase());o.setDisplaySlot(DisplaySlot.SIDEBAR);int n=8;o.getScore("§7----------------").setScore(n--);o.getScore("§fRodada: §e"+round+"/"+arena.rounds()).setScore(n--);o.getScore("§fTempo: §e"+format(remaining)).setScore(n--);o.getScore("§cProcuradores: §f"+hunters()).setScore(n--);o.getScore("§aEscondidos: §f"+hiders()).setScore(n--);o.getScore("§7Placar: §f"+hunterWins+" x "+hiderWins).setScore(n--);o.getScore("§7----------------").setScore(n--);p.setScoreboard(board);}}
    private String stateName(){return switch(state){case WAITING->"Aguardando";case STARTING->"Iniciando";case ROLE_SELECTION->"Sorteio";case HIDING->"Escondendo";case HUNTING->"Procurando";case ROUND_ENDING->"Próxima rodada";case GAME_ENDING->"Finalizando";};}
    private String format(int s){return String.format("%02d:%02d",Math.max(0,s)/60,Math.max(0,s)%60);}
    private List<Player> online(){List<Player> out=new ArrayList<>();for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null)out.add(p);}return out;}
    public void onQuit(Player p){
        UUID id = p.getUniqueId();
        if (!players.remove(id)) return;
        roles.remove(id); translocations.remove(id); hiderHits.remove(id); saved.remove(id);
        if (state == State.STARTING && players.size() < arena.minPlayers()) {
            state = State.WAITING;
            if (task != null) { task.cancel(); task = null; }
        } else if (state == State.HUNTING && hiders() == 0) {
            endRound(true);
        } else if (players.isEmpty()) {
            cleanup();
        }
    }
    private void restore(Player p){SavedPlayer s=saved.remove(p.getUniqueId());p.setGlowing(false);p.removePotionEffect(PotionEffectType.BLINDNESS);if(s!=null&&plugin.getConfig().getBoolean("settings.restore-players-after-game",true))s.restore(p);else{p.getInventory().clear();p.setGameMode(GameMode.SURVIVAL);p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());}if(arena.lobby()!=null)p.teleport(arena.lobby());}
    private void cleanup(){if(task!=null){task.cancel();task=null;}for(UUID id:new ArrayList<>(players))plugin.games().clearMembership(id);for(Player p:online()){bossBar.removePlayer(p);restore(p);}players.clear();roles.clear();translocations.clear();hiderHits.clear();state=State.WAITING;round=0;hiderWins=0;hunterWins=0;remaining=0;}
    private static final class SavedPlayer{final Location location;final ItemStack[] contents,armor;final GameMode mode;final Scoreboard scoreboard;final int food,level;final float exp;SavedPlayer(Player p){location=p.getLocation();contents=p.getInventory().getContents().clone();armor=p.getInventory().getArmorContents().clone();mode=p.getGameMode();scoreboard=p.getScoreboard();food=p.getFoodLevel();level=p.getLevel();exp=p.getExp();}void restore(Player p){p.getInventory().setContents(contents);p.getInventory().setArmorContents(armor);p.setGameMode(mode);p.setScoreboard(scoreboard == null ? Bukkit.getScoreboardManager().getMainScoreboard() : scoreboard);p.setFoodLevel(food);p.setLevel(level);p.setExp(exp);p.teleport(location);p.setFireTicks(0);}}
}
