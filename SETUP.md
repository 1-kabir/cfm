# Cursor for Minecraft Setup Guide

Welcome to the Cursor for Minecraft project! This guide will help you set up and run the project locally.

## Project Overview

Cursor for Minecraft is an AI-powered building engine that brings "Cursor-style" agentic building to the world of Minecraft. It allows users to describe structures, snap images, and have them built automatically in the Minecraft world.

### Tech Stack
- **Frontend:** Static HTML/CSS/JS (served directly from plugin)
- **Minecraft Plugin:** Java (Paper API) with embedded HTTP server
- **Database:** SQLite (file-based, optional MySQL support)
- **AI Integration:** Google Gemini API

### Repository Structure
- `/plugin`: Java source code for the Paper plugin with embedded web server
- `/resources/web`: Static HTML/CSS/JS files served by the plugin

## Prerequisites

Before setting up the project, ensure you have the following installed:

### Plugin Dependencies
- [Java JDK](https://openjdk.org/) (version 21)
- [Gradle](https://gradle.org/) (optional, project includes Gradle wrapper)

## Setting Up the Minecraft Plugin

1. Navigate to the plugin directory:
   ```bash
   cd plugin
   ```

2. Build the plugin JAR file:
   ```bash
   # On Windows
   ./gradlew.bat build

   # On macOS/Linux
   ./gradlew build
   ```

3. The compiled JAR file will be located in `plugin/build/libs/`

4. Copy the JAR file to your Minecraft server's `plugins` folder.

5. Start your Paper/Spigot Minecraft server (version 1.21.3 or compatible).

## Development Setup

### IDE Configuration

If you're experiencing import errors in your IDE, follow these steps:

1. **Import as Gradle Project**: Open your IDE and import the `plugin` directory as a Gradle project, not as a plain Java project.

2. **Refresh Gradle Dependencies**:
   ```bash
   ./gradlew clean build
   ```

3. **IDE-Specific Setup**:
   - **IntelliJ IDEA**:
     - Import project as Gradle project
     - Enable annotation processing: `Settings > Build > Compiler > Annotation Processors`
     - Install Lombok plugin and enable annotation processing

   - **VS Code**:
     - Install Extension Pack for Java
     - Install Gradle for Java extension
     - Ensure Java 21 is selected as the project JDK

   - **Eclipse**:
     - Import as Gradle project via File > Import > Gradle > Existing Gradle Project
     - Install Lombok by downloading the jar and running `java -jar lombok.jar`

4. **Verify Dependencies Are Resolved**:
   - Paper API (compileOnly) - provides Bukkit/Minecraft classes
   - WorldEdit API (compileOnly) - provides WorldEdit classes
   - Other libraries (implementation) - for AI, web, database functionality

### Common Import Issues & Solutions

- **Bukkit/Minecraft classes not found**: Ensure `compileOnly 'io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT'` is properly resolved in build.gradle
- **WorldEdit classes not found**: Verify `compileOnly 'com.sk89q.worldedit:worldedit-bukkit:7.4.0'` is available
- **Lombok annotations not processed**: Make sure annotation processor is enabled in your IDE
- **Other third-party libraries**: Check that all dependencies in build.gradle are properly downloaded

## Frontend Development

The frontend is served directly from the plugin at runtime. To develop the frontend:

1. Edit the files in `plugin/src/main/resources/web/`:
   - `index.html` - Main page
   - `css/styles.css` - Styles
   - `js/main.js` - JavaScript logic
   - Other static assets

2. Rebuild the plugin after making changes:
   ```bash
   ./gradlew build
   ```

3. Restart your Minecraft server to load the updated plugin

## Running the Full Application

1. Start your Minecraft server with the plugin installed.

2. The web interface will be available at `http://[server-ip]:[port]` (default port is 8080):
   - If running locally: `http://localhost:8080`
   - If running on a server: `http://[server-ip]:8080`

3. The web interface communicates directly with the plugin via embedded API endpoints.

## Development Commands

### Plugin Commands
- `./gradlew build` - Compile and package the plugin with web assets
- `./gradlew clean` - Clean build artifacts
- `./gradlew jar` - Create JAR file without running tests
- `./gradlew dependencies` - Show dependency tree
- `./gradlew idea` - Generate IntelliJ IDEA project files
- `./gradlew eclipse` - Generate Eclipse project files

## Configuration

### Web Interface Port
The web interface port can be configured in the plugin's configuration. By default, it runs on port 8080.

### Database Configuration
The plugin uses SQLite by default (file-based, requires no setup). To use MySQL instead:
1. Update the plugin configuration
2. Ensure MySQL server is accessible
3. Update the database connection settings

## Environment Variables

### Plugin
Create a `config.yml` file in your server's plugin configuration directory with the following variables:

```yaml
web_interface:
  enabled: true
  port: 8080
  allowed_ips: []

database:
  type: sqlite  # or mysql
  sqlite:
    file: data.db
  mysql:
    host: localhost
    port: 3306
    database: cursor_minecraft
    username: your_username
    password: your_password

ai:
  gemini_api_key: your_gemini_api_key_here
```

## Troubleshooting

### Common Issues

1. **Import errors in IDE**: Follow the IDE Configuration section above to properly set up the Gradle project.

2. **Plugin won't compile**: Ensure you have Java 21 installed and properly configured, then run `./gradlew clean build`.

3. **Web interface not accessible**: Verify that your Minecraft server is running with the plugin installed and the web interface port is not blocked by firewall.

4. **API Key issues**: Make sure your Google Gemini API key is valid and has sufficient quota.

5. **Port conflicts**: Ensure the configured web interface port is available.

6. **Database connection issues**: Check database credentials and connectivity if using MySQL.

7. **Dependency resolution issues**: Run `./gradlew dependencies` to check if all dependencies are properly resolved.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Commit your changes (`git commit -m 'Add amazing feature'`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

## Additional Notes

- The plugin requires Paper API version 1.21.11
- The web interface is served directly from the plugin JAR
- SQLite is the default database (no additional setup required)
- The web interface communicates with the plugin via embedded HTTP endpoints