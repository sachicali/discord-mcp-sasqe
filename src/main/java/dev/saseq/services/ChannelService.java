package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public ChannelService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    @Tool(name = "delete_channel", description = "Delete a channel")
    public String deleteChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                @ToolParam(description = "Discord channel ID") String channelId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        GuildChannel channel = guild.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        channel.delete().queue();
        return "Deleted " + channel.getType().name() + " channel: " + channel.getName();
    }

    @Tool(name = "create_text_channel", description = "Create a new text channel")
    public String createTextChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                    @ToolParam(description = "Channel name") String name,
                                    @ToolParam(description = "Category ID (optional)", required = false) String categoryId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        TextChannel textChannel;
        if (categoryId != null && !categoryId.isEmpty()) {
            Category category = guild.getCategoryById(categoryId);
            if (category == null) {
                throw new IllegalArgumentException("Category not found by categoryId");
            }
            textChannel = category.createTextChannel(name).complete();
            return "Created new text channel: " + textChannel.getName() + " (ID: " + textChannel.getId() + ") in category: " + category.getName();
        } else {
            textChannel = guild.createTextChannel(name).complete();
            return "Created new text channel: " + textChannel.getName() + " (ID: " + textChannel.getId() + ")";
        }
    }

    @Tool(name = "find_channel", description = "Find a channel type and ID using name and server ID")
    public String findChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                              @ToolParam(description = "Discord category name") String channelName) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (channelName == null || channelName.isEmpty()) {
            throw new IllegalArgumentException("channelName cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        List<GuildChannel> channels = guild.getChannels();
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("No channels found by guildId");
        }
        List<GuildChannel> filteredChannels = channels.stream().filter(c -> c.getName().equalsIgnoreCase(channelName)).toList();
        if (filteredChannels.isEmpty()) {
            throw new IllegalArgumentException("No channels found with name " + channelName);
        }
        if (filteredChannels.size() > 1) {
            return "Retrieved " + channels.size() + " channels:\n" +
                    channels.stream()
                            .map(c -> "- " + c.getType().name() + " channel: " + c.getName() + " (ID: " + c.getId() + ")")
                            .collect(Collectors.joining("\n"));
        }
        GuildChannel channel = filteredChannels.get(0);
        return "Retrieved " + channel.getType().name() + " channel: " + channel.getName() + " (ID: " + channel.getId() + ")";
    }

    @Tool(name = "list_channels", description = "List of all channels")
    public String listChannels(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        List<GuildChannel> channels = guild.getChannels();
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("No channels found by guildId");
        }
        return "Retrieved " + channels.size() + " channels:\n" +
                channels.stream()
                        .map(c -> "- " + c.getType().name() + " channel: " + c.getName() + " (ID: " + c.getId() + ")")
                        .collect(Collectors.joining("\n"));
    }
}