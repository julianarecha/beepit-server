package com.beepit.server.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

@Controller
public class TestController {
    
    @Get("/")
    @Produces(MediaType.TEXT_HTML)
    public String index() {
        return testChat();
    }
    
    @Get("/test-chat")
    @Produces(MediaType.TEXT_HTML)
    public String testChat() {
        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Beepit Messenger</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        
        .container {
            width: 95%;
            max-width: 1200px;
            height: 90vh;
            background: white;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        
        .header {
            background: #FFC107;
            color: #000;
            padding: 15px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .header-title {
            font-size: 24px;
            font-weight: bold;
        }
        
        .header-user {
            font-size: 14px;
        }
        
        .screen {
            flex: 1;
            display: none;
            flex-direction: column;
        }
        
        .screen.active {
            display: flex;
        }
        
        /* Login Screen */
        .login-screen {
            justify-content: center;
            align-items: center;
            padding: 40px;
        }
        
        .login-form {
            width: 100%;
            max-width: 400px;
        }
        
        .login-form h2 {
            margin-bottom: 30px;
            text-align: center;
        }
        
        .login-form input {
            width: 100%;
            padding: 15px;
            margin: 10px 0;
            border: 2px solid #ddd;
            border-radius: 8px;
            font-size: 16px;
        }
        
        .login-form button {
            width: 100%;
            padding: 15px;
            margin: 10px 0;
            background: #FFC107;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: bold;
            cursor: pointer;
            transition: background 0.3s;
        }
        
        .login-form button:hover {
            background: #FFD54F;
        }
        
        .test-users {
            margin-top: 20px;
            padding: 15px;
            background: #f0f0f0;
            border-radius: 8px;
            font-size: 12px;
        }
        
        /* Contacts Screen */
        .contacts-screen {
            flex-direction: row;
        }
        
        .contacts-list {
            width: 300px;
            border-right: 1px solid #ddd;
            display: flex;
            flex-direction: column;
        }
        
        .contacts-header {
            padding: 15px;
            background: #f5f5f5;
            border-bottom: 1px solid #ddd;
            font-weight: bold;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .search-button {
            background: #FFC107;
            border: none;
            padding: 5px 15px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 12px;
            font-weight: bold;
        }
        
        .search-button:hover {
            background: #FFD54F;
        }
        
        .contacts-items {
            flex: 1;
            overflow-y: auto;
        }
        
        .search-modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.5);
            justify-content: center;
            align-items: center;
            z-index: 1000;
        }
        
        .search-modal.active {
            display: flex;
        }
        
        .search-modal-content {
            background: white;
            padding: 30px;
            border-radius: 15px;
            width: 90%;
            max-width: 500px;
            max-height: 70vh;
            overflow-y: auto;
        }
        
        .search-modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }
        
        .search-modal-header h3 {
            margin: 0;
        }
        
        .close-button {
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
            color: #666;
        }
        
        .search-input {
            width: 100%;
            padding: 12px;
            margin-bottom: 20px;
            border: 2px solid #ddd;
            border-radius: 8px;
            font-size: 15px;
        }
        
        .user-item {
            padding: 15px;
            border-bottom: 1px solid #eee;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .user-info {
            flex: 1;
        }
        
        .user-name {
            font-weight: bold;
            margin-bottom: 3px;
        }
        
        .user-id {
            font-size: 12px;
            color: #666;
        }
        
        .add-button {
            background: #4CAF50;
            color: white;
            border: none;
            padding: 8px 20px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 12px;
            font-weight: bold;
        }
        
        .add-button:hover {
            background: #45a049;
        }
        
        .add-button:disabled {
            background: #ccc;
            cursor: not-allowed;
        }
        
        .contact-item {
            padding: 15px;
            border-bottom: 1px solid #eee;
            cursor: pointer;
            transition: background 0.2s;
        }
        
        .contact-item:hover {
            background: #f9f9f9;
        }
        
        .contact-item.active {
            background: #FFF9C4;
        }
        
        .contact-name {
            font-weight: bold;
            margin-bottom: 5px;
        }
        
        .contact-status {
            font-size: 12px;
            color: #666;
        }
        
        .contact-online {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            display: inline-block;
            margin-right: 5px;
        }
        
        .contact-online.online {
            background: #4CAF50;
        }
        
        .contact-online.offline {
            background: #ccc;
        }
        
        /* Chat Area */
        .chat-area {
            flex: 1;
            display: flex;
            flex-direction: column;
        }
        
        .chat-header {
            padding: 15px 20px;
            background: #f5f5f5;
            border-bottom: 1px solid #ddd;
            font-weight: bold;
        }
        
        .no-chat {
            flex: 1;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #999;
            font-size: 18px;
        }
        
        .messages {
            flex: 1;
            padding: 20px;
            overflow-y: auto;
            background: #f9f9f9;
            display: none;
        }
        
        .messages.active {
            display: block;
        }
        
        .message {
            margin: 10px 0;
            padding: 12px;
            border-radius: 8px;
            max-width: 70%;
            word-wrap: break-word;
        }
        
        .message.own {
            background: #FFC107;
            margin-left: auto;
            text-align: right;
        }
        
        .message.other {
            background: white;
            border: 1px solid #ddd;
        }
        
        .message-sender {
            font-weight: bold;
            font-size: 12px;
            margin-bottom: 5px;
            color: #666;
        }
        
        .message-content {
            font-size: 15px;
        }
        
        .input-area {
            padding: 20px;
            background: white;
            border-top: 1px solid #ddd;
            display: none;
            gap: 10px;
        }
        
        .input-area.active {
            display: flex;
        }
        
        .input-area input {
            flex: 1;
            padding: 12px;
            border: 2px solid #ddd;
            border-radius: 8px;
            font-size: 15px;
        }
        
        .input-area button {
            padding: 12px 30px;
            background: #FFC107;
            border: none;
            border-radius: 8px;
            font-size: 15px;
            font-weight: bold;
            cursor: pointer;
            transition: background 0.3s;
        }
        
        .input-area button:hover {
            background: #FFD54F;
        }
        
        .error {
            color: red;
            font-size: 14px;
            margin-top: 10px;
            text-align: center;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="header-title">🐝 Beepit Messenger</div>
            <div class="header-user" id="headerUser"></div>
        </div>
        
        <!-- Login Screen -->
        <div class="screen login-screen active" id="loginScreen">
            <div class="login-form">
                <h2>Iniciar Sesión</h2>
                <input type="text" id="loginUsername" placeholder="Usuario" value="Alice">
                <input type="password" id="loginPassword" placeholder="Contraseña" value="password123">
                <button onclick="login()">Entrar</button>
                <div class="error" id="loginError"></div>
                <div class="test-users">
                    <strong>Usuarios de prueba:</strong><br>
                    Alice, Bob, Charlie, Diana, Eve<br>
                    <strong>Password:</strong> password123
                </div>
            </div>
        </div>
        
        <!-- Contacts Screen -->
        <div class="screen contacts-screen" id="contactsScreen">
            <div class="contacts-list">
                <div class="contacts-header">
                    <span>Contactos</span>
                    <button class="search-button" onclick="openSearchModal()">+ Agregar</button>
                </div>
                <div id="pendingMessages" style="display: none; padding: 10px; background: #FFF3CD; border-bottom: 2px solid #FFC107; cursor: pointer;" onclick="showPendingChats()">
                    <strong>📬 <span id="pendingCount">0</span> mensaje(s) nuevo(s)</strong>
                    <div style="font-size: 12px; color: #666;">De usuarios no contactos</div>
                </div>
                <div class="contacts-items" id="contactsList"></div>
            </div>
            <div class="chat-area">
                <div class="chat-header" id="chatHeader">
                    <span id="chatContactName">Selecciona un contacto</span>
                </div>
                <div class="no-chat" id="noChat">
                    Selecciona un contacto para comenzar a chatear
                </div>
                <div class="messages" id="messages"></div>
                <div class="input-area" id="inputArea">
                    <input type="text" id="messageInput" placeholder="Escribe un mensaje..." onkeypress="if(event.key==='Enter') sendMessage()">
                    <button onclick="sendMessage()">Enviar</button>
                </div>
            </div>
        </div>
        
        <!-- Search Modal -->
        <div class="search-modal" id="searchModal">
            <div class="search-modal-content">
                <div class="search-modal-header">
                    <h3>Buscar Usuarios</h3>
                    <button class="close-button" onclick="closeSearchModal()">×</button>
                </div>
                <input type="text" class="search-input" id="searchInput" placeholder="Buscar por nombre..." oninput="filterUsers()">
                <div id="usersList"></div>
            </div>
        </div>
        
        <!-- Pending Messages Modal -->
        <div class="search-modal" id="pendingModal">
            <div class="search-modal-content">
                <div class="search-modal-header">
                    <h3>📬 Mensajes Pendientes</h3>
                    <button class="close-button" onclick="closePendingModal()">×</button>
                </div>
                <div id="pendingChatsList"></div>
            </div>
        </div>
    </div>

    <script>
        let currentUser = null;
        let contacts = [];
        let allUsers = [];
        let pendingConversations = [];
        let selectedContact = null;
        let ws = null;
        let checkPendingInterval = null;

        async function login() {
            const username = document.getElementById('loginUsername').value.trim();
            const password = document.getElementById('loginPassword').value.trim();
            const errorDiv = document.getElementById('loginError');
            
            errorDiv.textContent = '';
            
            if (!username || !password) {
                errorDiv.textContent = 'Ingresa usuario y contraseña';
                return;
            }
            
            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });
                
                const data = await response.json();
                
                if (!response.ok) {
                    errorDiv.textContent = data.error || 'Error al iniciar sesión';
                    return;
                }
                
                currentUser = data;
                document.getElementById('headerUser').textContent = currentUser.username;
                document.getElementById('loginScreen').classList.remove('active');
                document.getElementById('contactsScreen').classList.add('active');
                
                await loadContacts();
                await loadAllUsers();
                await checkPendingMessages();
                
                // Check for pending messages every 5 seconds
                checkPendingInterval = setInterval(checkPendingMessages, 5000);
            } catch (error) {
                console.error('Error:', error);
                errorDiv.textContent = 'Error de conexión';
            }
        }
        
        async function loadContacts() {
            try {
                const response = await fetch(`/api/auth/contacts/${currentUser.userId}`);
                contacts = await response.json();
                
                const contactsList = document.getElementById('contactsList');
                contactsList.innerHTML = '';
                
                contacts.forEach(contact => {
                    const div = document.createElement('div');
                    div.className = 'contact-item';
                    div.onclick = () => selectContact(contact);
                    div.innerHTML = `
                        <div>
                            <span class="contact-online ${contact.online ? 'online' : 'offline'}"></span>
                            <span class="contact-name">${contact.username}</span>
                        </div>
                        <div class="contact-status">${contact.status}</div>
                    `;
                    contactsList.appendChild(div);
                });
            } catch (error) {
                console.error('Error cargando contactos:', error);
            }
        }
        
        async function loadAllUsers() {
            try {
                const response = await fetch('/api/auth/users');
                allUsers = await response.json();
            } catch (error) {
                console.error('Error cargando usuarios:', error);
            }
        }
        
        async function checkPendingMessages() {
            try {
                const response = await fetch(`/api/auth/conversations/${currentUser.userId}`);
                const conversations = await response.json();
                
                // Filter conversations from non-contacts with unread messages
                const contactIds = new Set(contacts.map(c => c.userId));
                
                pendingConversations = conversations.filter(conv => {
                    const isNonContact = !contactIds.has(conv.otherUserId);
                    const hasUnread = conv.unreadCount > 0;
                    return isNonContact && hasUnread;
                });
                
                if (pendingConversations.length > 0) {
                    const totalUnread = pendingConversations.reduce((sum, conv) => sum + conv.unreadCount, 0);
                    console.log('📬 Mensajes pendientes:', totalUnread);
                    document.getElementById('pendingCount').textContent = totalUnread;
                    document.getElementById('pendingMessages').style.display = 'block';
                } else {
                    document.getElementById('pendingMessages').style.display = 'none';
                }
            } catch (error) {
                console.error('Error verificando mensajes pendientes:', error);
            }
        }
        
        function showPendingChats() {
            const list = document.getElementById('pendingChatsList');
            list.innerHTML = '';
            
            pendingConversations.forEach(conv => {
                const user = allUsers.find(u => u.userId === conv.otherUserId);
                if (!user) return;
                
                const div = document.createElement('div');
                div.className = 'user-item';
                div.innerHTML = `
                    <div class="user-info">
                        <div class="user-name">${user.username} <span style="background: #FFC107; padding: 2px 8px; border-radius: 10px; font-size: 11px;">${conv.unreadCount}</span></div>
                        <div class="user-id" style="color: #666; margin-top: 5px;">${conv.lastMessage || 'Nuevo mensaje'}</div>
                    </div>
                    <div>
                        <button class="add-button" style="background: #4CAF50; margin-bottom: 5px;"
                                onclick="acceptAndChat('${user.userId}', '${user.username}')">
                            ✓ Aceptar y Chatear
                        </button>
                        <button class="add-button" style="background: #2196F3;"
                                onclick="openChatWithUserFromPending('${user.userId}', '${user.username}')">
                            💬 Solo Chatear
                        </button>
                    </div>
                `;
                list.appendChild(div);
            });
            
            document.getElementById('pendingModal').classList.add('active');
        }
        
        function closePendingModal() {
            document.getElementById('pendingModal').classList.remove('active');
        }
        
        async function acceptAndChat(userId, username) {
            await addContact(userId);
            closePendingModal();
            const user = { userId, username, online: false, status: "" };
            selectContact(user);
        }
        
        function openChatWithUserFromPending(userId, username) {
            closePendingModal();
            const user = { userId, username, online: false, status: "" };
            selectContact(user);
        }
        
        function openSearchModal() {
            document.getElementById('searchModal').classList.add('active');
            document.getElementById('searchInput').value = '';
            displayUsers(allUsers);
        }
        
        function closeSearchModal() {
            document.getElementById('searchModal').classList.remove('active');
        }
        
        function filterUsers() {
            const searchTerm = document.getElementById('searchInput').value.toLowerCase();
            const filtered = allUsers.filter(user => 
                user.username.toLowerCase().includes(searchTerm)
            );
            displayUsers(filtered);
        }
        
        function displayUsers(users) {
            const usersList = document.getElementById('usersList');
            usersList.innerHTML = '';
            
            const contactIds = new Set(contacts.map(c => c.userId));
            
            users.forEach(user => {
                if (user.userId === currentUser.userId) return; // Skip self
                
                const isContact = contactIds.has(user.userId);
                
                const div = document.createElement('div');
                div.className = 'user-item';
                div.innerHTML = `
                    <div class="user-info">
                        <div class="user-name">${user.username}</div>
                        <div class="user-id">${user.userId}</div>
                    </div>
                    <button class="add-button" 
                            onclick="addContact('${user.userId}')" 
                            ${isContact ? 'disabled' : ''}>
                        ${isContact ? '✓ Contacto' : '+ Agregar'}
                    </button>
                    <button class="add-button" style="background: #2196F3; margin-left: 5px;"
                            onclick="openChatWithUser('${user.userId}', '${user.username}')">
                        💬 Chat
                    </button>
                `;
                usersList.appendChild(div);
            });
        }
        
        async function addContact(contactUserId) {
            try {
                const response = await fetch('/api/auth/contacts/add', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        userId: currentUser.userId,
                        contactId: contactUserId
                    })
                });
                
                const data = await response.json();
                
                if (response.ok) {
                    alert('Contacto agregado exitosamente');
                    await loadContacts();
                    await checkPendingMessages();
                    closeSearchModal();
                } else {
                    alert(data.error || 'Error al agregar contacto');
                }
            } catch (error) {
                console.error('Error agregando contacto:', error);
                alert('Error al agregar contacto');
            }
        }
        
        function openChatWithUser(userId, username) {
            const user = {
                userId: userId,
                username: username,
                online: false,
                status: ""
            };
            
            closeSearchModal();
            selectContact(user);
        }
        
        function selectContact(contact) {
            selectedContact = contact;
            
            // Update UI
            document.querySelectorAll('.contact-item').forEach(item => {
                item.classList.remove('active');
            });
            if (event && event.currentTarget) {
                event.currentTarget.classList.add('active');
            }
            
            document.getElementById('chatContactName').textContent = contact.username;
            document.getElementById('noChat').style.display = 'none';
            document.getElementById('messages').classList.add('active');
            document.getElementById('inputArea').classList.add('active');
            document.getElementById('messages').innerHTML = '';
            
            // Connect WebSocket for private chat
            connectPrivateChat(contact);
        }
        
        function connectPrivateChat(contact) {
            if (ws) {
                ws.close();
            }
            
            const roomId = [currentUser.userId, contact.userId].sort().join('_');
            const wsUrl = `ws://localhost:8080/ws/chat/${roomId}?username=${encodeURIComponent(currentUser.username)}&userId=${encodeURIComponent(currentUser.userId)}`;
            
            console.log('Conectando a:', wsUrl);
            
            ws = new WebSocket(wsUrl);
            
            ws.onopen = () => {
                console.log('✅ WebSocket conectado con', contact.username);
            };
            
            ws.onmessage = (event) => {
                console.log('📩 Mensaje raw:', event.data);
                try {
                    const data = JSON.parse(event.data);
                    console.log('📦 Data type:', data.type);
                    
                    if (data.type === 'history') {
                        console.log('📜 Historial recibido:', data.messages.length, 'mensajes');
                        // Load message history
                        data.messages.forEach(msg => {
                            const isOwn = msg.senderId === currentUser.userId;
                            addMessage(isOwn ? currentUser.username : contact.username, msg.content, isOwn);
                        });
                    } else if (data.type === 'message') {
                        // New message received
                        const isOwn = data.sender === currentUser.userId;
                        addMessage(isOwn ? currentUser.username : contact.username, data.content, isOwn);
                        
                        // Check for pending messages if this is from a non-contact
                        if (!isOwn) {
                            setTimeout(checkPendingMessages, 500);
                        }
                    }
                } catch (e) {
                    console.error('❌ Error procesando mensaje:', e);
                }
            };
            
            ws.onerror = (error) => {
                console.error('Error de WebSocket:', error);
            };
            
            ws.onclose = () => {
                console.log('WebSocket desconectado');
            };
        }
        
        function sendMessage() {
            const input = document.getElementById('messageInput');
            const message = input.value.trim();
            
            if (!message || !ws || ws.readyState !== WebSocket.OPEN) {
                return;
            }
            
            try {
                ws.send(JSON.stringify({ content: message }));
                input.value = '';
            } catch (error) {
                console.error('Error al enviar mensaje:', error);
            }
        }
        
        function addMessage(sender, content, isOwn) {
            const messagesDiv = document.getElementById('messages');
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message ' + (isOwn ? 'own' : 'other');
            
            if (!isOwn) {
                const senderDiv = document.createElement('div');
                senderDiv.className = 'message-sender';
                senderDiv.textContent = sender;
                messageDiv.appendChild(senderDiv);
            }
            
            const contentDiv = document.createElement('div');
            contentDiv.className = 'message-content';
            contentDiv.textContent = content;
            messageDiv.appendChild(contentDiv);
            
            messagesDiv.appendChild(messageDiv);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }
    </script>
</body>
</html>
""";
    }
}
