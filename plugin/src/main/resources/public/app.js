/**
 * CFM Neural Portal - Interface Control
 * Pure, high-performance logic for the architectural dashboard
 */

let currentConversationId = null;
let authHeader = null;
let isTyping = false;

// Initialize Lucide Icons
const refreshIcons = () => lucide.createIcons();

// Auth Bridge
const storedAuth = localStorage.getItem('cfm_auth');
if (storedAuth) {
    authHeader = storedAuth;
    // Immediate app entry if already authorized
    document.addEventListener('DOMContentLoaded', () => {
        showApp(true);
    });
}

// UI State Selectors
const loginContainer = document.getElementById('login-container');
const appContainer = document.getElementById('app-container');
const conversationList = document.getElementById('conversation-list');
const chatMessages = document.getElementById('chat-messages');
const promptInput = document.getElementById('prompt-input');
const emptyState = document.getElementById('empty-state');
const currentTitle = document.getElementById('current-title');

// Primary Handlers
document.getElementById('login-btn').addEventListener('click', handleLogin);
document.getElementById('logout-btn').addEventListener('click', handleLogout);
document.getElementById('send-btn').addEventListener('click', handleSendMessage);
document.getElementById('new-chat-btn').addEventListener('click', () => resetChat());

// Textarea Auto-Expansion
promptInput.addEventListener('input', function () {
    this.style.height = 'auto';
    this.style.height = (this.scrollHeight) + 'px';
});

// Structural Instruction Submission
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
        shakeElement(loginBtn);
        errorMsg.innerText = "Identity and security key required.";
        return;
    }

    loginBtn.disabled = true;
    loginBtn.innerText = "Establishing Link...";

    const header = 'Basic ' + btoa(`${user}:${pass}`);

    try {
        const res = await fetch('/api/health', {
            headers: { 'Authorization': header }
        });

        if (res.ok) {
            authHeader = header;
            localStorage.setItem('cfm_auth', header);
            showApp();
        } else {
            shakeElement(loginBtn);
            errorMsg.innerText = "Security check failed. Access denied.";
            loginBtn.disabled = false;
            loginBtn.innerText = "Initialize Link";
        }
    } catch (e) {
        errorMsg.innerText = "Critical connection failure. Portal unreachable.";
        loginBtn.disabled = false;
        loginBtn.innerText = "Initialize Link";
    }
}

function handleLogout() {
    localStorage.removeItem('cfm_auth');
    location.reload();
}

function showApp(immediate = false) {
    if (immediate) {
        loginContainer.style.display = 'none';
        appContainer.classList.remove('hidden');
        appContainer.style.opacity = '1';
        appContainer.style.transform = 'scale(1)';
        appContainer.style.pointerEvents = 'auto';
    } else {
        loginContainer.style.opacity = '0';
        loginContainer.style.transform = 'scale(1.1)';
        setTimeout(() => {
            loginContainer.style.display = 'none';
            appContainer.classList.remove('hidden');
            // Trigger reflow for transition
            requestAnimationFrame(() => {
                appContainer.style.opacity = '1';
                appContainer.style.transform = 'scale(1)';
                appContainer.style.pointerEvents = 'auto';
            });
        }, 500);
    }
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
            conversationList.innerHTML = '<div class="p-6 text-center text-[10px] text-[#3f3f46] font-bold uppercase tracking-widest">No previous matrices</div>';
            return;
        }

        data.sort((a, b) => b.id - a.id).forEach(conv => {
            const item = document.createElement('div');
            item.className = `conv-item w-full flex items-center justify-between px-4 py-3 rounded-xl cursor-pointer text-sm mb-1 border border-transparent ${currentConversationId === conv.id ? 'active' : 'hover:bg-[#121214]'}`;
            item.innerHTML = `
                <div class="flex items-center gap-3 truncate">
                    <i data-lucide="layers" class="w-4 h-4 text-[#52525b]"></i>
                    <span class="truncate font-semibold text-[#fafafa]">${conv.title}</span>
                </div>
            `;
            item.onclick = () => loadConversation(conv.id, conv.title);
            conversationList.appendChild(item);
        });
        refreshIcons();
    } catch (e) {
        console.error("Matrix load failure", e);
    }
}

async function loadConversation(id, title) {
    if (currentConversationId === id) return;

    currentConversationId = id;
    currentTitle.innerText = title;
    emptyState.classList.add('hidden');
    loadConversations();

    chatMessages.innerHTML = '';

    // Smooth scroll to top when changing
    chatMessages.scrollTo({ top: 0, behavior: 'smooth' });

    // Simulate construction log
    addMessage('assistant', `Constructing context from session **#${id}**. Matrix ready.`);
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
        addMessage('assistant', "Neural link severed. Verify server status and configuration.");
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
        console.error("Matrix instantiation failed", e);
    }
}

function addMessage(role, text) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${role}`;

    const formattedText = marked.parse(text);

    msgDiv.innerHTML = `
        <div class="avatar">${role === 'user' ? 'CF' : 'M'}</div>
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
    indicator.className = 'message assistant animate-in fade-in slide-in-from-bottom-2 duration-300';
    indicator.innerHTML = `
        <div class="avatar">M</div>
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
    currentTitle.innerText = "Operations Dashboard";
    chatMessages.innerHTML = '';
    emptyState.classList.remove('hidden');
    loadConversations();
}

function setPrompt(text) {
    promptInput.value = text;
    promptInput.focus();
    promptInput.dispatchEvent(new Event('input'));
}

function shakeElement(el) {
    el.classList.add('translate-x-1');
    setTimeout(() => el.classList.remove('translate-x-1'), 100);
}
