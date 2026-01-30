let currentConversationId = null;
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

// Event Listeners
document.getElementById('login-btn').addEventListener('click', handleLogin);
document.getElementById('logout-btn').addEventListener('click', handleLogout);
document.getElementById('send-btn').addEventListener('click', handleSendMessage);
document.getElementById('new-chat-btn').addEventListener('click', resetChat);

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
    const userUuid = "admin-uuid";

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
            item.textContent = conv.title;
            item.onclick = () => loadConversation(conv.id, conv.title);
            conversationList.appendChild(item);
        });
    } catch (e) {
        console.error("Failed to load conversations", e);
    }
}

async function loadConversation(id, title) {
    if (currentConversationId === id) return;

    currentConversationId = id;
    emptyState.classList.add('hidden');
    loadConversations();
    chatMessages.innerHTML = '';
    addMessage('assistant', `Loaded conversation: ${title}`);
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
        addMessage('assistant', data.response);
    } catch (e) {
        hideTypingIndicator();
        addMessage('assistant', "Error: Failed to communicate with server.");
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
        <div class="message-role">${role}</div>
        <div class="message-content">${formattedText}</div>
    `;

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
    loadConversations();
}
