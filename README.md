<div align="center">
  <img src="assets/img/Discord_MCP_full_logo.svg" width="60%" alt="DeepSeek-V3" />
</div>
<hr>
<div align="center" style="line-height: 1;">
    <a href="https://smithery.ai/server/@SaseQ/discord-mcp" target="_blank" style="margin: 2px;">
        <img alt="Smithery Badge" src="https://camo.githubusercontent.com/ee5c6c6dc502821f4d57313b2885f7878af52be14142dd98526ea12aedf9b260/68747470733a2f2f736d6974686572792e61692f62616467652f40646d6f6e74676f6d65727934302f646565707365656b2d6d63702d736572766572" data-canonical-src="https://smithery.ai/server/@SaseQ/discord-mcp" style="display: inline-block; vertical-align: middle;"/>
    </a>
    <a href="https://discord.gg/5Uvxe5jteM" target="_blank" style="margin: 2px;">
        <img alt="Discord" src="https://img.shields.io/badge/Discord-SaseQcode-7289da?logo=discord&logoColor=white&color=7289da" style="display: inline-block; vertical-align: middle;"/>
    </a>
</div>


## 📖 Description

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) server for the Discord API [(JDA)](https://jda.wiki/), 
allowing seamless integration of Discord Bot with MCP-compatible applications like Claude Desktop.


## 🔬 Installation

#### Clone the repository
```
git clone https://github.com/SaseQ/discord-mcp
```

#### Build the project
```
cd discord-mcp
mvn clean package
```

#### Configure Claude Desktop
```
{
  "mcpServers": {
    "discord-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/discord-mcp-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "DISCORD_TOKEN": "YOUR_DISCORD_BOT_TOKEN"
      }
    }
  }
}
```

*To get a discord bot token, visit the [Discord Developer Portal](https://discord.com/developers)


## ⚓ Smithery

Install Discord MCP Server automatically via Smithery:
```
npx -y @smithery/cli@latest install @SaseQ/discord-mcp --client claude
```


## 🛠️ Available Tools

#### Server Information
 - [`get_server_info`](): Get detailed discord server information

#### Message Management
 - [`send_message`](): Send a message to a specific channel
 - [`edit_message`](): Edit a message from a specific channel
 - [`delete_message`](): Delete a message from a specific channel
 - [`read_messages`](): Read recent message history from a specific channel
 - [`send_private_message`](): Send a private message to a specific user
 - [`edit_private_message`](): Edit a private message from a specific user
 - [`delete_private_message`](): Delete a private message from a specific user
 - [`read_private_messages`](): Read recent message history from a specific user
 - [`add_reaction`](): Add a reaction (emoji) to a specific message
 - [`remove_reaction`](): Remove a specified reaction (emoji) from a message

#### Channel Management
 - [`find_channel`](): Find a channel type and ID using name and server ID

#### Category Management
 - [`create_category`](): Create a new category for channels
 - [`delete_category`](): Delete a category
 - [`find_category`](): Find a category ID using name and server ID
 - [`list_channels_in_category`](): List of channels in a specific category

#### Webhook Management
 - [`create_webhook`](): Create a new webhook on a specific channel
 - [`delete_webhook`](): Delete a webhook
 - [`list_webhooks`](): List of webhooks on a specific channel
 - [`send_webhook_message`](): Send a message via webhook


<hr>

A more detailed examples can be found in the [Wiki](https://github.com/SaseQ/discord-mcp/wiki).
