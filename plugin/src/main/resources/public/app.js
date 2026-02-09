let currentConversationId = null;
let currentMode = 'PLANNING';
let authHeader = null;
let isTyping = false;

// Check for stored auth
const storedAuth = localStorage.getItem('cfm_auth');
if (storedAuth) {
    authHeader = storedAuth;
    document.addEventListener('DOMContentLoaded', () => showApp(true));
}

// Elements
const loginContainer = document.getElementById('login-container');
const appContainer = document.getElementById('app-container');
const conversationList = document.getElementById('conversation-list');
const chatMessages = document.getElementById('chat-messages');
const promptInput = document.getElementById('prompt-input');
const emptyState = document.getElementById('empty-state');
// New Elements
const chatHeader = document.getElementById('chat-header');
const modeBadge = document.getElementById('current-mode-badge');
const chatTitle = document.getElementById('chat-title');
const modeSwitchBtn = document.getElementById('mode-switch-btn');
const pasteCodeBtn = document.getElementById('paste-code-btn');

// Event Listeners
document.getElementById('login-btn').addEventListener('click', handleLogin);
document.getElementById('logout-btn').addEventListener('click', handleLogout);
document.getElementById('send-btn').addEventListener('click', handleSendMessage);
document.getElementById('new-chat-btn').addEventListener('click', resetChat);
modeSwitchBtn.addEventListener('click', handleModeSwitch);

// Auto-resize textarea
promptInput.addEventListener('input', function () {
    this.style.height = 'auto';
    this.style.height = (this.scrollHeight) + 'px';
});

// Enter to send
promptInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSendMessage();
    }
});

async function handleLogin() {
    const user = document.getElementById('username').value;
    const pass = document.getElementById('password').value;
    const errorMsg = document.getElementById('login-error');
    const loginBtn = document.getElementById('login-btn');

    if (!user || !pass) {
        errorMsg.innerText = "Both fields required";
        return;
    }

    loginBtn.disabled = true;
    loginBtn.innerText = "Connecting...";

    const header = 'Basic ' + btoa(`${user}:${pass}`);

    try {
        const res = await fetch('/api/health', { headers: { 'Authorization': header } });

        if (res.ok) {
            authHeader = header;
            localStorage.setItem('cfm_auth', header);
            showApp();
        } else {
            errorMsg.innerText = "Invalid credentials";
            loginBtn.disabled = false;
            loginBtn.innerText = "Access Portal";
        }
    } catch (e) {
        errorMsg.innerText = "Connection failed";
        loginBtn.disabled = false;
        loginBtn.innerText = "Access Portal";
    }
}

function handleLogout() {
    localStorage.removeItem('cfm_auth');
    location.reload();
}

function showApp(immediate = false) {
    if (immediate) {
        loginContainer.classList.add('hidden');
        appContainer.classList.remove('hidden');
    } else {
        setTimeout(() => {
            loginContainer.classList.add('hidden');
            appContainer.classList.remove('hidden');
        }, 100);
    }
    loadConversations();
}

async function loadConversations() {
    const userUuid = "admin-uuid"; // TODO: Get from auth if possible

    try {
        const res = await fetch(`/api/conversations?user_uuid=${userUuid}`, {
            headers: { 'Authorization': authHeader }
        });
        const data = await res.json();

        conversationList.innerHTML = '';
        if (data.length === 0) {
            conversationList.innerHTML = '<div style="padding: 20px; text-align: center; font-size: 12px; color: var(--gray-400);">No conversations</div>';
            return;
        }

        data.sort((a, b) => b.id - a.id).forEach(conv => {
            const item = document.createElement('div');
            item.className = `conv-item ${currentConversationId === conv.id ? 'active' : ''}`;
            item.textContent = conv.title || `Conversation #${conv.id}`;
            item.onclick = () => loadConversation(conv.id, conv.title, conv.currentMode);
            conversationList.appendChild(item);
        });
    } catch (e) {
        console.error("Failed to load conversations", e);
    }
}

async function loadConversation(id, title, mode) {
    if (currentConversationId === id) return;

    currentConversationId = id;
    currentMode = mode || 'PLANNING';

    emptyState.classList.add('hidden');
    chatHeader.classList.remove('hidden');
    chatTitle.innerText = title;

    updateUIForMode(currentMode);
    loadConversations(); // Update active state in list logic (simple re-render)

    chatMessages.innerHTML = '';

    // Ideally fetch messages here. For now just clear and show loaded.
    addMessage('assistant', `Loaded conversation: **${title}**`);
}

function updateUIForMode(mode) {
    modeBadge.innerText = mode;
    currentMode = mode;

    if (mode === 'PLANNING') {
        modeBadge.style.background = 'var(--accent-color)';
        modeSwitchBtn.innerText = "Switch to Build Mode";
        modeSwitchBtn.classList.remove('hidden');
        pasteCodeBtn.classList.add('hidden');
        promptInput.placeholder = "Refine your plan...";
    } else {
        modeBadge.style.background = '#e0aaff'; // Purple for Build
        modeSwitchBtn.innerText = "Back to Planning";
        modeSwitchBtn.classList.remove('hidden'); // Or hide if we don't want backtracking
        promptInput.placeholder = "Type 'Go' to generate build...";
    }
}

