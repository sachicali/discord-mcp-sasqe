package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
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

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Public tool to retrieve a Discord user's ID by their username (optionally with discriminator) in a guild.
     * @param username Username (optionally in the format username#discriminator)
     * @param guildId Optional guild/server ID; uses default if not provided
     * @return User ID string if found, or error message
     */
    @Tool(name = "get_user_id_by_name", description = "Get a Discord user's ID by username in a guild for ping usage <@id>.")
    public String getUserIdByName(
            @ToolParam(description = "Discord username (optionally username#discriminator)") String username,
            @ToolParam(description = "Discord server ID", required = false) String guildId) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username cannot be null");
        }
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        String name = username;
        String discriminatorLocal = null;
        if (username.contains("#")) {
            int idx = username.lastIndexOf('#');
            name = username.substring(0, idx);
            discriminatorLocal = username.substring(idx + 1);
        }
        List<Member> members = guild.getMemberCache().getElementsByUsername(name, true);
        if (discriminatorLocal != null) {
            final String finalDiscriminator = discriminatorLocal;
            members = members.stream()
                    .filter(m -> m.getUser().getDiscriminator().equals(finalDiscriminator))
                    .toList();
        }
        if (members.isEmpty()) {
            throw new IllegalArgumentException("No user found with username " + username);
        }
        if (members.size() > 1) {
            String userList = members.stream()
                    .map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator() + " (ID: " + m.getUser().getId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Multiple users found with username '" + username + "'. List: " + userList + ". Please specify the full username#discriminator.");
        }
        return members.get(0).getUser().getId();
    }

    @Tool(name = "get_server_info", description = "Get detailed discord server information")
    public String getServerInfo(@ToolParam(description = "Discord server ID") String guildId) {
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("Discord server ID cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
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
    public String sendMessage(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Message content") String message) {
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

    @Tool(name = "edit_message", description = "Edit a message from a specific channel")
    public String editMessage(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Specific message ID") String messageId,
                              @ToolParam(description = "New message content") String newMessage) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (newMessage == null || newMessage.isEmpty()) {
            throw new IllegalArgumentException("newMessage cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message messageById = textChannelById.retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        Message editedMessage = messageById.editMessage(newMessage).complete();
        return "Message edited successfully. Message link: " + editedMessage.getJumpUrl();
    }

    @Tool(name = "delete_message", description = "Delete a message from a specific channel")
    public String deleteMessage(@ToolParam(description = "Discord channel ID") String channelId,
                                @ToolParam(description = "Specific message ID") String messageId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        TextChannel textChannelById = jda.getTextChannelById(channelId);
        if (textChannelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message messageById = textChannelById.retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        messageById.delete().queue();
        return "Message deleted successfully";
    }

    @Tool(name = "read_messages", description = "Read recent message history from a specific channel")
    public String readMessages(@ToolParam(description = "Discord channel ID") String channelId,
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

    @Tool(name = "send_private_message", description = "Send a private message to a specific user")
    public String sendPrivateMessage(@ToolParam(description = "Discord user ID") String userId,
                                     @ToolParam(description = "Message content") String message) {
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

    @Tool(name = "edit_private_message", description = "Edit a private message from a specific user")
    public String editPrivateMessage(@ToolParam(description = "Discord user ID") String userId,
                                     @ToolParam(description = "Specific message ID") String messageId,
                                     @ToolParam(description = "New message content") String newMessage) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (newMessage == null || newMessage.isEmpty()) {
            throw new IllegalArgumentException("newMessage cannot be null");
        }

        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found by userId");
        }
        Message messageById = user.openPrivateChannel().complete().retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        Message editedMessage = messageById.editMessage(newMessage).complete();
        return "Message edited successfully. Message link: " + editedMessage.getJumpUrl();
    }

    @Tool(name = "delete_private_message", description = "Delete a private message from a specific user")
    public String deletePrivateMessage(@ToolParam(description = "Discord user ID") String userId,
                                       @ToolParam(description = "Specific message ID") String messageId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found by userId");
        }
        Message messageById = user.openPrivateChannel().complete().retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        messageById.delete().queue();
        return "Message deleted successfully";
    }

    @Tool(name = "read_private_messages", description = "Read recent message history from a specific user")
    public String readPrivateMessages(@ToolParam(description = "Discord user ID") String userId,
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

    @Tool(name = "delete_channel", description = "Delete a channel")
    public String deleteChannel(@ToolParam(description = "Discord server ID") String guildId,
                                @ToolParam(description = "Discord server ID") String channelId) {
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
    public String createTextChannel(@ToolParam(description = "Discord server ID") String guildId,
                                    @ToolParam(description = "Channel name") String name,
                                    @ToolParam(description = "Category ID (optional)", required = false) String categoryId) {
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
    public String findChannel(@ToolParam(description = "Discord server ID") String guildId,
                              @ToolParam(description = "Discord category name") String channelName) {
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
    public String listChannels(@ToolParam(description = "Discord server ID") String guildId) {
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

    @Tool(name = "create_category", description = "Create a new category for channels")
    public String createCategory(@ToolParam(description = "Discord server ID") String guildId,
                                 @ToolParam(description = "Discord category name") String name) {
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
        Category category = guild.createCategory(name).complete();
        return "Created new category: " + category.getName();
    }

    @Tool(name = "delete_category", description = "Delete a category")
    public String deleteCategory(@ToolParam(description = "Discord server ID") String guildId,
                               @ToolParam(description = "Discord category ID") String categoryId) {
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (categoryId == null || categoryId.isEmpty()) {
            throw new IllegalArgumentException("categoryId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Category category = guild.getCategoryById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category not found by categoryId");
        }
        category.delete().queue();
        return "Deleted category: " + category.getName();
    }

    @Tool(name = "find_category", description = "Find a category ID using name and server ID")
    public String findCategory(@ToolParam(description = "Discord server ID") String guildId,
                               @ToolParam(description = "Discord category name") String categoryName) {
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (categoryName == null || categoryName.isEmpty()) {
            throw new IllegalArgumentException("categoryName cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        List<Category> categories = guild.getCategoriesByName(categoryName, true);
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("Category " + categoryName + " not found");
        }
        if (categories.size() > 1) {
            String channelList = categories.stream()
                    .map(c -> "**" + c.getName() + "** - `" + c.getId() + "`")
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Multiple channels found with name " + categoryName + ".\n" +
                    "List: " + channelList + ".\nPlease specify the channel ID.");
        }
        Category category = categories.get(0);
        return "Retrieved category: " + category.getName() + ", with ID: " + category.getId();
    }

    @Tool(name = "list_channels_in_category", description = "List of channels in a specific category")
    public String listChannelsInCategory(@ToolParam(description = "Discord server ID") String guildId,
                                         @ToolParam(description = "Discord category ID") String categoryId) {
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (categoryId == null || categoryId.isEmpty()) {
            throw new IllegalArgumentException("categoryId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }
        Category category = guild.getCategoryById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category not found by categoryId");
        }
        List<GuildChannel> channels = category.getChannels();
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("Category not contains any channels");
        }
        return "Retrieved " + channels.size() + " channels:\n" +
                channels.stream()
                        .map(c -> "- " + c.getType().name() + " channel: " + c.getName() + " (ID: " + c.getId() + ")")
                        .collect(Collectors.joining("\n"));
    }

    @Tool(name = "create_webhook", description = "Create a new webhook on a specific channel")
    public String createWebhook(@ToolParam(description = "Discord channel ID") String channelId,
                                @ToolParam(description = "Webhook name") String name) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("webhook name cannot be null");
        }

        TextChannel channelById = jda.getTextChannelById(channelId);
        if (channelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Webhook webhook = channelById.createWebhook(name).complete();
        return "Created " + name + " webhook: " + webhook.getUrl();
    }

    @Tool(name = "delete_webhook", description = "Delete a webhook")
    public String deleteWebhook(@ToolParam(description = "Discord webhook ID") String webhookId) {
        if (webhookId == null || webhookId.isEmpty()) {
            throw new IllegalArgumentException("webhookId cannot be null");
        }

        Webhook webhook = jda.retrieveWebhookById(webhookId).complete();
        if (webhook == null) {
            throw new IllegalArgumentException("Webhook not found by webhookId");
        }
        webhook.delete().queue();
        return "Deleted " + webhook.getName() + " webhook";
    }

    @Tool(name = "list_webhooks", description = "List of webhooks on a specific channel")
    public String listWebhooks(@ToolParam(description = "Discord channel ID") String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        TextChannel channelById = jda.getTextChannelById(channelId);
        if (channelById == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        List<Webhook> webhooks = channelById.retrieveWebhooks().complete();
        if (webhooks.isEmpty()) {
            throw new IllegalArgumentException("No webhooks found");
        }
        List<String> formattedWebhooks = formatWebhooks(webhooks);
        return "**Retrieved " + formattedWebhooks.size() + " messages:** \n" + String.join("\n", formattedWebhooks);
    }

    private List<String> formatWebhooks(List<Webhook> webhooks) {
        return webhooks.stream()
                .map(w -> String.format("- (ID: %s) **[%s]** ```%s```", w.getId(), w.getName(), w.getUrl()))
                .toList();
    }

    @Tool(name = "send_webhook_message", description = "Send a message via webhook")
    public String sendWebhookMessage(@ToolParam(description = "Discord webhook link") String webhookUrl,
                                     @ToolParam(description = "Message content") String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalArgumentException("webhookUrl cannot be null");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be null");
        }

        IncomingWebhookClient webhookClient = WebhookClient.createClient(jda, webhookUrl);
        if (webhookClient == null) {
            throw new IllegalArgumentException("Webhook not found by webhookUrl");
        }
        Message sentMessage = webhookClient.sendMessage(message).complete();
        return "Message sent successfully. Message link: " + sentMessage.getJumpUrl();
    }
}
