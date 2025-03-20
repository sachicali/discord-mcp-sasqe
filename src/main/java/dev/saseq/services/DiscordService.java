package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DiscordService {
    private final JDA jda;

    public DiscordService(JDA jda) {
        this.jda = jda;
    }

    @Tool(name = "get_server_info",description = "Get detailed discord server information")
    public String getServerInfo(@ToolParam(description = "Discord server ID") String guildId) {
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("Discord server ID cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server ID: " + guildId + " not found");
        }

        String serverName = guild.getName();
        String serverId = guild.getId();
        Member owner = guild.retrieveOwner().complete();
        int totalMembers = guild.getMemberCount();
        int textChannelCount = guild.getTextChannels().size();
        int voiceChannelCount = guild.getVoiceChannels().size();
        int categoryCount = guild.getCategories().size();
        String creationDate = guild.getTimeCreated().toLocalDate().toString();

        int boostCount = guild.getBoostCount();
        String boostTier = guild.getBoostTier().toString();

        return "Server Name: " + serverName + "\n" +
                "Server ID: " + serverId + "\n" +
                "Owner: " + owner.getUser().getName() + "\n" +
                "Created On: " + creationDate + "\n" +
                "Members: " + totalMembers + "\n" +
                "Channels: " +
                " - Text: " + textChannelCount + "\n" +
                " - Voice: " + voiceChannelCount + "\n" +
                "  - Categories: " + categoryCount + "\n" +
                "Boosts: " +
                " - Count: " + boostCount + "\n" +
                " - Tier: " + boostTier;
    }

    @Tool(name = "send_message",description = "Send a message to a specific channel")
    public String sendMessageToDiscordChannel(@ToolParam(description = "Message content") String message,
                                              @ToolParam(description = "Discord channel ID") String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message sentMessage = textChannelById.sendMessage(message).complete();
        return "Message sent successfully. Message link: " + sentMessage.getJumpUrl();
    }

    @Tool(name = "send_private_message",description = "Send a private message to a specific user")
    public String sendMessageToPrivateUser(@ToolParam(description = "Message content") String message,
                                           @ToolParam(description = "Discord user ID") String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be null");
        }

        User user = jda.getGuilds().stream()
                .map(guild -> guild.retrieveMemberById(userId).complete())
                .filter(Objects::nonNull)
                .map(Member::getUser)
                .findFirst()
                .orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("User not found by userId");
        }

        Message sentMessage = user.openPrivateChannel().complete().sendMessage(message).complete();
        return "Message sent successfully. Message link: " + sentMessage.getJumpUrl();
    }
}
