/**
 * ProtocolLib - Bukkit server library that allows access to the Minecraft protocol. Copyright (C) 2012 Kristian S.
 * Stangeland
 * <p>
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.comphenix.protocol;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.ImmutableList;

/**
 * Represents the configuration of ProtocolLib.
 *
 * @author Kristian
 */
public class ProtocolConfig {

    private static final String LAST_UPDATE_FILE = "lastupdate";

    private static final String SECTION_GLOBAL = "global";
    private static final String SECTION_AUTOUPDATER = "auto updater";

    private static final String METRICS_ENABLED = "metrics";

    private static final String IGNORE_VERSION_CHECK = "ignore version check";

    private static final String DEBUG_MODE_ENABLED = "debug";
    private static final String DETAILED_ERROR = "detailed error";
    private static final String CHAT_WARNINGS = "chat warnings";

    private static final String SUPPRESSED_REPORTS = "suppressed reports";

    private static final String UPDATER_NOTIFY = "notify";
    private static final String UPDATER_DOWNLAD = "download";
    private static final String UPDATER_DELAY = "delay";

    // Defaults
    private static final long DEFAULT_UPDATER_DELAY = 43200;

    private Plugin plugin;
    private Configuration config;
    private boolean loadingSections;

    private ConfigurationSection global;
    private ConfigurationSection updater;

    // Last update time
    private long lastUpdateTime;
    private boolean configChanged;
    private boolean valuesChanged;

    // Modifications
    private int modCount;

