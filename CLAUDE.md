# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Building the project
```bash
mvn clean package
```
The JAR file will be available in the `/target` directory as `discord-mcp-0.0.1.jar`.

### Running the application locally
```bash
java -jar target/discord-mcp-0.0.1.jar
```
Required environment variables:
- `DISCORD_TOKEN`: Discord bot token (mandatory)
- `DISCORD_GUILD_ID`: Default Discord server ID (optional)

### Docker build and run
```bash
# Build Docker image
docker build -t discord-mcp .

# Run Docker container
docker run --rm -i \
  -e DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN> \
  -e DISCORD_GUILD_ID=<OPTIONAL_DEFAULT_SERVER_ID> \
  discord-mcp
```

## Architecture Overview

This is a Spring Boot application that implements a Model Context Protocol (MCP) server for Discord integration using JDA (Java Discord API).

### Core Components

1. **Main Application**: `DiscordMcpApplication.java` - Spring Boot entry point
2. **Configuration**: `DiscordMcpConfig.java` - Registers MCP tool providers
3. **Lazy JDA Provider**: `LazyJDAProvider.java` - Handles delayed Discord connection initialization
4. **Service Layer**: Located in `src/main/java/dev/saseq/services/`
   - `DiscordService`: Server information retrieval
   - `MessageService`: Message operations (send, edit, delete, read, reactions)
   - `UserService`: User operations (find users, private messages)
   - `ChannelService`: Channel management (create, delete, find, list)
   - `CategoryService`: Category management
   - `WebhookService`: Webhook operations
   - `ForumService`: Forum channel operations (create forums, create posts, manage tags)
   - `ThreadService`: Thread management (create, archive, lock, pin threads)

### MCP Integration

The application uses Spring AI's MCP server capabilities:
- Configured as an STDIO-based MCP server (see `application.properties`)
- All service classes are registered as tool providers via `MethodToolCallbackProvider`
- Each public method in service classes becomes an available MCP tool
- **Lazy initialization**: Discord connection only happens when first Discord tool is called

### Performance Optimizations

The application is optimized for fast startup to prevent MCP tool scanning timeouts:
- **Lazy Loading**: `spring.main.lazy-initialization=true` defers bean creation
- **Lazy JDA Provider**: Discord connection delayed until first tool use
- **Thread-Safe Initialization**: Double-checked locking ensures safe concurrent access
- **Instant Startup**: Application responds to MCP scanning immediately

### Key Dependencies

- **Spring Boot 3.3.6**: Framework foundation
- **Spring AI MCP Server**: MCP protocol implementation
- **JDA 5.6.1**: Discord API wrapper
- **Java 17**: Runtime requirement

### Environment Configuration

The application requires:
- `DISCORD_TOKEN`: Bot authentication token
- `DISCORD_GUILD_ID` (optional): Default server ID for tools

When `DISCORD_GUILD_ID` is set, the `guildId` parameter becomes optional for all Discord operations.

### Application Properties

- Server runs on port 8085
- STDIO transport mode for MCP communication
- Console logging disabled to prevent interference with STDIO
- Lazy initialization enabled for fast startup
- Logs written to `./target/logs/mcp-weather-stdio-server.log`

## Development Notes

### Discord Connection Behavior
- The application starts instantly without connecting to Discord
- Discord connection is established only when the first Discord-related tool is called
- After first connection, all subsequent tool calls use the same JDA instance
- Connection includes auto-reconnect and GUILD_MEMBERS intent for full functionality

### Error Handling
- If `DISCORD_TOKEN` is missing, tools will fail with clear error messages
- JDA initialization failures are wrapped in `RuntimeException` with descriptive messages
- All services include proper null checks and validation

### Adding New Tools
When adding new Discord-related services:
1. Inject `LazyJDAProvider` instead of `JDA` directly
2. Call `jdaProvider.getJDA()` to access the JDA instance
3. Add the service to `DiscordMcpConfig.toolObjects()` list
4. Use `@Tool` and `@ToolParam` annotations for MCP integration