package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagData;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForumService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public ForumService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Creates a new forum channel in a specified Discord server.
     *
     * @param guildId    Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param name       The name for the new forum channel.
     * @param categoryId Optional ID of the category to which the new forum should be added.
     * @param topic      Optional topic/description for the forum channel.
     * @return A confirmation message with the name and ID of the created forum channel.
     */
    @Tool(name = "create_forum_channel", description = "Create a new forum channel")
    public String createForumChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                      @ToolParam(description = "Forum channel name") String name,
                                      @ToolParam(description = "Category ID (optional)", required = false) String categoryId,
                                      @ToolParam(description = "Forum topic/description (optional)", required = false) String topic) {
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

        ForumChannel forumChannel;
        if (categoryId != null && !categoryId.isEmpty()) {
            Category category = guild.getCategoryById(categoryId);
            if (category == null) {
                throw new IllegalArgumentException("Category not found by categoryId");
            }
            forumChannel = category.createForumChannel(name).complete();
        } else {
            forumChannel = guild.createForumChannel(name).complete();
        }

        if (topic != null && !topic.isEmpty()) {
            forumChannel.getManager().setTopic(topic).queue();
        }

        return "Created new forum channel: " + forumChannel.getName() + " (ID: " + forumChannel.getId() + ")" +
                (categoryId != null ? " in category" : "");
    }

    /**
     * Creates a new post (thread) in a forum channel.
     *
     * @param forumChannelId The ID of the forum channel where the post will be created.
     * @param title          The title of the forum post.
     * @param content        The content of the initial message.
     * @param tagNames       Optional comma-separated list of tag names to apply to the post.
     * @return A confirmation message with the thread ID and jump URL.
     */
    @Tool(name = "create_forum_post", description = "Create a new post (thread) in a forum channel")
    public String createForumPost(@ToolParam(description = "Forum channel ID") String forumChannelId,
                                   @ToolParam(description = "Post title") String title,
                                   @ToolParam(description = "Post content") String content,
                                   @ToolParam(description = "Comma-separated tag names (optional)", required = false) String tagNames) {
        if (forumChannelId == null || forumChannelId.isEmpty()) {
            throw new IllegalArgumentException("forumChannelId cannot be null");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("title cannot be null");
        }
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content cannot be null");
        }

        ForumChannel forum = jda.getForumChannelById(forumChannelId);
        if (forum == null) {
            throw new IllegalArgumentException("Forum channel not found by forumChannelId");
        }

        MessageCreateData message = new MessageCreateBuilder()
                .addContent(content)
                .build();

        var postAction = forum.createForumPost(title, message);

        if (tagNames != null && !tagNames.isEmpty()) {
            List<ForumTag> availableTags = forum.getAvailableTags();
            List<ForumTag> tagsToApply = new ArrayList<>();
            
            for (String tagName : tagNames.split(",")) {
                String trimmedTag = tagName.trim();
                availableTags.stream()
                    .filter(tag -> tag.getName().equalsIgnoreCase(trimmedTag))
                    .findFirst()
                    .ifPresent(tagsToApply::add);
            }
            
            if (!tagsToApply.isEmpty()) {
                postAction = postAction.setTags(tagsToApply);
            }
        }

        ForumPost post = postAction.complete();
        ThreadChannel thread = post.getThreadChannel();
        Message starterMessage = post.getMessage();

        return "Created forum post successfully!\n" +
               "Thread ID: " + thread.getId() + "\n" +
               "Thread Name: " + thread.getName() + "\n" +
               "Jump URL: " + starterMessage.getJumpUrl();
    }

    /**
     * Lists all forum channels in a specified Discord server.
     *
     * @param guildId Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @return A formatted string listing all forum channels in the server.
     */
    @Tool(name = "list_forum_channels", description = "List all forum channels in a server")
    public String listForumChannels(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<ForumChannel> forums = guild.getForumChannels();
        if (forums.isEmpty()) {
            return "No forum channels found in this server";
        }

        return "Retrieved " + forums.size() + " forum channels:\n" +
                forums.stream()
                        .map(f -> "- " + f.getName() + " (ID: " + f.getId() + ")" +
                                (f.getTopic() != null && !f.getTopic().isEmpty() ? " - " + f.getTopic() : ""))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Finds a forum channel by name in a specified Discord server.
     *
     * @param guildId     Optional ID of the Discord server (guild). If not provided, the default server will be used.
     * @param forumName   The name of the forum channel to find.
     * @return Information about the found forum channel including its ID and available tags.
     */
    @Tool(name = "find_forum_channel", description = "Find a forum channel by name")
    public String findForumChannel(@ToolParam(description = "Discord server ID", required = false) String guildId,
                                    @ToolParam(description = "Forum channel name") String forumName) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }
        if (forumName == null || forumName.isEmpty()) {
            throw new IllegalArgumentException("forumName cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<ForumChannel> forums = guild.getForumChannelsByName(forumName, true);
        if (forums.isEmpty()) {
            throw new IllegalArgumentException("No forum channel found with name: " + forumName);
        }

        ForumChannel forum = forums.get(0);
        StringBuilder result = new StringBuilder();
        result.append("Found forum channel: ").append(forum.getName())
              .append(" (ID: ").append(forum.getId()).append(")\n");
        
        if (forum.getTopic() != null && !forum.getTopic().isEmpty()) {
            result.append("Topic: ").append(forum.getTopic()).append("\n");
        }

        List<ForumTag> tags = forum.getAvailableTags();
        if (!tags.isEmpty()) {
            result.append("Available tags:\n");
            for (ForumTag tag : tags) {
                result.append("- ").append(tag.getName());
                if (tag.isModerated()) {
                    result.append(" (moderated)");
                }
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Deletes a forum channel from a Discord server.
     *
     * @param forumChannelId The ID of the forum channel to delete.
     * @return A confirmation message.
     */
    @Tool(name = "delete_forum_channel", description = "Delete a forum channel")
    public String deleteForumChannel(@ToolParam(description = "Forum channel ID") String forumChannelId) {
        if (forumChannelId == null || forumChannelId.isEmpty()) {
            throw new IllegalArgumentException("forumChannelId cannot be null");
        }

        ForumChannel forum = jda.getForumChannelById(forumChannelId);
        if (forum == null) {
            throw new IllegalArgumentException("Forum channel not found by forumChannelId");
        }

        String forumName = forum.getName();
        forum.delete().queue();
        return "Deleted forum channel: " + forumName;
    }

    /**
     * Adds a tag to a forum channel.
     *
     * @param forumChannelId The ID of the forum channel.
     * @param tagName        The name of the tag to add.
     * @param emoji          Optional emoji for the tag.
     * @param moderated      Whether the tag should be moderated (only mods can apply).
     * @return A confirmation message.
     */
    @Tool(name = "add_forum_tag", description = "Add a tag to a forum channel")
    public String addForumTag(@ToolParam(description = "Forum channel ID") String forumChannelId,
                               @ToolParam(description = "Tag name") String tagName,
                               @ToolParam(description = "Tag emoji (optional)", required = false) String emoji,
                               @ToolParam(description = "Make tag moderated (true/false)", required = false) String moderated) {
        if (forumChannelId == null || forumChannelId.isEmpty()) {
            throw new IllegalArgumentException("forumChannelId cannot be null");
        }
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("tagName cannot be null");
        }

        ForumChannel forum = jda.getForumChannelById(forumChannelId);
        if (forum == null) {
            throw new IllegalArgumentException("Forum channel not found by forumChannelId");
        }

        List<ForumTagData> currentTags = new ArrayList<>();
        for (ForumTag tag : forum.getAvailableTags()) {
            currentTags.add(ForumTagData.from(tag));
        }

        if (currentTags.size() >= 20) {
            throw new IllegalArgumentException("Forum channel already has maximum number of tags (20)");
        }

        ForumTagData newTag = ForumTagData.of(tagName);
        if ("true".equalsIgnoreCase(moderated)) {
            newTag = newTag.setModerated(true);
        }

        currentTags.add(newTag);
        forum.getManager().setAvailableTags(currentTags).queue();

        return "Added tag '" + tagName + "' to forum channel: " + forum.getName();
    }

    /**
     * Lists all active threads in a forum channel.
     *
     * @param forumChannelId The ID of the forum channel.
     * @return A list of active threads in the forum.
     */
    @Tool(name = "list_forum_threads", description = "List all active threads in a forum channel")
    public String listForumThreads(@ToolParam(description = "Forum channel ID") String forumChannelId) {
        if (forumChannelId == null || forumChannelId.isEmpty()) {
            throw new IllegalArgumentException("forumChannelId cannot be null");
        }

        ForumChannel forum = jda.getForumChannelById(forumChannelId);
        if (forum == null) {
            throw new IllegalArgumentException("Forum channel not found by forumChannelId");
        }

        List<ThreadChannel> threads = forum.getThreadChannels();
        if (threads.isEmpty()) {
            return "No active threads found in forum: " + forum.getName();
        }

        StringBuilder result = new StringBuilder();
        result.append("Active threads in ").append(forum.getName()).append(" (").append(threads.size()).append(" threads):\n");
        
        for (ThreadChannel thread : threads) {
            result.append("- ").append(thread.getName())
                  .append(" (ID: ").append(thread.getId()).append(")");
            
            if (thread.isArchived()) {
                result.append(" [ARCHIVED]");
            }
            if (thread.isLocked()) {
                result.append(" [LOCKED]");
            }
            if (thread.isPinned()) {
                result.append(" [PINNED]");
            }
            
            result.append("\n");
        }

        return result.toString();
    }
}