package me.kilosheet.infinityluckpermsintegration;

import org.bukkit.plugin.java.JavaPlugin;

public class InfinityLuckPermsIntegration extends JavaPlugin {

    private String discordWebhookUrl;

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        discordWebhookUrl = getConfig().getString("Discord-Webhook");

        if (discordWebhookUrl == null || discordWebhookUrl.isEmpty()) {
            getLogger().warning("§bInfinity§cLuckPerms§aIntegration §7-> §6Please fill the discord webhook part in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        new LuckPermsListener(this);

        getLogger().info("InfinityLuckPermsIntegration has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("InfinityLuckPermsIntegration has been disabled.");
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }
}
