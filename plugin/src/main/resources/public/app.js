/**
 * Cursor for Minecraft - Core Logic
 * High-performance, modern AI chat interface
 */

let currentConversationId = null;
let authHeader = null;
let isTyping = false;

// Initialize Lucide Icons
const refreshIcons = () => lucide.createIcons();

// Auth Check
const storedAuth = localStorage.getItem('cursor_auth');
if (storedAuth) {
    authHeader = storedAuth;
    showApp();
}

// UI Elements
const loginContainer = document.getElementById('login-container');
const appContainer = document.getElementById('app-container');
const conversationList = document.getElementById('conversation-list');
const chatMessages = document.getElementById('chat-messages');
const promptInput = document.getElementById('prompt-input');
const emptyState = document.getElementById('empty-state');
const currentTitle = document.getElementById('current-title');

// Event Listeners
document.getElementById('login-btn').addEventListener('click', handleLogin);
document.getElementById('logout-btn').addEventListener('click', handleLogout);
document.getElementById('send-btn').addEventListener('click', handleSendMessage);
document.getElementById('new-chat-btn').addEventListener('click', () => resetChat());

// Auto-resize textarea
promptInput.addEventListener('input', function () {
    this.style.height = 'auto';
    this.style.height = (this.scrollHeight) + 'px';
});

// Handle Enter to send
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
        errorMsg.innerText = "Please fill in all fields.";
        return;
    }

    loginBtn.disabled = true;
    loginBtn.innerText = "Authenticating...";

    const header = 'Basic ' + btoa(`${user}:${pass}`);

    try {
        const res = await fetch('/api/health', {
            headers: { 'Authorization': header }
        });

        if (res.ok) {
            authHeader = header;
            localStorage.setItem('cursor_auth', header);
            showApp();
        } else {
            errorMsg.innerText = "Invalid credentials. Try again.";
            loginBtn.disabled = false;
            loginBtn.innerText = "Sign In";
        }
    } catch (e) {
        errorMsg.innerText = "Could not connect to server.";
        loginBtn.disabled = false;
        loginBtn.innerText = "Sign In";
    }
}

function handleLogout() {
    localStorage.removeItem('cursor_auth');
    location.reload();
}

function showApp() {
    loginContainer.classList.add('hidden');
    appContainer.classList.remove('hidden');
    loadConversations();
    refreshIcons();
}

async function loadConversations() {
    const userUuid = "admin-uuid";

    try {
        const res = await fetch(`/api/conversations?user_uuid=${userUuid}`, {
            headers: { 'Authorization': authHeader }
        });
        const data = await res.json();

        conversationList.innerHTML = '';
        if (data.length === 0) {
            conversationList.innerHTML = '<div class="p-4 text-center text-xs text-[#52525b]">No builds yet</div>';
            return;
        }

        data.sort((a, b) => b.id - a.id).forEach(conv => {
            const item = document.createElement('div');
            item.className = `conv-item w-full flex items-center justify-between px-3 py-2 rounded-lg cursor-pointer text-sm mb-1 border border-transparent ${currentConversationId === conv.id ? 'active' : 'hover:bg-[#18181b]'}`;
            item.innerHTML = `
                <div class="flex items-center gap-2 truncate">
                    <i data-lucide="message-square" class="w-4 h-4 text-[#52525b]"></i>
                    <span class="truncate font-medium">${conv.title}</span>
                </div>
            `;
            item.onclick = () => loadConversation(conv.id, conv.title);
            conversationList.appendChild(item);
        });
        refreshIcons();
    } catch (e) {
        console.error("Failed to load conversations", e);
    }
}

async function loadConversation(id, title) {
    currentConversationId = id;
    currentTitle.innerText = title;
    emptyState.classList.add('hidden');
    loadConversations(); // Update active state

    chatMessages.innerHTML = '';

    try {
        // In a real app, you'd fetch message history here
        addMessage('assistant', `Loaded build session: **${title}**. I'm ready to continue or modify this structure.`);
    } catch (e) {
        console.error("Failed to load conversation", e);
    }
}

async function handleSendMessage() {
    const msg = promptInput.value.trim();
    if (!msg || isTyping) return;

    if (emptyState) emptyState.classList.add('hidden');

    if (!currentConversationId) {
        currentTitle.innerText = msg.substring(0, 30);
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
        addMessage('assistant', data.response);
    } catch (e) {
        hideTypingIndicator();
        addMessage('assistant', "Error: Connection lost. Ensure the plugin is still running.");
    }
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
                status: "ACTIVE"
            })
        });
        const data = await res.json();
        currentConversationId = data.id;
        loadConversations();
    } catch (e) {
        console.error("Failed to create conversation", e);
    }
}

function addMessage(role, text) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${role}`;

    const formattedText = marked.parse(text);

    msgDiv.innerHTML = `
        <div class="avatar">${role === 'user' ? 'U' : 'C'}</div>
        <div class="message-content">
            <div class="message-bubble">
                ${formattedText}
            </div>
        </div>
    `;

    chatMessages.appendChild(msgDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function showTypingIndicator() {
    isTyping = true;
    const indicator = document.createElement('div');
    indicator.id = 'typing-indicator-wrapper';
    indicator.className = 'message assistant';
    indicator.innerHTML = `
        <div class="avatar">C</div>
        <div class="message-content">
            <div class="typing-indicator">
                <div class="dot"></div>
                <div class="dot"></div>
                <div class="dot"></div>
            </div>
        </div>
    `;
    chatMessages.appendChild(indicator);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function hideTypingIndicator() {
    isTyping = false;
    const el = document.getElementById('typing-indicator-wrapper');
    if (el) el.remove();
}

function resetChat() {
    currentConversationId = null;
    currentTitle.innerText = "New Build";
    chatMessages.innerHTML = '';
    emptyState.classList.remove('hidden');
    loadConversations();
}

function setPrompt(text) {
    promptInput.value = text;
    promptInput.focus();
    promptInput.style.height = 'auto';
    promptInput.style.height = (promptInput.scrollHeight) + 'px';
}
