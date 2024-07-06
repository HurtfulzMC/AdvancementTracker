package me.hurtful.advancementtracker;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.util.Iterator;

public class AdvancementTracker extends JavaPlugin implements Listener {

    private static final int CHAT_WIDTH = 53;
    private JDA jda;
    private String discordChannelId;

    @Override
    public void onEnable() {
        getLogger().info("AdvancementTracker has been enabled!");
        getServer().getPluginManager().registerEvents(this, this);

        // Load configuration
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        String token = config.getString("discord.token");
        discordChannelId = config.getString("discord.channelId");

        // Initialize Discord bot
        if (token != null && !token.isEmpty()) {
            try {
                jda = JDABuilder.createDefault(token).build();
                jda.awaitReady();
                getLogger().info("Discord bot has been initialized!");
            } catch (IllegalArgumentException e) {
                getLogger().severe("Invalid Discord bot token: " + e.getMessage());
            } catch (InterruptedException e) {
                getLogger().severe("Discord bot initialization was interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                getLogger().severe("Failed to initialize Discord bot: " + e.getMessage());
            }
        } else {
            getLogger().warning("Discord bot token is not set in the config. Discord integration will be disabled.");
        }
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
        }
        getLogger().info("AdvancementTracker has been disabled!");
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        String key = advancement.getKey().getKey();

        if (!key.startsWith("recipes/")) {
            int[] progress = getAdvancementProgress(player);
            sendEnhancedProgressMessage(player, key, progress[0], progress[1]);

            if (progress[0] == progress[1]) {
                sendAllAdvancementsCompletedMessage(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int[] progress = getAdvancementProgress(player);
        sendJoinProgressMessage(player, progress[0], progress[1]);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("advancements")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return true;
            }

            Player player = (Player) sender;
            int[] progress = getAdvancementProgress(player);

            sendProgressMessage(player, progress[0], progress[1]);

            return true;
        }
        return false;
    }

    private int[] getAdvancementProgress(Player player) {
        int totalAdvancements = 0;
        int completedAdvancements = 0;

        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            String key = advancement.getKey().getKey();

            if (!key.startsWith("recipes/")) {
                totalAdvancements++;
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                if (progress.isDone()) {
                    completedAdvancements++;
                }
            }
        }

        return new int[]{completedAdvancements, totalAdvancements};
    }

    private void sendEnhancedProgressMessage(Player player, String advancementKey, int completed, int total) {
        player.sendMessage("");
        player.sendMessage(centerText(ChatColor.GOLD + "★ " + ChatColor.YELLOW + "Advancement Completed!" + ChatColor.GOLD + " ★"));
        player.sendMessage(centerText(ChatColor.AQUA + "「 " + advancementKey + " 」"));
        player.sendMessage(centerText(createColorfulProgressBar(completed, total)));
        player.sendMessage(centerText(ChatColor.GOLD + String.format("%d/%d ", completed, total) +
                ChatColor.YELLOW + "(" + ChatColor.GREEN + formatPercentage(completed, total) + ChatColor.YELLOW + ")"));
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private void sendProgressMessage(Player player, int completed, int total) {
        player.sendMessage("");
        player.sendMessage(centerText(ChatColor.GOLD + "★ " + ChatColor.YELLOW + "Advancement Progress" + ChatColor.GOLD + " ★"));
        player.sendMessage(centerText(createColorfulProgressBar(completed, total)));
        player.sendMessage(centerText(ChatColor.GOLD + String.format("%d/%d ", completed, total) +
                ChatColor.YELLOW + "(" + ChatColor.GREEN + formatPercentage(completed, total) + ChatColor.YELLOW + ")"));
        player.sendMessage("");
    }

    private void sendJoinProgressMessage(Player player, int completed, int total) {
        player.sendMessage("");
        player.sendMessage(centerText(ChatColor.GOLD + "Welcome back, " + player.getName() + "!"));
        player.sendMessage(centerText(ChatColor.YELLOW + "Your current advancement progress:"));
        player.sendMessage(centerText(createColorfulProgressBar(completed, total)));
        player.sendMessage(centerText(ChatColor.GOLD + String.format("%d/%d ", completed, total) +
                ChatColor.YELLOW + "(" + ChatColor.GREEN + formatPercentage(completed, total) + ChatColor.YELLOW + ")"));
        player.sendMessage("");
    }

    private void sendAllAdvancementsCompletedMessage(Player player) {
        player.sendMessage("");
        player.sendMessage(centerText(ChatColor.GOLD + "✦✦✦ " + ChatColor.YELLOW + "CONGRATULATIONS!" + ChatColor.GOLD + " ✦✦✦"));
        player.sendMessage(centerText(ChatColor.GREEN + "You have completed ALL advancements!"));
        player.sendMessage(centerText(ChatColor.AQUA + "You are a true Minecraft master!"));
        player.sendMessage("");
        player.sendMessage(centerText(ChatColor.GOLD + "Your journey is complete, but the adventure"));
        player.sendMessage(centerText(ChatColor.GOLD + "never truly ends in the world of Minecraft!"));
        player.sendMessage("");

        // Play a celebratory sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Send a message to all players and Discord
        String message = ChatColor.GOLD + "✦ " + ChatColor.GREEN + player.getName() +
                ChatColor.YELLOW + " has completed ALL Minecraft advancements!" + ChatColor.GOLD + " ✦";
        Bukkit.broadcastMessage(message);
        sendToDiscord(player, ChatColor.stripColor(message));
    }

    private void sendToDiscord(Player player, String message) {
        if (jda != null && discordChannelId != null) {
            TextChannel channel = jda.getTextChannelById(discordChannelId);
            if (channel != null) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Color.ORANGE);
                embed.setAuthor(player.getName(), null, "https://crafatar.com/avatars/" + player.getUniqueId() + "?size=64&overlay");
                embed.setDescription(message);
                embed.setFooter("Minecraft Server Achievement", "https://www.minecraft.net/content/dam/games/minecraft/key-art/Games_Subnav_Minecraft-300x465.jpg");
                channel.sendMessageEmbeds(embed.build()).queue();
            } else {
                getLogger().warning("Could not find Discord channel with ID: " + discordChannelId);
            }
        }
    }

    private String centerText(String text) {
        int spaces = (CHAT_WIDTH - ChatColor.stripColor(text).length()) / 2;
        return StringUtils.repeat(" ", spaces) + text;
    }

    private String createColorfulProgressBar(int completed, int total) {
        int barWidth = 20;
        int filledWidth = (int) ((double) completed / total * barWidth);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barWidth; i++) {
            if (i < filledWidth) {
                bar.append(getGradientColor(i, barWidth)).append("■");
            } else {
                bar.append(ChatColor.GRAY).append("□");
            }
        }

        return bar.toString();
    }

    private ChatColor getGradientColor(int index, int total) {
        ChatColor[] colors = {ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.AQUA, ChatColor.LIGHT_PURPLE};
        int colorIndex = (int) ((double) index / total * (colors.length - 1));
        return colors[colorIndex];
    }

    private String formatPercentage(int completed, int total) {
        return String.format("%.1f%%", (double) completed / total * 100);
    }

    private static class StringUtils {
        public static String repeat(String str, int times) {
            return new String(new char[times]).replace("\0", str);
        }
    }
}