async function handleModeSwitch() {
    if (!currentConversationId) return;

    const newMode = currentMode === 'PLANNING' ? 'BUILDING' : 'PLANNING';

    // Optimistic UI update
    updateUIForMode(newMode);
    addMessage('system', `Switched to **${newMode}** mode.`);

    try {
        await fetch(`/api/conversations/${currentConversationId}/mode`, {
            method: 'POST',
            headers: {
                'Authorization': authHeader,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ mode: newMode })
        });
    } catch (e) {
        console.error("Failed to switch mode", e);
        addMessage('system', "Error syncing mode with server.");
    }
}

async function handleSendMessage() {
    const msg = promptInput.value.trim();
    if (!msg || isTyping) return;

    emptyState.classList.add('hidden');

    if (!currentConversationId) {
        await createConversation(msg);
    }

    addMessage('user', msg);
    promptInput.value = '';
    promptInput.style.height = 'auto';

    showTypingIndicator();

    try {
        const res = await fetch(`/api/conversations/${currentConversationId}/messages`, {
            method: 'POST',
            headers: {
                'Authorization': authHeader,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message: msg })
        });

        const data = await res.json();
        hideTypingIndicator();

        // Check if response is JSON (Build)
        if (data.response && (data.response.trim().startsWith('{') || data.response.trim().startsWith('```json'))) {
            handleBuildResponse(data.response);
        } else {
            addMessage('assistant', data.response);
        }

    } catch (e) {
        hideTypingIndicator();
        addMessage('assistant', "Error: Failed to communicate with server.");
    }
}

function handleBuildResponse(response) {
    // 1. Try to clean markdown
    let jsonStr = response;
    if (jsonStr.includes('```json')) {
        jsonStr = jsonStr.split('```json')[1].split('```')[0];
    } else if (jsonStr.includes('```')) {
        jsonStr = jsonStr.split('```')[1].split('```')[0];
    }

    addMessage('assistant', "Generated a new build configuration.");

    // Create a special Build Card
    const card = document.createElement('div');
    card.className = 'message assistant build-card';
    card.innerHTML = `
        <div class="build-preview">
            <h4>Build Ready</h4>
            <div class="actions">
                 <button onclick="copyToClipboard('/cfm paste ${currentConversationId}')" class="primary-btn small">Copy Paste Command</button>
            </div>
            <pre class="code-preview">${jsonStr.substring(0, 200)}...</pre>
        </div>
    `;

    chatMessages.appendChild(card);
    chatMessages.scrollTop = chatMessages.scrollHeight;

    // Also show the main paste button in header if in build mode
    if (currentMode === 'BUILDING') {
        pasteCodeBtn.classList.remove('hidden');
        pasteCodeBtn.onclick = () => copyToClipboard(`/cfm paste ${currentConversationId}`); // Assuming ID works, or we need BuildID? 
        // NOTE: The server currently returns just the raw JSON response strings. 
        // We ideally need the Build ID. 
        // For now, let's assume the /cfm paste command takes the conversation ID and finds the latest build? 
        // Actually CFMCommand.java expects Build ID. 
        // The ConversationService does NOT return the Build ID in the message response.
        // Quick fix: The user can use /cfm list to find builds? 
        // BETTER FIX: Users can just use the URL (localhost). 
        // OR: Update ConversationService to return a structured object { text: "...", buildId: 123 }. 
        // For now, let's use the copyToClipboard with a placeholder prompt or accept that the user might need to check /cfm list or use URL.
        // Actually, let's just use the copy clipboard for the JSON itself? No, too large.

        // Correction: We will use a generic message.
        pasteCodeBtn.innerText = "Copy Paste Command";
        pasteCodeBtn.onclick = () => {
            // We don't have the BuildID here easily without a separate fetch. 
            // Let's just alert the user.
            alert("Use /cfm list in-game to see the Build ID, then /cfm paste <id>");
        };
    }
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        alert("Copied to clipboard: " + text);
    });
}

async function createConversation(title) {
    try {
        const res = await fetch('/api/conversations', {
            method: 'POST',
            headers: {
                'Authorization': authHeader,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userUuid: "admin-uuid",
                userUsername: "admin",
                title: title.substring(0, 30),
                status: "ACTIVE",
                // metadata?
            })
        });
        const data = await res.json();
        currentConversationId = data.id;

        // Show header
        emptyState.classList.add('hidden');
        chatHeader.classList.remove('hidden');
        chatTitle.innerText = title;
        updateUIForMode('PLANNING'); // Default

        loadConversations();
    } catch (e) {
        console.error("Failed to create conversation", e);
    }
}

function addMessage(role, text) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${role}`;

    if (role === 'system') {
        msgDiv.innerHTML = `<em style="color:var(--gray-400); font-size: 0.9em;">${text}</em>`;
    } else {
        const formattedText = marked.parse(text);
        msgDiv.innerHTML = `
            <div class="message-role">${role}</div>
            <div class="message-content">${formattedText}</div>
        `;
    }

    chatMessages.appendChild(msgDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function showTypingIndicator() {
    isTyping = true;
    const indicator = document.createElement('div');
    indicator.id = 'typing-indicator';
    indicator.className = 'typing';
    indicator.innerHTML = `
        <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
        </div>
    `;
    chatMessages.appendChild(indicator);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function hideTypingIndicator() {
    isTyping = false;
    const el = document.getElementById('typing-indicator');
    if (el) el.remove();
}

function resetChat() {
    currentConversationId = null;
    chatMessages.innerHTML = '';
    emptyState.classList.remove('hidden');
    chatHeader.classList.add('hidden');
    loadConversations();
}
