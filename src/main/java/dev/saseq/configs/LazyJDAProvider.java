package dev.saseq.configs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LazyJDAProvider {
    
    private volatile JDA jda;
    private final String token;
    private final Object lock = new Object();
    
    public LazyJDAProvider(@Value("${DISCORD_TOKEN:}") String token) {
        this.token = token;
    }
    
    public JDA getJDA() {
        if (jda == null) {
            synchronized (lock) {
                if (jda == null) {
                    if (token == null || token.isEmpty()) {
                        throw new IllegalStateException("DISCORD_TOKEN environment variable is not set");
                    }
                    try {
                        jda = JDABuilder.createDefault(token)
                                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                                .setAutoReconnect(true)
                                .build();
                        // Wait for the ready state to avoid potential issues
                        jda.awaitReady();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize Discord JDA: " + e.getMessage(), e);
                    }
                }
            }
        }
        return jda;
    }
}