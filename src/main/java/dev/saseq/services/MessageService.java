package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final JDA jda;

    public MessageService(JDA jda) {
        this.jda = jda;
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
}