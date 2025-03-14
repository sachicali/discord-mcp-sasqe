package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class DiscordService {
    private final JDA jda;

    public DiscordService(JDA jda) {
        this.jda = jda;
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
            throw new IllegalArgumentException("Not found channel by channelId");
        }
        Message sentMessage = textChannelById.sendMessage(message).complete();
        return "Message sent successfully. Message link: " + sentMessage.getJumpUrl();
    }
}
