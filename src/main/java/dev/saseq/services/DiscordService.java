package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DiscordService {
    private final JDA jda;

    public DiscordService(JDA jda) {
        this.jda = jda;
    }

    @Tool(name = "get_server_info", description = "Get detailed discord server information")
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

    @Tool(name = "send_message", description = "Send a message to a specific channel")
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

    @Tool(name = "read_messages", description = "Read recent message history from a specific channel")
    public String readMessageFromDiscordChannel(@ToolParam(description = "Discord channel ID") String channelId,
                                                @ToolParam(description = "Number of messages to retrieve", required = false) String count) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        int limit = 100;
        if (count != null) {
            limit = Integer.parseInt(count);
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        List<Message> messages = textChannelById.getHistory().retrievePast(limit).complete();
        List<String> formatedMessages = formatMessages(messages);

        return "**Retrieved " + messages.size() + " messages:** \n" + String.join("\n", formatedMessages);
    }

    @Tool(name = "send_private_messages", description = "Send a private message to a specific user")
    public String sendMessageToPrivateUser(@ToolParam(description = "Message content") String message,
                                           @ToolParam(description = "Discord user ID") String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be null");
        }

        User user = getUserById(userId);

        if (user == null) {
            throw new IllegalArgumentException("User not found by userId");
        }

        Message sentMessage = user.openPrivateChannel().complete().sendMessage(message).complete();
        return "Message sent successfully. Message link: " + sentMessage.getJumpUrl();
    }

    @Tool(name = "read_private_message", description = "Read recent message history from a specific user")
    public String readMessageFromPrivateUser(@ToolParam(description = "Discord user ID") String userId,
                                             @ToolParam(description = "Number of messages to retrieve", required = false) String count) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        int limit = 100;
        if (count != null) {
            limit = Integer.parseInt(count);
        }

        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found by userId");
        }

        List<Message> messages = user.openPrivateChannel().complete().getHistory().retrievePast(limit).complete();
        List<String> formatedMessages = formatMessages(messages);

        return "**Retrieved " + messages.size() + " messages:** \n" + String.join("\n", formatedMessages);
    }

    private User getUserById(String userId) {
        return jda.getGuilds().stream()
                .map(guild -> guild.retrieveMemberById(userId).complete())
                .filter(Objects::nonNull)
                .map(Member::getUser)
                .findFirst()
                .orElse(null);
    }

    private List<String> formatMessages(List<Message> messages) {
        return messages.stream()
                .map(m -> {
                    String authorName = m.getAuthor().getName();
                    String timestamp = m.getTimeCreated().toString();
                    String content = m.getContentDisplay();
                    String messageId = m.getId();

                    return String.format("- (ID: %s) **[%s]** `%s`: ```%s```", messageId, authorName, timestamp, content);
                }).toList();
    }

    @Tool(name = "add_reaction", description = "Add a reaction (emoji) to a specific message")
    public String addReaction(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Discord message ID") String messageId,
                              @ToolParam(description = "Emoji (Unicode or string)") String emoji) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (emoji == null || emoji.isEmpty()) {
            throw new IllegalArgumentException("emoji cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message message = textChannelById.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }

        message.addReaction(Emoji.fromUnicode(emoji)).queue();
        return "Added reaction successfully. Message link: " + message.getJumpUrl();
    }

    @Tool(name = "remove_reaction", description = "Remove a specified reaction (emoji) from a message")
    public String removeReaction(@ToolParam(description = "Discord channel ID") String channelId,
                                 @ToolParam(description = "Discord message ID") String messageId,
                                 @ToolParam(description = "Emoji (Unicode or string)") String emoji) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (emoji == null || emoji.isEmpty()) {
            throw new IllegalArgumentException("emoji cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message message = textChannelById.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }

        message.removeReaction(Emoji.fromUnicode(emoji)).queue();
        return "Added reaction successfully. Message link: " + message.getJumpUrl();
    }

    @Tool(name = "find_text_channel", description = "Find a text channel (name or link) using an ID or name")
    public String findTextChannel(@ToolParam(description = "Discord channel identifier (ID or Name)") String channelIdentifier) {
        if (channelIdentifier == null || channelIdentifier.isEmpty()) {
            throw new IllegalArgumentException("channelIdentifier cannot be null");
        }

        List<TextChannel> textChannels;
        TextChannel channelById = jda.getTextChannelById(channelIdentifier);

        if (channelById != null) {
            textChannels = Collections.singletonList(channelById);
        } else {
            textChannels = jda.getTextChannelsByName(channelIdentifier, true);
        }

        if (textChannels.isEmpty()) {
            throw new IllegalArgumentException("Channel " + channelIdentifier + " not found");
        }
        if (textChannels.size() > 1) {
            String channelList = textChannels.stream()
                    .map(c -> "**" + c.getName() + "** - `" + c.getId() + "`")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Multiple channels found with name " + channelIdentifier + ".\n" +
                    "List: " + channelList + ".\nPlease specify the channel ID.");
        }

        TextChannel responseTextChannel = textChannels.get(0);
        return "Retrieved " + responseTextChannel.getName() + " text channel, with ID " +
                responseTextChannel.getId() + ". Link: " + responseTextChannel.getJumpUrl();
    }
}
