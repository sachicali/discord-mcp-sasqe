<div align="center">
  <img src="assets/img/Discord_MCP_full_logo.svg" width="60%" alt="DeepSeek-V3" />
</div>
<hr>
<div align="center" style="line-height: 1;">
    <a href="https://smithery.ai/server/@saseq/discord-mcp-server" target="_blank" style="margin: 2px;">
        <img alt="Smithery Badge" src="https://camo.githubusercontent.com/ee5c6c6dc502821f4d57313b2885f7878af52be14142dd98526ea12aedf9b260/68747470733a2f2f736d6974686572792e61692f62616467652f40646d6f6e74676f6d65727934302f646565707365656b2d6d63702d736572766572" data-canonical-src="https://smithery.ai/badge/@saseq/discord-mcp-server" style="display: inline-block; vertical-align: middle;"/>
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
cd invoice-manager
mvn clean package
```

#### Configure Claude Desktop
```
{
  "mcpServers": {
    "bitcoin-mcp-server": {
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


## 🛠️ Available Tools

#### Message Management
 - [`send_message`](): Send a message to a specific channel

<hr>

A more detailed examples can be found in the [Wiki](https://github.com/SaseQ/discord-mcp/wiki).
