package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IncomingWebhookClient;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebhookService {

    private final JDA jda;

    public WebhookService(JDA jda) {
        this.jda = jda;
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
