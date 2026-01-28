let currentConversationId = null;
let authHeader = null;

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

// Event Listeners
document.getElementById('login-btn').addEventListener('click', handleLogin);
document.getElementById('logout-btn').addEventListener('click', handleLogout);
document.getElementById('send-btn').addEventListener('click', handleSendMessage);
document.getElementById('new-chat-btn').addEventListener('click', () => resetChat());

async function handleLogin() {
    const user = document.getElementById('username').value;
    const pass = document.getElementById('password').value;
    const errorMsg = document.getElementById('login-error');

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
            errorMsg.innerText = "Invalid credentials";
        }
    } catch (e) {
        errorMsg.innerText = "Connection failed";
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
}

async function loadConversations() {
    // For now we use a placeholder UUID, in reality we might get it from server/auth
    const userUuid = "admin-uuid";

    try {
        const res = await fetch(`/api/conversations?user_uuid=${userUuid}`, {
            headers: { 'Authorization': authHeader }
        });
        const data = await res.json();

        conversationList.innerHTML = '';
        data.forEach(conv => {
            const item = document.createElement('div');
            item.className = `conv-item ${currentConversationId === conv.id ? 'active' : ''}`;
            item.innerHTML = `<i class="far fa-message"></i> <span>${conv.title}</span>`;
            item.onclick = () => loadConversation(conv.id);
            conversationList.appendChild(item);
        });
    } catch (e) {
        console.error("Failed to load conversations", e);
    }
}

async function loadConversation(id) {
    currentConversationId = id;
    loadConversations(); // Refresh list to show active

    // Reset chat messages
    chatMessages.innerHTML = '';

    try {
        // Load messages (TODO: implement message history in DB/API)
        // For now just show "Conversation loaded"
        addMessage('assistant', `Loaded conversation ${id}. You can continue describing your build.`);
    } catch (e) {
        console.error("Failed to load conversation", e);
    }
}

async function handleSendMessage() {
    const msg = promptInput.value.trim();
    if (!msg) return;

    if (!currentConversationId) {
        // Create new conversation first
        await createConversation(msg);
    }

    addMessage('user', msg);
    promptInput.value = '';

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
        addMessage('assistant', data.response);
    } catch (e) {
        addMessage('assistant', "Error: Failed to connect to AI.");
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
    msgDiv.innerHTML = `
        <div class="message-content">
            <div class="avatar">${role === 'user' ? 'U' : 'AI'}</div>
            <div class="text">${text}</div>
        </div>
    `;
    chatMessages.appendChild(msgDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function resetChat() {
    currentConversationId = null;
    chatMessages.innerHTML = `
        <div class="welcome-screen">
            <div class="logo-large">C</div>
            <h1>What should we build today?</h1>
        </div>
    `;
    loadConversations();
}

function setPrompt(text) {
    promptInput.value = text;
    promptInput.focus();
}
