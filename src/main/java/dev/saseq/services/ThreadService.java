package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThreadService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public ThreadService(JDA jda) {
        this.jda = jda;
    }

    private String resolveGuildId(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty()) {
            return defaultGuildId;
        }
        return guildId;
    }

    /**
     * Creates a new thread in a text channel.
     *
     * @param channelId     The ID of the text channel where the thread will be created.
     * @param threadName    The name of the thread.
     * @param messageId     Optional ID of a message to start the thread from.
     * @param autoArchive   Optional auto-archive duration in minutes (60, 1440, 4320, 10080).
     * @return Information about the created thread.
     */
    @Tool(name = "create_thread", description = "Create a new thread in a text channel")
    public String createThread(@ToolParam(description = "Text channel ID") String channelId,
                                @ToolParam(description = "Thread name") String threadName,
                                @ToolParam(description = "Message ID to start thread from (optional)", required = false) String messageId,
                                @ToolParam(description = "Auto-archive duration in minutes (optional)", required = false) String autoArchive) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (threadName == null || threadName.isEmpty()) {
            throw new IllegalArgumentException("threadName cannot be null");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Text channel not found by channelId");
        }

        ThreadChannel thread;
        if (messageId != null && !messageId.isEmpty()) {
            Message message = channel.retrieveMessageById(messageId).complete();
            if (message == null) {
                throw new IllegalArgumentException("Message not found by messageId");
            }
            thread = message.createThreadChannel(threadName).complete();
        } else {
            var action = channel.createThreadChannel(threadName);
            if (autoArchive != null && !autoArchive.isEmpty()) {
                try {
                    int duration = Integer.parseInt(autoArchive);
                    ThreadChannel.AutoArchiveDuration archiveDuration = ThreadChannel.AutoArchiveDuration.fromKey(duration);
                    action = action.setAutoArchiveDuration(archiveDuration);
                } catch (Exception e) {
                    // Invalid duration, use default
                }
            }
            thread = action.complete();
        }

        return "Created thread successfully!\n" +
               "Thread ID: " + thread.getId() + "\n" +
               "Thread Name: " + thread.getName() + "\n" +
               "Parent Channel: " + channel.getName();
    }

    /**
     * Sends a message to a thread.
     *
     * @param threadId The ID of the thread.
     * @param message  The message content to send.
     * @return A confirmation message with the sent message details.
     */
    @Tool(name = "send_thread_message", description = "Send a message to a thread")
    public String sendThreadMessage(@ToolParam(description = "Thread ID") String threadId,
                                     @ToolParam(description = "Message content") String message) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        Message sentMessage = thread.sendMessage(message).complete();
        return "Message sent to thread '" + thread.getName() + "'\n" +
               "Message link: " + sentMessage.getJumpUrl();
    }

    /**
     * Archives or unarchives a thread.
     *
     * @param threadId The ID of the thread to archive/unarchive.
     * @param archive  Whether to archive (true) or unarchive (false) the thread.
     * @return A confirmation message.
     */
    @Tool(name = "archive_thread", description = "Archive or unarchive a thread")
    public String archiveThread(@ToolParam(description = "Thread ID") String threadId,
                                 @ToolParam(description = "Archive thread (true/false)") String archive) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (archive == null || archive.isEmpty()) {
            throw new IllegalArgumentException("archive cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        boolean shouldArchive = Boolean.parseBoolean(archive);
        thread.getManager().setArchived(shouldArchive).queue();

        return (shouldArchive ? "Archived" : "Unarchived") + " thread: " + thread.getName();
    }

    /**
     * Locks or unlocks a thread.
     *
     * @param threadId The ID of the thread to lock/unlock.
     * @param lock     Whether to lock (true) or unlock (false) the thread.
     * @return A confirmation message.
     */
    @Tool(name = "lock_thread", description = "Lock or unlock a thread")
    public String lockThread(@ToolParam(description = "Thread ID") String threadId,
                              @ToolParam(description = "Lock thread (true/false)") String lock) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (lock == null || lock.isEmpty()) {
            throw new IllegalArgumentException("lock cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        boolean shouldLock = Boolean.parseBoolean(lock);
        thread.getManager().setLocked(shouldLock).queue();

        return (shouldLock ? "Locked" : "Unlocked") + " thread: " + thread.getName();
    }

    /**
     * Pins or unpins a thread in a forum channel.
     *
     * @param threadId The ID of the thread to pin/unpin.
     * @param pin      Whether to pin (true) or unpin (false) the thread.
     * @return A confirmation message.
     */
    @Tool(name = "pin_thread", description = "Pin or unpin a thread in a forum")
    public String pinThread(@ToolParam(description = "Thread ID") String threadId,
                             @ToolParam(description = "Pin thread (true/false)") String pin) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (pin == null || pin.isEmpty()) {
            throw new IllegalArgumentException("pin cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        boolean shouldPin = Boolean.parseBoolean(pin);
        thread.getManager().setPinned(shouldPin).queue();

        return (shouldPin ? "Pinned" : "Unpinned") + " thread: " + thread.getName();
    }

    /**
     * Adds a member to a private thread.
     *
     * @param threadId The ID of the thread.
     * @param userId   The ID of the user to add.
     * @return A confirmation message.
     */
    @Tool(name = "add_thread_member", description = "Add a member to a thread")
    public String addThreadMember(@ToolParam(description = "Thread ID") String threadId,
                                   @ToolParam(description = "User ID to add") String userId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        User user = jda.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found by userId");
        }

        thread.addThreadMember(user).queue();
        return "Added user " + user.getName() + " to thread: " + thread.getName();
    }

    /**
     * Removes a member from a thread.
     *
     * @param threadId The ID of the thread.
     * @param userId   The ID of the user to remove.
     * @return A confirmation message.
     */
    @Tool(name = "remove_thread_member", description = "Remove a member from a thread")
    public String removeThreadMember(@ToolParam(description = "Thread ID") String threadId,
                                      @ToolParam(description = "User ID to remove") String userId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        User user = jda.getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found by userId");
        }

        thread.removeThreadMember(user).queue();
        return "Removed user " + user.getName() + " from thread: " + thread.getName();
    }

    /**
     * Lists all active threads in a Discord server.
     *
     * @param guildId Optional ID of the Discord server. If not provided, the default server will be used.
     * @return A list of all active threads in the server.
     */
    @Tool(name = "list_all_threads", description = "List all active threads in a server")
    public String listAllThreads(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        guildId = resolveGuildId(guildId);
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Discord server not found by guildId");
        }

        List<ThreadChannel> threads = guild.getThreadChannels();
        if (threads.isEmpty()) {
            return "No active threads found in server: " + guild.getName();
        }

        StringBuilder result = new StringBuilder();
        result.append("Active threads in ").append(guild.getName()).append(" (").append(threads.size()).append(" threads):\n");
        
        for (ThreadChannel thread : threads) {
            IThreadContainerUnion parent = thread.getParentChannel();
            result.append("- ").append(thread.getName())
                  .append(" (ID: ").append(thread.getId()).append(")")
                  .append(" in ").append(parent.getName());
            
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

    /**
     * Gets detailed information about a thread.
     *
     * @param threadId The ID of the thread.
     * @return Detailed information about the thread.
     */
    @Tool(name = "get_thread_info", description = "Get detailed information about a thread")
    public String getThreadInfo(@ToolParam(description = "Thread ID") String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        StringBuilder info = new StringBuilder();
        info.append("Thread Information:\n");
        info.append("Name: ").append(thread.getName()).append("\n");
        info.append("ID: ").append(thread.getId()).append("\n");
        info.append("Parent Channel: ").append(thread.getParentChannel().getName()).append("\n");
        info.append("Owner ID: ").append(thread.getOwnerId()).append("\n");
        info.append("Created: ").append(thread.getTimeCreated()).append("\n");
        info.append("Member Count: ").append(thread.getMemberCount()).append("\n");
        info.append("Message Count: ").append(thread.getMessageCount()).append("\n");
        info.append("Archived: ").append(thread.isArchived()).append("\n");
        info.append("Locked: ").append(thread.isLocked()).append("\n");
        info.append("Pinned: ").append(thread.isPinned()).append("\n");
        info.append("Auto-archive duration: ").append(thread.getAutoArchiveDuration()).append(" minutes\n");

        List<ForumTag> appliedTags = thread.getAppliedTags();
        if (!appliedTags.isEmpty()) {
            info.append("Applied Tags: ");
            info.append(appliedTags.stream()
                    .map(ForumTag::getName)
                    .collect(Collectors.joining(", ")));
            info.append("\n");
        }

        return info.toString();
    }

    /**
     * Reads recent messages from a thread.
     *
     * @param threadId The ID of the thread.
     * @param count    Optional number of messages to retrieve (default is 50).
     * @return A formatted list of recent messages from the thread.
     */
    @Tool(name = "read_thread_messages", description = "Read recent messages from a thread")
    public String readThreadMessages(@ToolParam(description = "Thread ID") String threadId,
                                      @ToolParam(description = "Number of messages to retrieve", required = false) String count) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found by threadId");
        }

        int limit = 50;
        if (count != null && !count.isEmpty()) {
            try {
                limit = Integer.parseInt(count);
                if (limit > 100) limit = 100;
                if (limit < 1) limit = 1;
            } catch (NumberFormatException e) {
                // Use default
            }
        }

        List<Message> messages = thread.getHistory().retrievePast(limit).complete();
        if (messages.isEmpty()) {
            return "No messages found in thread: " + thread.getName();
        }

        StringBuilder result = new StringBuilder();
        result.append("Retrieved ").append(messages.size()).append(" messages from thread '")
              .append(thread.getName()).append("':\n\n");

        for (Message msg : messages) {
            result.append("- (ID: ").append(msg.getId()).append(") ")
                  .append("[").append(msg.getAuthor().getName()).append("] ")
                  .append("`").append(msg.getTimeCreated()).append("`: ")
                  .append("```").append(msg.getContentDisplay()).append("```\n");
        }

        return result.toString();
    }
}