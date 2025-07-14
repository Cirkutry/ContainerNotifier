package com;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ContainerNotifier extends JavaPlugin implements Listener {
    
    private String webhookUrl;
    private Map<String, ItemThreshold> itemThresholds;
    private boolean debugMode;
    private int alertCooldown;
    private boolean checkPlayerInventory;
    private NBTHandler nbtHandler;
    private final Map<String, Long> alertCooldowns = new ConcurrentHashMap<>();
    private static class ItemThreshold {
        private final String id;
        private final String item;
        private final int threshold;
        private final ConfigurationSection nbtConfig;
        
        public ItemThreshold(String id, String item, int threshold, ConfigurationSection nbtConfig) {
            this.id = id;
            this.item = item;
            this.threshold = threshold;
            this.nbtConfig = nbtConfig;
        }
        
        public String getId() {
            return id;
        }
        
        public String getItem() {
            return item;
        }
        
        public int getThreshold() {
            return threshold;
        }
        
        public ConfigurationSection getNbtConfig() {
            return nbtConfig;
        }
        
        public boolean hasNbtRequirement() {
            return nbtConfig != null && !nbtConfig.getKeys(false).isEmpty();
        }
    }
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        nbtHandler = new NBTHandler(getLogger(), debugMode);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("containernotifier").setExecutor(this);
        
        getLogger().info("ContainerNotifier plugin enabled!");
        getLogger().info("Monitoring " + itemThresholds.size() + " item types");
        if (debugMode) {
            getLogger().info("Debug mode enabled");
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("ContainerNotifier plugin disabled!");
    }
    
    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        webhookUrl = config.getString("webhook-url", "");
        if (webhookUrl.isEmpty()) {
            getLogger().warning("No webhook URL configured! Alerts will not be sent.");
        }
        debugMode = config.getBoolean("settings.debug", false);
        alertCooldown = config.getInt("settings.alert-cooldown", 300);
        checkPlayerInventory = config.getBoolean("settings.check-player-inventory", false);
        itemThresholds = new HashMap<>();
        if (config.contains("thresholds")) {
            ConfigurationSection thresholdsSection = config.getConfigurationSection("thresholds");
            for (String id : thresholdsSection.getKeys(false)) {
                ConfigurationSection thresholdSection = thresholdsSection.getConfigurationSection(id);
                if (thresholdSection != null) {
                    String item = thresholdSection.getString("item");
                    int threshold = thresholdSection.getInt("threshold");
                    ConfigurationSection nbtConfig = thresholdSection.getConfigurationSection("nbt");
                    
                    if (item != null && threshold > 0) {
                        itemThresholds.put(item, new ItemThreshold(id, item, threshold, nbtConfig));
                        if (debugMode) {
                            getLogger().info("Loaded threshold for item " + item + " (ID: " + id + ") - threshold: " + threshold);
                        }
                    }
                }
            }
        }
        if (nbtHandler != null) {
            nbtHandler = new NBTHandler(getLogger(), debugMode);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("containernotifier")) {
            return false;
        }
        
        if (!sender.hasPermission("containernotifier.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.YELLOW + "ContainerNotifier v" + getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/containernotifier reload - Reload configuration");
            sender.sendMessage(ChatColor.YELLOW + "/containernotifier status - Show plugin status");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "ContainerNotifier configuration reloaded!");
                sender.sendMessage(ChatColor.GREEN + "Monitoring " + itemThresholds.size() + " item types");
                break;
                
            case "status":
                sender.sendMessage(ChatColor.YELLOW + "=== ContainerNotifier Status ===");
                sender.sendMessage(ChatColor.YELLOW + "Webhook URL: " + (webhookUrl.isEmpty() ? ChatColor.RED + "Not configured" : ChatColor.GREEN + "Configured"));
                sender.sendMessage(ChatColor.YELLOW + "Monitored items: " + ChatColor.WHITE + itemThresholds.size());
                sender.sendMessage(ChatColor.YELLOW + "Debug mode: " + (debugMode ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
                sender.sendMessage(ChatColor.YELLOW + "Alert cooldown: " + ChatColor.WHITE + alertCooldown + " seconds");
                sender.sendMessage(ChatColor.YELLOW + "Check player inventory: " + (checkPlayerInventory ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Use /containernotifier or /containernotifier help for help.");
                break;
        }
        
        return true;
    }
    
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!isStorageContainer(holder)) return;
        String location = getLocationString(holder, player);
        
        if (debugMode) {
            getLogger().info("Player " + player.getName() + " opened storage at " + location);
        }
        scanInventory(inventory, player, location, getContainerType(holder));
        if (checkPlayerInventory) {
            scanInventory(player.getInventory(), player, location, "Player Inventory");
        }
    }
    
    private boolean isStorageContainer(InventoryHolder holder) {
        if (holder == null) return false;
        String holderType = holder.getClass().getSimpleName().toLowerCase();
        return holderType.contains("chest") || 
               holderType.contains("shulker") || 
               holderType.contains("barrel") || 
               holderType.contains("hopper") || 
               holderType.contains("dispenser") || 
               holderType.contains("dropper") ||
               holderType.contains("furnace") ||
               holderType.contains("enderchest") ||
               holderType.contains("brewingstand") ||
               holderType.contains("beacon");
    }
    
    private String getContainerType(InventoryHolder holder) {
        if (holder == null) return "Unknown";
        
        String holderType = holder.getClass().getSimpleName().toLowerCase();
        
        if (holderType.contains("chest")) {
            if (holderType.contains("ender")) return "Ender Chest";
            return "Chest";
        } else if (holderType.contains("shulker")) {
            return "Shulker Box";
        } else if (holderType.contains("barrel")) {
            return "Barrel";
        } else if (holderType.contains("hopper")) {
            return "Hopper";
        } else if (holderType.contains("dispenser")) {
            return "Dispenser";
        } else if (holderType.contains("dropper")) {
            return "Dropper";
        } else if (holderType.contains("furnace")) {
            return "Furnace";
        } else if (holderType.contains("brewingstand")) {
            return "Brewing Stand";
        } else if (holderType.contains("beacon")) {
            return "Beacon";
        }
        
        return "Container";
    }
    
    private String getLocationString(InventoryHolder holder, Player player) {
        try {
            if (holder instanceof org.bukkit.block.BlockState) {
                Block block = ((org.bukkit.block.BlockState) holder).getBlock();
                return String.format("%s (X: %d, Y: %d, Z: %d)", 
                    block.getWorld().getName(), 
                    block.getX(), 
                    block.getY(), 
                    block.getZ());
            }
        } catch (Exception e) {
        }
        
        return String.format("%s (X: %d, Y: %d, Z: %d)", 
            player.getWorld().getName(), 
            player.getLocation().getBlockX(), 
            player.getLocation().getBlockY(), 
            player.getLocation().getBlockZ());
    }
    
    private void scanInventory(Inventory inventory, Player player, String location, String inventoryType) {
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<String, ItemStack> itemExamples = new HashMap<>();
        Map<String, ItemThreshold> itemThresholdMap = new HashMap<>();
        
        if (debugMode) {
            getLogger().info("=== Container Contents Debug for " + player.getName() + " ===");
            getLogger().info("Inventory Type: " + inventoryType);
            getLogger().info("Location: " + location);
        }
        
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            String itemKey = getItemKey(item);
            String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? 
                               item.getItemMeta().getDisplayName() : "None";
            
            if (debugMode) {
                getLogger().info("Found item - count: " + item.getAmount() + 
                               ", name: " + displayName + 
                               ", item: " + itemKey);
                getLogger().info("NBT Data: " + nbtHandler.getNBTDebugString(item));
            }
            
            ItemThreshold threshold = itemThresholds.get(itemKey);
            
            if (threshold != null) {
                if (player.hasPermission("containernotifier.bypass.item." + threshold.getId())) {
                    if (debugMode) {
                        getLogger().info("Player " + player.getName() + " has bypass permission for item " + threshold.getId());
                    }
                    continue;
                }
                if (threshold.hasNbtRequirement()) {
                    if (!nbtHandler.matchesNBT(item, threshold.getNbtConfig())) {
                        if (debugMode) {
                            getLogger().info("Item " + itemKey + " does not match NBT requirements");
                        }
                        continue; // NBT doesn't match, skip this item
                    }
                }
                itemCounts.put(itemKey, itemCounts.getOrDefault(itemKey, 0) + item.getAmount());
                itemExamples.put(itemKey, item); // Keep an example for alert
                itemThresholdMap.put(itemKey, threshold);
            }
        }
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String itemKey = entry.getKey();
            int totalAmount = entry.getValue();
            
            ItemThreshold threshold = itemThresholdMap.get(itemKey);
            
            if (threshold != null && totalAmount > threshold.getThreshold()) {
                String cooldownKey = player.getUniqueId().toString() + ":" + threshold.getId();
                long currentTime = System.currentTimeMillis();
                Long lastAlert = alertCooldowns.get(cooldownKey);
                
                if (lastAlert != null && (currentTime - lastAlert) < (alertCooldown * 1000L)) {
                    if (debugMode) {
                        getLogger().info("Alert for " + player.getName() + " - " + itemKey + " (ID: " + threshold.getId() + ") is on cooldown");
                    }
                    continue;
                }
                alertCooldowns.put(cooldownKey, currentTime);
                ItemStack exampleItem = itemExamples.get(itemKey);
                sendAlert(player, exampleItem, location, threshold, inventoryType, totalAmount);
            }
        }
        
        if (debugMode) {
            getLogger().info("=== End Container Contents Debug ===");
        }
    }
    
    private String getItemKey(ItemStack item) {
        try {
            if (item.getType().getKey() != null) {
                return item.getType().getKey().toString();
            }
        } catch (Exception e) {
        }
        return "minecraft:" + item.getType().name().toLowerCase();
    }
    
    private void sendAlert(Player player, ItemStack item, String location, ItemThreshold threshold, String inventoryType, int totalAmount) {
        if (webhookUrl.isEmpty()) {
            if (debugMode) {
                getLogger().warning("No webhook URL configured, cannot send alert");
            }
            return;
        }
    
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "ContainerNotifier-Plugin");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
    
                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : threshold.getItem();
    
                StringBuilder nbtBuilder = new StringBuilder();
                if (threshold.hasNbtRequirement()) {
                    ConfigurationSection nbtConfig = threshold.getNbtConfig();
                    for (String nbtKey : nbtConfig.getKeys(false)) {
                        ConfigurationSection nbtEntry = nbtConfig.getConfigurationSection(nbtKey);
                        if (nbtEntry != null) {
                            String type = nbtEntry.getString("type");
                            String key = nbtEntry.getString("key");
                            String value = nbtEntry.getString("value");
                            nbtBuilder.append("• `").append(key)
                                      .append("` (`").append(type)
                                      .append("`): `").append(value)
                                      .append("`\n");
                        }
                    }
                }

                String isoTimestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .format(new java.util.Date());

                StringBuilder json = new StringBuilder();
                json.append("{\"embeds\":[{")
                    .append("\"title\":\"📦 Storage Alert\",")
                    .append("\"color\":16753920,")
                    .append("\"fields\":[")
                    .append("{\"name\":\"Player\",\"value\":\"`").append(player.getName()).append("`\",\"inline\":true},");

                if (!itemName.equals(threshold.getItem())) {
                    json.append("{\"name\":\"Item\",\"value\":\"`").append(itemName).append("` (`").append(threshold.getItem()).append("`)\",\"inline\":true},");
                } else {
                    json.append("{\"name\":\"Item\",\"value\":\"`").append(itemName).append("`\",\"inline\":true},");
                }

                json.append("{\"name\":\"Amount\",\"value\":\"`").append(totalAmount).append("` (Threshold: `").append(threshold.getThreshold()).append("`)\",\"inline\":true},")
                    .append("{\"name\":\"Item ID\",\"value\":\"`").append(threshold.getId()).append("`\",\"inline\":true},")
                    .append("{\"name\":\"Location\",\"value\":\"`").append(location).append("`\",\"inline\":true},")
                    .append("{\"name\":\"Container Type\",\"value\":\"`").append(inventoryType).append("`\",\"inline\":true}");
    
                if (threshold.hasNbtRequirement()) {
                    json.append(",{\"name\":\"NBT Requirements\",\"value\":\"")
                        .append(nbtBuilder.toString().replace("\"", "\\\"").replace("\n", "\\n"))
                        .append("\",\"inline\":false}");
                }
    
                json.append("],")
                    .append("\"timestamp\":\"").append(isoTimestamp).append("\"")
                    .append("}]}");
    
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
    
                int responseCode = connection.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                    getLogger().info("Alert sent for " + player.getName() + " - " + itemName + " x" + totalAmount + " (ID: " + threshold.getId() + ")");
                } else {
                    getLogger().warning("Failed to send webhook alert. Response code: " + responseCode);
                }
    
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error sending webhook alert", e);
            }
        });
    }
}