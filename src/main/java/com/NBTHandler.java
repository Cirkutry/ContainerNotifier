package com;

import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

public class NBTHandler {
    
    private final Logger logger;
    private final boolean debugMode;
    
    public NBTHandler(Logger logger, boolean debugMode) {
        this.logger = logger;
        this.debugMode = debugMode;
    }
    
    /**
     * Checks if an item matches the NBT requirements specified in the configuration
     * @param item The ItemStack to check
     * @param nbtConfig The NBT configuration section from config.yml
     * @return true if the item matches all NBT requirements, false otherwise
     */
    @SuppressWarnings("deprecation")
    public boolean matchesNBT(ItemStack item, ConfigurationSection nbtConfig) {
        if (item == null || nbtConfig == null) {
            return false;
        }
        
        try {
            NBTItem nbtItem = new NBTItem(item);
            
            for (String nbtKey : nbtConfig.getKeys(false)) {
                ConfigurationSection nbtEntry = nbtConfig.getConfigurationSection(nbtKey);
                if (nbtEntry == null) continue;
                
                if (!checkNBTTag(nbtItem, nbtEntry, nbtKey)) {
                    return false;
                }
            }
            return true;
            
        } catch (Exception e) {
            if (debugMode) {
                logger.warning("Error checking NBT data: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Recursively checks an NBT tag against the configuration
     */
    private boolean checkNBTTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String entryKey) {
        String type = nbtEntry.getString("type");
        String key = nbtEntry.getString("key");
        
        if (type == null || key == null) {
            if (debugMode) {
                logger.warning("Invalid NBT configuration for entry: " + entryKey + " - missing type or key");
            }
            return false;
        }
        if (!nbtCompound.hasTag(key)) {
            if (debugMode) {
                logger.info("Item missing NBT key: " + key);
            }
            return false;
        }
        
        try {
            // eww
            switch (type.toUpperCase()) {
                case "BYTE":
                    return checkByteTag(nbtCompound, nbtEntry, key);
                    
                case "SHORT":
                    return checkShortTag(nbtCompound, nbtEntry, key);
                    
                case "INT":
                case "INTEGER":
                    return checkIntTag(nbtCompound, nbtEntry, key);
                    
                case "LONG":
                    return checkLongTag(nbtCompound, nbtEntry, key);
                    
                case "FLOAT":
                    return checkFloatTag(nbtCompound, nbtEntry, key);
                    
                case "DOUBLE":
                    return checkDoubleTag(nbtCompound, nbtEntry, key);
                    
                case "BYTE_ARRAY":
                    return checkByteArrayTag(nbtCompound, nbtEntry, key);
                    
                case "STRING":
                    return checkStringTag(nbtCompound, nbtEntry, key);
                    
                case "STRING_ARRAY":
                    return checkStringArrayTag(nbtCompound, nbtEntry, key);
                    
                case "COMPOUND":
                    return checkCompoundTag(nbtCompound, nbtEntry, key);
                    
                case "INT_ARRAY":
                    return checkIntArrayTag(nbtCompound, nbtEntry, key);
                    
                default:
                    if (debugMode) {
                        logger.warning("Unsupported NBT type: " + type);
                    }
                    return false;
            }
        } catch (Exception e) {
            if (debugMode) {
                logger.warning("Error checking NBT tag " + key + " of type " + type + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean checkByteTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        String configValue = nbtEntry.getString("value");
        if (configValue == null) return false;
        
        try {
            byte expected = Byte.parseByte(configValue);
            byte actual = nbtCompound.getByte(key);
            boolean matches = expected == actual;
            
            if (debugMode) {
                logger.info("NBT BYTE check - Key: " + key + ", Expected: " + expected + ", Actual: " + actual + ", Matches: " + matches);
            }
            return matches;
        } catch (NumberFormatException e) {
            if (debugMode) {
                logger.warning("Invalid byte value in NBT config: " + configValue);
            }
            return false;
        }
    }
    
    private boolean checkShortTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        String configValue = nbtEntry.getString("value");
        if (configValue == null) return false;
        
        try {
            short expected = Short.parseShort(configValue);
            short actual = nbtCompound.getShort(key);
            boolean matches = expected == actual;
            
            if (debugMode) {
                logger.info("NBT SHORT check - Key: " + key + ", Expected: " + expected + ", Actual: " + actual + ", Matches: " + matches);
            }
            return matches;
        } catch (NumberFormatException e) {
            if (debugMode) {
                logger.warning("Invalid short value in NBT config: " + configValue);
            }
            return false;
        }
    }
    
    private boolean checkIntTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        String configValue = nbtEntry.getString("value");
        if (configValue == null) return false;
        
        try {
            int expected = Integer.parseInt(configValue);
            int actual = nbtCompound.getInteger(key);
            boolean matches = expected == actual;
            
            if (debugMode) {
                logger.info("NBT INT check - Key: " + key + ", Expected: " + expected + ", Actual: " + actual + ", Matches: " + matches);
            }
            return matches;
        } catch (NumberFormatException e) {
            if (debugMode) {
                logger.warning("Invalid integer value in NBT config: " + configValue);
            }
            return false;
        }
    }
    
    private boolean checkLongTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        String configValue = nbtEntry.getString("value");
        if (configValue == null) return false;
        
        try {
            long expected = Long.parseLong(configValue);
            long actual = nbtCompound.getLong(key);
            boolean matches = expected == actual;
            
            if (debugMode) {
                logger.info("NBT LONG check - Key: " + key + ", Expected: " + expected + ", Actual: " + actual + ", Matches: " + matches);
            }
            return matches;
        } catch (NumberFormatException e) {
            if (debugMode) {
                logger.warning("Invalid long value in NBT config: " + configValue);
            }
            return false;
        }
    }
    
    private boolean checkFloatTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        String configValue = nbtEntry.getString("value");
        if (configValue == null) return false;
        
        try {
            float expected = Float.parseFloat(configValue);
            float actual = nbtCompound.getFloat(key);
            boolean matches = Math.abs(expected - actual) < 0.001f;
            
            if (debugMode) {
                logger.info("NBT FLOAT check - Key: " + key + ", Expected: " + expected + ", Actual: " + actual + ", Matches: " + matches);
            }
            return matches;
        } catch (NumberFormatException e) {
            if (debugMode) {
                logger.warning("Invalid float value in NBT config: " + configValue);
            }
            return false;
        }
    }
    
    private boolean checkDoubleTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        String configValue = nbtEntry.getString("value");
        if (configValue == null) return false;
        
        try {
            double expected = Double.parseDouble(configValue);
            double actual = nbtCompound.getDouble(key);
            boolean matches = Math.abs(expected - actual) < 0.001;
            
            if (debugMode) {
                logger.info("NBT DOUBLE check - Key: " + key + ", Expected: " + expected + ", Actual: " + actual + ", Matches: " + matches);
            }
            return matches;
        } catch (NumberFormatException e) {
            if (debugMode) {
                logger.warning("Invalid double value in NBT config: " + configValue);
            }
            return false;
        }
    }
    
    private boolean checkStringTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        String expected = nbtEntry.getString("value");
        if (expected == null) return false;
        
        String actual = nbtCompound.getString(key);
        boolean matches = expected.equals(actual);
        
        if (debugMode) {
            logger.info("NBT STRING check - Key: " + key + ", Expected: " + expected + ", Actual: " + actual + ", Matches: " + matches);
        }
        return matches;
    }
    