    public ProtocolConfig(Plugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    /**
     * Reload configuration file.
     */
    public void reloadConfig() {
        // Reset
        configChanged = false;
        valuesChanged = false;
        modCount++;

        this.config = plugin.getConfig();
        this.lastUpdateTime = loadLastUpdate();
        loadSections(!loadingSections);
    }

    /**
     * Load the last update time stamp from the file system.
     *
     * @return Last update time stamp.
     */
    private long loadLastUpdate() {
        Path dataFile = getLastUpdateFile();

        if (Files.exists(dataFile)) {
            try {
                String value = new String(Files.readAllBytes(dataFile), StandardCharsets.UTF_8);
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Cannot parse " + dataFile + " as a number.");
            } catch (IOException e) {
                plugin.getLogger().warning("Cannot read " + dataFile);
            }
        }
        // Default last update
        return 0;
    }

    /**
     * Store the given time stamp.
     *
     * @param value - time stamp to store.
     */
    private void saveLastUpdate(long value) {
        Path dataFile = getLastUpdateFile();

        try {
            // The data folder must exist
            Files.createDirectories(dataFile.getParent());

            // write update timestamp
            Files.write(dataFile, Long.toString(value).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Cannot write " + dataFile, e);
        }
    }

    /**
     * Retrieve the file that is used to store the update time stamp.
     *
     * @return File storing the update time stamp.
     */
    private Path getLastUpdateFile() {
        return plugin.getDataFolder().toPath().resolve(LAST_UPDATE_FILE);
    }

    /**
     * Load data sections.
     *
     * @param copyDefaults - whether or not to copy configuration defaults.
     */
    private void loadSections(boolean copyDefaults) {
        if (config != null) {
            global = config.getConfigurationSection(SECTION_GLOBAL);
        }
        if (global != null) {
            updater = global.getConfigurationSection(SECTION_AUTOUPDATER);
            if (updater.getValues(true).isEmpty()) {
                plugin.getLogger().warning("Updater section is missing, regenerate your config!");
            }
        }

        // Automatically copy defaults
        if (copyDefaults && (!getFile().exists() || global == null || updater == null)) {
            loadingSections = true;

            if (config != null) {
                config.options().copyDefaults(true);
            }
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            loadingSections = false;

            // Inform the user
            plugin.getLogger().info("Created default configuration.");
        }
    }

    /**
     * Set a particular configuration key value pair.
     *
     * @param section - the configuration root.
     * @param path - the path to the key.
     * @param value - the value to set.
     */
    private void setConfig(ConfigurationSection section, String path, Object value) {
        configChanged = true;
        section.set(path, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getGlobalValue(String path, T def) {
        try {
            return (T) global.get(path, def);
        } catch (Throwable ex) {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getUpdaterValue(String path, T def) {
        try {
            return (T) updater.get(path, def);
        } catch (Throwable ex) {
            return def;
        }
    }

    /**
     * Retrieve a reference to the configuration file.
     *
     * @return Configuration file on disk.
     */
    public File getFile() {
        return new File(plugin.getDataFolder(), "config.yml");
    }

    /**
     * Determine if detailed error reporting is enabled. Default FALSE.
     *
     * @return TRUE if it is enabled, FALSE otherwise.
     */
    public boolean isDetailedErrorReporting() {
        return getGlobalValue(DETAILED_ERROR, false);
    }

    /**
     * Print warnings to players with protocol.info
     * @return true if enabled, false if not
     */
    public boolean isChatWarnings() {
        return getGlobalValue(CHAT_WARNINGS, true);
    }

    /**
     * Retrieve whether or not ProtocolLib should determine if a new version has been released.
     *
     * @return TRUE if it should do this automatically, FALSE otherwise.
     */
    public boolean isAutoNotify() {
        return getUpdaterValue(UPDATER_NOTIFY, true);
    }

    /**
     * Retrieve whether or not ProtocolLib should automatically download the new version.
     *
     * @return TRUE if it should, FALSE otherwise.
     */
    public boolean isAutoDownload() {
        return updater != null && getUpdaterValue(UPDATER_DOWNLAD, false);
    }

    /**
     * Determine whether or not debug mode is enabled.
     * <p>
     * This grants access to the filter command.
     *
     * @return TRUE if it is, FALSE otherwise.
     */
    public boolean isDebug() {
        return getGlobalValue(DEBUG_MODE_ENABLED, false);
    }

    /**
     * Set whether or not debug mode is enabled.
     *
     * @param value - TRUE if it is enabled, FALSE otherwise.
     */
    public void setDebug(boolean value) {
        setConfig(global, DEBUG_MODE_ENABLED, value);
        modCount++;
    }

    /**
     * Retrieve an immutable list of every suppressed report type.
     *
     * @return Every suppressed report type.
     */
    public ImmutableList<String> getSuppressedReports() {
        return ImmutableList.copyOf(getGlobalValue(SUPPRESSED_REPORTS, new ArrayList<String>()));
    }

    /**
     * Retrieve the amount of time to wait until checking for a new update.
     *
     * @return The amount of time to wait.
     */
    public long getAutoDelay() {
        // Note that the delay must be greater than 59 seconds
        return Math.max(getUpdaterValue(UPDATER_DELAY, 0), DEFAULT_UPDATER_DELAY);
    }

    /**
     * The version of Minecraft to ignore the built-in safety feature.
     *
     * @return The version to ignore ProtocolLib's satefy.
     */
    public String getIgnoreVersionCheck() {
        return getGlobalValue(IGNORE_VERSION_CHECK, "");
    }

    /**
     * Retrieve whether or not metrics is enabled.
     *
     * @return TRUE if metrics is enabled, FALSE otherwise.
     */
    public boolean isMetricsEnabled() {
        return getGlobalValue(METRICS_ENABLED, true);
    }

    /**
     * Retrieve the last time we updated, in seconds since 1970.01.01 00:00.
     *
     * @return Last update time.
     */
    public long getAutoLastTime() {
        return lastUpdateTime;
    }

    /**
     * Set the last time we updated, in seconds since 1970.01.01 00:00.
     * <p>
     * Note that this is not considered to modify the configuration, so the modification count will not be incremented.
     *
     * @param lastTimeSeconds - new last update time.
     */
    public void setAutoLastTime(long lastTimeSeconds) {
        this.valuesChanged = true;
        this.lastUpdateTime = lastTimeSeconds;
    }

    /**
     * Retrieve the number of modifications made to this configuration.
     *
     * @return The number of modifications.
     */
    public int getModificationCount() {
        return modCount;
    }

    /**
     * Save the current configuration file.
     */
    public void saveAll() {
        if (valuesChanged) {
            saveLastUpdate(lastUpdateTime);
        }
        if (configChanged) {
            plugin.saveConfig();
        }

        // And we're done
        valuesChanged = false;
        configChanged = false;
    }
}
