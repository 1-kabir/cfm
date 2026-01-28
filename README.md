# Cursor for Minecraft (Gemini 3 Hackathon)

An AI-powered building engine that brings "Cursor-style" agentic building to the world of Minecraft. Describe it, snap it, build it. This is for a hackathon project based on the [Gemini 3 Hackathon](https://gemini3.devpost.com/).

## 🛠 Tech Stack
- **Frontend:** Static HTML/CSS/JS (served directly from plugin)
- **Web Server:** Embedded HTTP server in Java plugin
- **Database:** SQLite (file-based)
- **Minecraft Logic:** Java (Paper API)
- **Intelligence:** Google Gemini API (Multimodal & JSON Mode)

## 📁 Repository Structure
- `/plugin`: Java source code for the Paper plugin with embedded web server and database
- `/resources/web`: Static HTML/CSS/JS files served by the plugin

## Features

- If Pro is enabled, the user can talk to the model and plan out the build further so it understands better what the user wants (default mode is called Fast)
- Context management so it can handle changes to builds effectively
- Block data of every block in Minecraft
- Block states to understand North, South, East, West and place double blocks properly
- Web interface accessible at `http://[server-ip]:[port]` for build management
- Conversation history and build iteration tracking via SQLite database
- Shows particle effects for region selections
- All ingame commands will be locked behind `aibuild.admin` permission

## Testing Workflows

1. A basic workflow that requests for a VoxelJS schema based generation with default block as air
2. A more complex workflow that supports placing multiple blocks easily with default block as air
3. Test with zero shot, one shot, and few shot prompting on each for best results

## In-Game Commands

- /aibuild - Opens a help menu
- /aibuild help - Opens a help menu
- /aibuild tool - Gives the user a golden axe with which the user can set an area selection to generate the build in
- /aibuild create - Starts a build setup wizard which will guide the user through the process of setting up & creating a build
- /aibuild resume <buildID> - Lets the user resume a build creation wizard
- /aibuild schemagen <buildID> - Generates a .schema file for the latest iteration of the build
- /aibuild list - Lists all the builds that the user has created
- /aibuild workflow <workflowName> - Lets the user set a workflow for the build
- /aibuild parse - Takes the current selection and parses it into JSON
- /aibuild jsonparse <fileURL/path> - Takes a JSON file and parses it into a build

### /aibuild create Workflow

1. The user inputs a build prompt in NLP and/or an image/video URL of a build they wish to replicate
2. [OPTIONAL] The image/video is derived into text by Gemini and explained thoroughly
3. A prompt is then sent to the current available main workflow
4. The JSON schema is processed by the plugin
5. The schema is parsed based on the selection made ingame
6. Build progress and history are stored in the SQLite database
7. Web interface provides additional build management capabilities

## Web Interface

The plugin serves a web interface directly from the Minecraft server at `http://[server-ip]:[port]` (default port 8080):
- View and manage all builds
- Access conversation history with the AI
- Monitor build progress
- Configure build settings
- A modern UI, relatively simple, provides a ChatGPT/v0 styled interface to create new builds and choose dimensions, make changes etc..
- Simple dark/light mode toggle