    private boolean checkByteArrayTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        List<String> configValues = nbtEntry.getStringList("values");
        if (configValues == null || configValues.isEmpty()) {
            String singleValue = nbtEntry.getString("value");
            if (singleValue == null) return false;
            configValues = java.util.Arrays.asList(singleValue.split(","));
        }
        
        try {
            byte[] expected = new byte[configValues.size()];
            for (int i = 0; i < configValues.size(); i++) {
                expected[i] = Byte.parseByte(configValues.get(i).trim());
            }
            
            byte[] actual = nbtCompound.getByteArray(key);
            boolean matches = java.util.Arrays.equals(expected, actual);
            
            if (debugMode) {
                logger.info("NBT BYTE_ARRAY check - Key: " + key + ", Expected: " + java.util.Arrays.toString(expected) + 
                           ", Actual: " + java.util.Arrays.toString(actual) + ", Matches: " + matches);
            }
            return matches;
        } catch (Exception e) {
            if (debugMode) {
                logger.warning("Invalid byte array values in NBT config: " + configValues);
            }
            return false;
        }
    }
    
    private boolean checkStringArrayTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        List<String> expected = nbtEntry.getStringList("values");
        if (expected == null || expected.isEmpty()) return false;
        
        java.util.List<String> actualList = nbtCompound.getStringList(key);
        boolean matches = expected.equals(actualList);

        if (debugMode) {
            logger.info("NBT STRING_ARRAY check - Key: " + key + ", Expected: " + expected +
                       ", Actual: " + actualList + ", Matches: " + matches);
        }
        return matches;
    }
    
    private boolean checkIntArrayTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        List<String> configValues = nbtEntry.getStringList("values");
        if (configValues == null || configValues.isEmpty()) {
            String singleValue = nbtEntry.getString("value");
            if (singleValue == null) return false;
            configValues = java.util.Arrays.asList(singleValue.split(","));
        }
        
        try {
            int[] expected = new int[configValues.size()];
            for (int i = 0; i < configValues.size(); i++) {
                expected[i] = Integer.parseInt(configValues.get(i).trim());
            }
            
            int[] actual = nbtCompound.getIntArray(key);
            boolean matches = java.util.Arrays.equals(expected, actual);
            
            if (debugMode) {
                logger.info("NBT INT_ARRAY check - Key: " + key + ", Expected: " + java.util.Arrays.toString(expected) + 
                           ", Actual: " + java.util.Arrays.toString(actual) + ", Matches: " + matches);
            }
            return matches;
        } catch (Exception e) {
            if (debugMode) {
                logger.warning("Invalid int array values in NBT config: " + configValues);
            }
            return false;
        }
    }
    
    private boolean checkCompoundTag(NBTCompound nbtCompound, ConfigurationSection nbtEntry, String key) {
        ConfigurationSection children = nbtEntry.getConfigurationSection("children");
        if (children == null) return false;
        
        NBTCompound childCompound = nbtCompound.getCompound(key);
        if (childCompound == null) return false;
        for (String childKey : children.getKeys(false)) {
            ConfigurationSection childEntry = children.getConfigurationSection(childKey);
            if (childEntry == null) continue;
            
            if (!checkNBTTag(childCompound, childEntry, childKey)) {
                return false;
            }
        }
        
        if (debugMode) {
            logger.info("NBT COMPOUND check - Key: " + key + ", All children matched");
        }
        return true;
    }
    
    /**
     * Gets a debug string representation of an item's NBT data
     * @param item The ItemStack to examine
     * @return String representation of NBT data
     */
    @SuppressWarnings("deprecation")
    public String getNBTDebugString(ItemStack item) {
        if (item == null) {
            return "null";
        }
        
        try {
            NBTItem nbtItem = new NBTItem(item);
            StringBuilder sb = new StringBuilder();
            sb.append("NBT Keys: ");
            
            for (String key : nbtItem.getKeys()) {
                sb.append(key).append("=").append(nbtItem.getString(key)).append(", ");
            }
            
            if (sb.length() > 11) {
                sb.setLength(sb.length() - 2); // Remove last ", "
            } else {
                sb.append("none");
            }
            
            return sb.toString();
        } catch (Exception e) {
            return "Error reading NBT: " + e.getMessage();
        }
    }
}