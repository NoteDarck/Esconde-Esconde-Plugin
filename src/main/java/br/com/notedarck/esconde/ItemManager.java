package br.com.notedarck.esconde;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ItemManager {
    private final EscondeEscondePlugin plugin;
    private final NamespacedKey idKey;
    private FileConfiguration config;

    ItemManager(EscondeEscondePlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "esconde-item");
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) plugin.saveResource("items.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
        FileConfiguration defaults = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(plugin.getResource("items.yml")));
        mergeMissing(config, defaults, "items");
        migrateHunterWeapon();
        try { config.save(file); } catch (java.io.IOException ex) { plugin.getLogger().warning("Não foi possível salvar items.yml: " + ex.getMessage()); }
    }

    private void migrateHunterWeapon() {
        String base = "items.hunter-weapon.enchantments";
        if (config.contains(base + ".KNOCKBACK") && !config.contains(base + ".SHARPNESS")) {
            config.set(base + ".KNOCKBACK", null);
            config.set(base + ".SHARPNESS", 5);
        }
    }

    private void mergeMissing(FileConfiguration target, FileConfiguration defaults, String path) {
        ConfigurationSection section = defaults.getConfigurationSection(path);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String child = path + "." + key;
            if (defaults.isConfigurationSection(child)) mergeMissing(target, defaults, child);
            else if (!target.contains(child)) target.set(child, defaults.get(child));
        }
    }

    public boolean enabled(String id) { return config.getBoolean("items." + id + ".enabled", false); }
    public int slot(String id) { return Math.max(0, Math.min(35, config.getInt("items." + id + ".slot", -1))); }
    public boolean allowMove(String id) { return config.getBoolean("items." + id + ".allow-move", true); }
    public boolean allowDrop(String id) { return config.getBoolean("items." + id + ".allow-drop", true); }
    public List<String> actions(String id) { return config.getStringList("items." + id + ".actions"); }

    public ItemStack create(String id) {
        if (!enabled(id)) return null;
        String path = "items." + id;
        Material material = Material.matchMaterial(config.getString(path + ".material", "STONE"));
        if (material == null) return null;
        int amount = Math.max(1, Math.min(material.getMaxStackSize(), config.getInt(path + ".amount", 1)));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (meta instanceof LeatherArmorMeta leather && config.contains(path + ".color")) {
            String[] rgb = config.getString(path + ".color", "255,255,255").split(",");
            if (rgb.length == 3) leather.setColor(org.bukkit.Color.fromRGB(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim())));
        }
        String name = config.getString(path + ".name", config.getString(path + ".display_name"));
        if (name != null) meta.setDisplayName(color(name));
        List<String> lore = config.getStringList(path + ".lore");
        if (!lore.isEmpty()) meta.setLore(lore.stream().map(this::color).toList());
        ConfigurationSection enchantments = config.getConfigurationSection(path + ".enchantments");
        if (enchantments != null) for (String key : enchantments.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByName(key.toUpperCase(Locale.ROOT));
            if (enchantment != null) meta.addEnchant(enchantment, enchantments.getInt(key, 1), true);
        }
        if (config.getBoolean(path + ".glow", false)) meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    public void give(Player player, String id) {
        ItemStack item = create(id);
        if (item == null) return;
        int slot = slot(id);
        if (slot >= 0) player.getInventory().setItem(slot, item);
        else player.getInventory().addItem(item);
    }

    public boolean matches(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) return false;
        String value = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        return id.equals(value);
    }

    public String id(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public List<String> configuredIds() {
        ConfigurationSection section = config.getConfigurationSection("items");
        return section == null ? Collections.emptyList() : new ArrayList<>(section.getKeys(false));
    }

    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text); }
}
