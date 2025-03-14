package dev.saseq.configs;

import dev.saseq.services.DiscordService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordMcpConfig {
    @Bean
    public ToolCallbackProvider discordTools(DiscordService discordService) {
        return MethodToolCallbackProvider.builder().toolObjects(discordService).build();
    }

    @Bean
    public JDA jda(@Value("${DISCORD_TOKEN:}") String token) {
        if (token == null || token.isEmpty()) {
            System.err.println("ERROR: The environment variable DISCORD_TOKEN is not set. Please set it to run the application properly.");
            System.exit(1);
        }
        return JDABuilder.createDefault(token).build();
    }
}
