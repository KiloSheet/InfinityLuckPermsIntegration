package me.kilosheet.infinityluckpermsintegration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.log.LogBroadcastEvent;
import net.luckperms.api.LuckPermsProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LuckPermsListener {

    private final InfinityLuckPermsIntegration plugin;
    private long lastMessageTime = 0;
    public LuckPermsListener(InfinityLuckPermsIntegration plugin) {
        this.plugin = plugin;

        LuckPerms luckPerms = LuckPermsProvider.get();
        EventBus eventBus = luckPerms.getEventBus();

        eventBus.subscribe(LogBroadcastEvent.class, this::onLogBroadcast);
    }

    private void onLogBroadcast(LogBroadcastEvent event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime >= TimeUnit.SECONDS.toMillis(3)) {
            String sourceName = event.getEntry().getSource().getName();
            String targetName = event.getEntry().getTarget().getName();
            String description = event.getEntry().getDescription();

            String message = String.format(
                    "**Description**\n- %s\n**Target User/Group**\n- %s\n**Executor**\n- %s",
                    escapeJson(description), escapeJson(targetName), escapeJson(sourceName)
            );

            plugin.getLogger().info("Prepared message to send: " + message);

            sendMessageToDiscord(message);

            lastMessageTime = currentTime;
        } else {
            plugin.getLogger().info("Skipping message due to cooldown.");
        }
    }

    private void sendMessageToDiscord(String message) {
        String webhookUrl = plugin.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Discord webhook URL is not set in the config.");
            return;
        }

        String title = plugin.getConfig().getString("Discord.Title", "LuckPermsLogs");
        String colorHex = plugin.getConfig().getString("Discord.Embed-Color", "#1ABC9C");

        int color;
        try {
            color = Integer.parseInt(colorHex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            color = 0x1ABC9C; 
            plugin.getLogger().warning("Invalid embed color format, using default color.");
        }

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonPayload = String.format(
                    "{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d}]}",
                    escapeJson(title), escapeJson(message), color
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                plugin.getLogger().warning("Failed to send message to Discord. Response code: " + responseCode);
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new BufferedReader(new InputStreamReader(errorStream))
                                .lines().collect(Collectors.joining("\n"));
                        plugin.getLogger().warning("Response error: " + errorResponse);
                    }
                }
            } else {
                plugin.getLogger().info("Message sent successfully to Discord.");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("An error occurred while sending a message to Discord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
