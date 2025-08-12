package dev.saseq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class DiscordMcpApplication {
    
    private static final CountDownLatch keepAlive = new CountDownLatch(1);
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DiscordMcpApplication.class, args);
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            keepAlive.countDown();
            context.close();
        }));
        
        try {
            // Keep the application running for STDIO MCP server
            keepAlive.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}