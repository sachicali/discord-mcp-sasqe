package dev.saseq.configs;

import dev.saseq.services.DiscordService;
import dev.saseq.services.MessageService;
import dev.saseq.services.UserService;
import dev.saseq.services.ChannelService;
import dev.saseq.services.CategoryService;
import dev.saseq.services.WebhookService;
import dev.saseq.services.ForumService;
import dev.saseq.services.ThreadService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordMcpConfig {
    @Bean
    public ToolCallbackProvider discordTools(DiscordService discordService,
                                             MessageService messageService,
                                             UserService userService,
                                             ChannelService channelService,
                                             CategoryService categoryService,
                                             WebhookService webhookService,
                                             ForumService forumService,
                                             ThreadService threadService) {
        return MethodToolCallbackProvider.builder().toolObjects(
                discordService,
                messageService,
                userService,
                channelService,
                categoryService,
                webhookService,
                forumService,
                threadService
        ).build();
    }

    @Bean
    @org.springframework.context.annotation.Lazy
    public JDA jda(@Value("${DISCORD_TOKEN:}") String token) {
        if (token == null || token.isEmpty()) {
            System.err.println("ERROR: The environment variable DISCORD_TOKEN is not set. Please set it to run the application properly.");
            System.exit(1);
        }
        try {
            return JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setAutoReconnect(true)
                    .build();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to initialize Discord JDA: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Discord JDA", e);
        }
    }
}
