# Beepit Messenger

Aplicación de mensajería instantánea en tiempo real construida con arquitectura reactiva y modelo de actores.

## Descripción

Beepit es un mensajero moderno que permite comunicación 1-a-1 entre usuarios con historial persistente, notificaciones de mensajes pendientes y gestión de contactos. El sistema está diseñado con arquitectura de actores usando Akka para alta concurrencia y escalabilidad.

## Tecnologías

- **Java 25** (Amazon Corretto 25.0.1_8)
- **Micronaut 4.6.1** - Framework reactivo
- **Akka Actor Typed 2.8.8** - Modelo de actores para concurrencia
- **Scala 3.2.2** - Runtime para Akka
- **Project Reactor** - Programación reactiva
- **Micronaut Serde** - Serialización JSON
- **Bean Validation** - Validación de entrada
- **Thymeleaf** - Motor de plantillas
- **WebSocket** - Comunicación en tiempo real
- **Resilience4j** - Rate limiting
- **Logback + Logstash** - Logging estructurado
- **Gradle 9.2.1** - Sistema de construcción
- **JUnit 5 + Akka TestKit** - Testing

## Clientes Compatibles

Este backend funciona con:
- ✅ **Cliente Kotlin Multiplatform** (Desktop, Android, Web)
- ✅ **Cliente Web Thymeleaf** (navegador, `/test-chat`)
- ✅ Cualquier cliente HTTP/WebSocket compatible

## Arquitectura

### Modelo de Actores (Akka)

El sistema utiliza **3 ActorSystems independientes** para simplicidad y aislamiento:

1. **ChatRoomActor** (`chat-room-system`)
   - Gestión de salas de chat grupales (legacy, no usado actualmente)

2. **UserManagerActor** (`user-manager-system`)
   - Registro y autenticación de usuarios
   - Gestión de contactos
   - Estado online/offline
   - Usuarios pre-cargados: Alice, Bob, Charlie, Diana, Eve

3. **ConversationManagerActor** (`conversation-manager-system`)
   - Gestión de conversaciones 1-a-1
   - Almacenamiento de mensajes en memoria
   - Historial de conversaciones
   - Estado de mensajes (entregado/leído)

**Scheduler Único**: Todos los actores comparten el mismo scheduler del sistema para optimizar recursos.

### Flujo de Datos

```
Cliente (WebSocket) 
    ↓
ChatWebSocketServerWithAkka
    ↓
ConversationManagerActor (Ask Pattern)
    ↓
ConcurrentHashMap (Almacenamiento en memoria)
    ↓
Broadcast a todos los participantes
```

## Características

### Mensajería
- Chat privado 1-a-1 en tiempo real
- Historial de mensajes persistente (en memoria)
- Los mensajes se guardan incluso si el destinatario está offline
- Carga automática del historial al abrir una conversación
- Broadcast optimizado con Map de sesiones por usuario
- Rate limiting: 10 mensajes por segundo por usuario
- Validación de longitud de mensajes (máximo 5000 caracteres)

### Gestión de Usuarios
- Sistema de login/registro con validación
- Lista de contactos personalizada
- Buscador de usuarios
- Agregar contactos desde el buscador
- Chat directo sin necesidad de agregar como contacto
- Validación de username (3-20 caracteres) y password (mínimo 6)

### Notificaciones
- Notificación visual de mensajes pendientes
- Contador de mensajes no leídos
- Alertas de mensajes de no-contactos
- Modal con opciones: "Aceptar y Chatear" o "Solo Chatear"
- Polling automático cada 5 segundos

### Seguridad y Performance
- Rate limiting con Resilience4j
- Timeouts configurables por operación (2-5 segundos)
- Logging estructurado con MDC (userId, sessionId, conversationId)
- Gestión automática de limpieza de sesiones
- Manejo de errores con @OnError y @OnClose

## Instalación y Ejecución

### Requisitos Previos
- **JDK 25+** (Amazon Corretto 25 recomendado)
- **Gradle 9.2.1** (incluido en wrapper)

### 1. Navegar al Directorio del Servidor

```bash
cd ruta\al\directorio\beepit-server
```

### 2. Compilar el Servidor

```bash
.\gradlew.bat clean build
```

Para compilar sin ejecutar tests:
```bash
.\gradlew.bat clean build -x test
```

Para ejecutar solo los tests:
```bash
.\gradlew.bat test
```

### 3. Ejecutar el Servidor

```bash
.\gradlew.bat run
```

El servidor se iniciará en: `http://localhost:8080`

### 4. Verificar el Estado

Endpoints de health:
- **HTTP**: `http://localhost:8080/health`
- **WebSocket Test**: `http://localhost:8080/test-chat`

### 5. Conectar Clientes

**Cliente Kotlin Multiplatform:**
```bash
cd ..\beepit
.\gradlew.bat run           # Desktop
.\gradlew.bat installDebug  # Android
.\gradlew.bat jsBrowserDevelopmentRun  # Web
```

**Cliente Web (Thymeleaf):**
Navegar a: `http://localhost:8080/test-chat`

## Usuarios de Prueba

El sistema viene con 5 usuarios pre-configurados (sin contactos):

| Usuario  | Contraseña   |
|----------|--------------|
| alice    | password123  |
| bob      | password123  |
| charlie  | password123  |
| diana    | password123  |
| eve      | password123  |

**Nota**: Todos los usuarios comienzan sin contactos. Usa el buscador para agregar contactos.

## API REST

### Autenticación

#### Registrar Usuario
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "nuevo_usuario",
  "password": "contraseña_segura"
}
```

**Respuesta exitosa**:
```json
{
  "userId": "uuid-generado",
  "username": "nuevo_usuario",
  "contacts": [],
  "createdAt": "2024-01-15T10:30:00Z",
  "online": false
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "password123"
}
```

**Respuesta exitosa**:
```json
{
  "userId": "alice-uuid",
  "username": "alice",
  "contacts": ["bob-uuid", "charlie-uuid"],
  "createdAt": "2024-01-15T10:30:00Z",
  "online": true
}
```

**Respuesta de error**:
```json
{
  "error": "Invalid username or password"
}
```

### Gestión de Contactos

#### Obtener Contactos
```http
GET /api/auth/contacts/{userId}
```

**Respuesta**:
```json
[
  {
    "userId": "bob-uuid",
    "username": "bob",
    "statusMessage": "Available",
    "online": true
  }
]
```

#### Buscar Usuarios
```http
GET /api/auth/users?query=bob
```

**Respuesta**:
```json
[
  {
    "userId": "bob-uuid",
    "username": "bob",
    "statusMessage": "Available",
    "online": true
  }
]
```

#### Agregar Contacto
```http
POST /api/auth/contacts/add
Content-Type: application/json

{
  "userId": "alice-uuid",
  "contactUserId": "bob-uuid"
}
```

**Respuesta exitosa**:
```json
{
  "success": true,
  "message": "Contact added successfully"
}
```

### Conversaciones

#### Obtener Conversaciones del Usuario
```http
GET /api/auth/conversations/{userId}
```

**Respuesta** (formato actualizado para Kotlin client):
```json
[
  {
    "conversationId": "alice-uuid_bob-uuid",
    "otherUserId": "bob-uuid",
    "isContact": true,
    "unreadCount": 2,
    "lastMessage": "Hola Alice!"
  }
]
```

**Descripción de campos**:
- `conversationId`: ID único de la conversación (formato: userId1_userId2 ordenados)
- `otherUserId`: ID del otro participante
- `isContact`: true si es contacto, false si es mensaje de no-contacto
- `unreadCount`: Cantidad de mensajes no leídos
- `lastMessage`: Último mensaje de la conversación (puede ser null)
          "timestamp": "2024-01-15T10:35:00Z",
          "delivered": true,
          "read": false
        }
      ],
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:35:00Z"
    },
    "otherParticipant": {
      "userId": "bob-uuid",
      "username": "bob",
      "statusMessage": "Available",
      "online": true
    },
    "unreadCount": 1,
    "isContact": true
  }
]
```

## WebSocket

### Conexión

**Formato de URL:**
```
ws://localhost:8080/ws/chat/{roomId}?userId={userId}&username={username}
```

**Parámetros**:
- `roomId`: ID de la sala de chat (formato: `userId1_userId2` ordenados alfabéticamente)
- `userId`: ID del usuario actual (query param)
- `username`: Nombre del usuario (query param)

**Ejemplo desde JavaScript:**
```javascript
const roomId = "alice-uuid_bob-uuid";
const socket = new WebSocket(
  `ws://localhost:8080/ws/chat/${roomId}?userId=alice-uuid&username=alice`
);
```

**Ejemplo desde Kotlin (Cliente Multiplatform):**
```kotlin
val roomId = listOf(userId1, userId2).sorted().joinToString("_")
wsManager.connect(
    roomId = roomId,
    userId = currentUserId,
    username = currentUsername
)
```

**Nota Android**: El emulador debe usar `ws://10.0.2.2:8080` en lugar de `ws://localhost:8080`

### Eventos

#### onopen - Conexión establecida
```javascript
socket.onopen = () => {
  console.log('✅ WebSocket conectado');
};
```

Al conectar, el servidor envía automáticamente el historial de mensajes.

#### onmessage - Recibir mensajes
```javascript
socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  
  if (data.type === 'history') {
    // Historial de mensajes
    console.log('📜 Historial recibido:', data.messages.length);
    data.messages.forEach(msg => {
      // Procesar cada mensaje
    });
  } else {
    // Nuevo mensaje individual
    const message = {
      senderId: data.senderId,
      content: data.content,
      timestamp: data.timestamp
    };
  }
};
```

#### Enviar mensaje

**Desde JavaScript:**
```javascript
const message = {
  content: messageText
};
socket.send(JSON.stringify(message));
```

**Desde Kotlin:**
```kotlin
wsManager.sendMessage("Hola!")
```

**Nota**: Solo se envía el contenido. El servidor agrega automáticamente senderId, recipientId, messageId y timestamp.

#### onerror - Manejo de errores
```javascript
socket.onerror = (error) => {
  console.error('❌ Error en WebSocket:', error);
};
```

#### onclose - Desconexión
```javascript
socket.onclose = () => {
  console.log('WebSocket desconectado');
};
```

### Formato de Mensajes

#### Mensaje Nuevo (Cliente → Servidor)
```json
{
  "content": "Hola Bob!"
}
```

#### Mensaje Nuevo (Servidor → Cliente)
```json
{
  "type": "message",
  "messageId": "uuid-generado",
  "senderId": "alice-uuid",
  "recipientId": "bob-uuid",
  "content": "Hola Bob!",
  "timestamp": "2024-01-15T10:35:00.123Z"
}
```

#### Historial (Servidor → Cliente al conectar)
```json
{
  "type": "history",
  "messages": [
    {
      "messageId": "msg-uuid-1",
      "senderId": "bob-uuid",
      "recipientId": "alice-uuid",
      "content": "Hola Alice!",
      "timestamp": "2024-01-15T10:30:00.456Z",
      "delivered": true,
      "read": false
    }
  ]
}
```

## Estructura del Proyecto

```
beepit-server/
├── src/main/java/com/beepit/server/
│   ├── actor/
│   │   ├── ActorSystemProvider.java       # Provider del ActorSystem único
│   │   ├── ChatRoomActor.java             # Actor para chat grupal (legacy)
│   │   ├── ConversationManagerActor.java  # Gestión de conversaciones 1-a-1
│   │   └── UserManagerActor.java          # Gestión de usuarios
│   │
│   ├── controller/
│   │   ├── AuthController.java            # API REST para auth y contactos
│   │   ├── ChatController.java            # Health check endpoint
│   │   └── TestController.java            # Interfaz Thymeleaf de prueba
│   │
│   ├── domain/
│   │   ├── command/                       # Commands para actores (sealed interfaces) ⭐ NUEVO
│   │   │   ├── ChatRoomCommand.java       # Comandos para ChatRoomActor
│   │   │   ├── ConversationManagerCommand.java  # Comandos para ConversationManagerActor
│   │   │   └── UserManagerCommand.java    # Comandos para UserManagerActor
│   │   │
│   │   ├── response/                      # Responses de actores (sealed interfaces) ⭐ NUEVO
│   │   │   ├── ChatRoomResponse.java      # Respuestas de ChatRoomActor
│   │   │   ├── ConversationManagerResponse.java  # Respuestas de ConversationManagerActor
│   │   │   ├── ParticipantsResponse.java  # Respuesta de participantes
│   │   │   └── UserManagerResponse.java   # Respuestas de UserManagerActor
│   │   │
│   │   └── model/
│   │       ├── AppUser.java               # Modelo de usuario
│   │       ├── Contact.java               # Modelo de contacto
│   │       ├── Conversation.java          # Modelo de conversación
│   │       ├── Message.java               # Modelo de mensaje
│   │       ├── MessageType.java           # Enum de tipos de mensaje ⭐ NUEVO
│   │       ├── PrivateMessage.java        # Modelo de mensaje privado
│   │       ├── RoomState.java             # Estado de sala de chat ⭐ NUEVO
│   │       └── UserSession.java           # Sesión de usuario
│   │
│   ├── service/
│   │   └── RateLimiterService.java        # Servicio de rate limiting
│   │
│   ├── websocket/
│   │   └── ChatWebSocketServerWithAkka.java  # Handler WebSocket (refactorizado)
│   │
│   └── Application.java                   # Punto de entrada
│
├── src/main/resources/
│   ├── views/
│   │   └── test-chat.html                 # Plantilla Thymeleaf ⭐ NUEVO
│   ├── application.yml                    # Configuración de Micronaut
│   └── logback.xml                        # Configuración de logging (mejorado)
│
├── src/test/java/com/beepit/server/
│   └── actor/
│       ├── UserManagerActorTest.java       # ⭐ NUEVO
│       └── ConversationManagerActorTest.java  # ⭐ NUEVO
│
├── build.gradle.kts                       # Configuración de Gradle
└── README.md                              # Este archivo
```

### Detalles Técnicos

#### ActorSystemProvider.java
- Crea y gestiona **3 ActorSystems independientes**:
  * `chat-room-system`: Para ChatRoomActor
  * `user-manager-system`: Para UserManagerActor
  * `conversation-manager-system`: Para ConversationManagerActor
- Proporciona **scheduler único compartido** para todas las operaciones
- Spawns actores con tipos específicos (Akka Typed)
- Limpieza automática con @PreDestroy termina todos los sistemas

#### Estructura de Commands y Responses (Sealed Interfaces)

**domain/command/** - Comandos para actores usando sealed interfaces con pattern matching:

- **UserManagerCommand.java**: 7 public records (RegisterUser, LoginUser, GetUser, GetAllUsers, AddContact, GetContacts, SetUserOnline)
- **ConversationManagerCommand.java**: 5 public records (SendPrivateMessage, GetConversation, GetUserConversations, MarkMessageDelivered, MarkMessageRead)
- **ChatRoomCommand.java**: 4 public records (JoinRoom, LeaveRoom, SendMessage, GetRoomParticipants)

**domain/response/** - Respuestas de actores usando sealed interfaces:

- **UserManagerResponse.java**: 7 public records (UserRegistered, UserLoggedIn, UserFound, AllUsers, ContactAdded, ContactList, ErrorResponse)
- **ConversationManagerResponse.java**: 5 public records (MessageSent, ConversationFound, ConversationsList, MessageUpdated, ErrorResponse)
- **ChatRoomResponse.java**: 3 public records (JoinedRoom, MessageSent, ErrorResponse)
- **ParticipantsResponse.java**: Record para lista de participantes

**Ventajas de esta arquitectura**:
- ✅ Tipos seguros con sealed interfaces y permits clauses
- ✅ Pattern matching exhaustivo en switch expressions
- ✅ Separación clara de responsabilidades por paquetes
- ✅ Código más mantenible sin clases anidadas
- ✅ Reutilización de tipos entre múltiples clases
- ✅ Acceso a campos mediante métodos (record accessors)

#### UserManagerActor.java
#### UserManagerActor.java
- Procesa comandos mediante pattern matching sobre UserManagerCommand
- Métodos principales: onRegisterUser(), onLoginUser(), onGetUser(), onGetAllUsers(), onAddContact(), onGetContacts(), onSetUserOnline()
- Validaciones: username 3-20 chars, password mín 6, contacto no duplicado
- Responde con records de UserManagerResponse
- Usuarios pre-cargados: Alice, Bob, Charlie, Diana, Eve

#### ConversationManagerActor.java
- Procesa comandos mediante pattern matching sobre ConversationManagerCommand
- Métodos principales: onSendPrivateMessage(), onGetConversation(), onGetUserConversations(), onMarkMessageDelivered(), onMarkMessageRead()
- Almacenamiento en ConcurrentHashMap<conversationId, Conversation>
- Genera conversationId consistente: sort([user1, user2]).join("-")
- Validaciones: max 5000 caracteres, contenido no vacío
- Responde con records de ConversationManagerResponse

#### ChatRoomActor.java
- Procesa comandos mediante pattern matching sobre ChatRoomCommand
- Métodos principales: onJoinRoom(), onLeaveRoom(), onSendMessage(), onGetParticipants()
- Estado gestionado por RoomState.java (extraído a domain/model)
- Responde con records de ChatRoomResponse
- Actualmente legacy, no usado en la interfaz principal

#### ChatWebSocketServerWithAkka.java
**Optimizaciones**:
- Rate limiting con RateLimiterService (10 msg/s/usuario)
- Map optimizado: `userSessions` (userId -> List<Session>) para O(1) lookup
- Map rápido: `sessionById` (sessionId -> SessionInfo)
- Broadcast eficiente: Filtra por roomId una sola vez

**Métodos refactorizados**:
- `onOpen()` dividido en: extractUserId(), extractUsername(), parseOtherUserId(), registerSession(), loadAndSendHistory()
- `onMessage()`: Valida rate limit, longitud (max 5000), contenido no vacío
- `@OnClose`: unregisterSession() - Limpia Maps, rate limiters, MDC
- `@OnError`: Logging estructurado con MDC context

**Timeouts configurables**:
- FAST_TIMEOUT: 2s (getContacts, getAllUsers)
- DEFAULT_TIMEOUT: 3s (sendMessage)
- 5s para getUserConversations (operación compleja)

**MDC Context**: userId, sessionId, conversationId en todos los logs
- Logging estructurado con MDC (userId, sessionId)
- Métodos separados para mejor mantenibilidad:
  * `extractUserId()`, `extractUsername()`
  * `loadAndSendHistory()`
  * `registerSession()`, `unregisterSession()`
  * `broadcastToConversation()`
- Timeouts configurables:
  * Operaciones rápidas (GetUser, GetContacts): 2 segundos
  * Operaciones normales (SendMessage, GetConversation): 3 segundos
  * Operaciones complejas (GetUserConversations): 5 segundos
- Validación de mensajes: contenido no vacío, máximo 5000 caracteres
- Limpieza automática de rate limiters al cerrar sesión

#### RateLimiterService.java
- Resilience4j RateLimiter por usuario
- Límite: 10 mensajes por segundo
- Timeout: 100ms
- Limpieza automática de rate limiters inactivos

#### TestController.java
- Usa Thymeleaf para renderizar plantillas
- Configuración dinámica de URLs (API y WebSocket)
- Soporta HTTP y HTTPS
- Detección automática de host y puerto

## Flujo de Usuario

### 1. Login
```
Usuario ingresa credenciales
    ↓
POST /api/auth/login
    ↓
UserManagerActor (LoginUser)
    ↓
Respuesta con datos del usuario
```

### 2. Agregar Contacto
```
Usuario busca en la interfaz
    ↓
GET /api/auth/users?query=bob
    ↓
Usuario hace clic en "+ Agregar"
    ↓
POST /api/auth/contacts/add
    ↓
UserManagerActor (AddContact)
    ↓
Contacto agregado, se refresca la lista
```

### 3. Enviar Mensaje
```
Usuario escribe mensaje y presiona Enter
    ↓
socket.send(JSON.stringify(message))
    ↓
ChatWebSocketServerWithAkka (onMessage)
    ↓
ConversationManagerActor (SendPrivateMessage)
    ↓
Mensaje guardado en ConcurrentHashMap
    ↓
Broadcast a todos los WebSockets de los participantes
```

### 4. Recibir Historial
```
Usuario abre chat con contacto
    ↓
WebSocket se conecta a /chat/{userId}/{contactId}
    ↓
ChatWebSocketServerWithAkka (onOpen)
    ↓
ConversationManagerActor (GetConversation)
    ↓
broadcaster.broadcastSync() envía historial
    ↓
Cliente recibe {type: "history", messages: [...]}
```

### 5. Mensajes Pendientes
```
Polling cada 5 segundos:
    ↓
GET /api/auth/conversations/{userId}
    ↓
ConversationManagerActor (GetUserConversations)
    ↓
Respuesta con unreadCount por conversación
    ↓
Si unreadCount > 0 y !isContact → mostrar 📬
    ↓
Usuario puede: "Aceptar y Chatear" o "Solo Chatear"
```

## Casos de Prueba

### Test 1: Chat entre contactos
1. Login como Alice
2. Buscar y agregar a Bob como contacto
3. Login como Bob (en otra ventana)
4. Buscar y agregar a Alice como contacto
5. Alice abre chat con Bob
6. Alice envía: "Hola Bob!"
7. Bob ve el mensaje instantáneamente
8. Bob responde: "Hola Alice!"
9. Ambos ven la conversación completa

### Test 2: Mensaje a no-contacto
1. Login como Charlie
2. Login como Diana (en otra ventana)
3. Charlie busca a Diana (sin agregar)
4. Charlie hace clic en "💬 Chat"
5. Charlie envía: "Hola Diana!"
6. Diana ve notificación 📬 (1)
7. Diana hace clic en 📬
8. Modal muestra: "charlie: Hola Diana!"
9. Diana puede: "✓ Aceptar y Chatear" (agrega contacto) o "💬 Solo Chatear"

### Test 3: Historial persistente
1. Login como Eve
2. Eve abre chat con Alice
3. Eve envía varios mensajes
4. Eve cierra el navegador
5. Eve vuelve a entrar y hace login
6. Eve abre chat con Alice
7. Todos los mensajes anteriores se cargan automáticamente

### Test 4: Rate Limiting
1. Login como Alice
2. Alice abre chat con Bob
3. Alice envía más de 10 mensajes en menos de 1 segundo
4. A partir del mensaje 11, Alice recibe error: `{"error": "Rate limit exceeded"}`
5. Después de 1 segundo, Alice puede enviar 10 mensajes más

### Test 5: Validación
1. Intenta registrarte con username "ab" → Error: "username debe tener entre 3 y 20 caracteres"
2. Intenta registrarte con password "123" → Error: "password debe tener mínimo 6 caracteres"
3. Intenta enviar mensaje vacío → Error: "Message content cannot be empty"
4. Intenta enviar mensaje de 6000 caracteres → Error: "Message too long (max 5000 characters)"

## Testing

### Tests Unitarios

El proyecto incluye tests para los actores principales usando **JUnit 5 + Akka TestKit**:

**UserManagerActorTest**
- ✅ Test de registro de usuario
- ✅ Test de login exitoso
- ✅ Test de credenciales inválidas
- ✅ Test de agregar contactos
- ✅ Test de listar todos los usuarios
- Verifica usuarios pre-cargados: alice, bob, charlie, diana, eve

**ConversationManagerActorTest**
- ✅ Test de envío de mensajes
- ✅ Test de obtener conversación
- ✅ Test de listar conversaciones de usuario
- ✅ Test de consistencia de conversation ID (user1→user2 = user2→user1)

### Ejecutar Tests

```bash
# Ejecutar todos los tests
.\gradlew.bat test

# Ejecutar test específico
.\gradlew.bat test --tests UserManagerActorTest

# Ver reporte HTML de tests
.\gradlew.bat test
# Abrir: build/reports/tests/test/index.html
```

## Troubleshooting

### El servidor no inicia
**Problema**: `Error: Could not find or load main class`
**Solución**: 
```bash
.\gradlew.bat clean build -x test
.\gradlew.bat run
```

### Rate limit exceeded
**Problema**: Cliente recibe `{"error": "Rate limit exceeded"}`
**Causa**: Usuario envió más de 10 mensajes en 1 segundo
**Solución**: 
1. Esperar 1 segundo antes de reintentar
2. Implementar debounce en el frontend
3. Mostrar mensaje al usuario: "Por favor, espera un momento antes de enviar más mensajes"

### Errores de validación
**Problema**: `400 Bad Request` al registrarse o enviar mensajes
**Causas comunes**:
- Username muy corto (< 3 caracteres) o muy largo (> 20 caracteres)
- Password muy corto (< 6 caracteres)
- Mensaje vacío o demasiado largo (> 5000 caracteres)
**Solución**: Verificar los constraints en el cliente antes de enviar

### Logs JSON no se generan
**Problema**: No aparece archivo `logs/app.json`
**Solución**:
1. Verificar que existe directorio `logs/` en la raíz del proyecto
2. Crear manualmente: `mkdir logs`
3. Verificar permisos de escritura
4. Reiniciar el servidor

### JDK incorrecto
**Problema**: `Unsupported class file major version 69`
**Solución**: Verificar que estás usando JDK 25:
```bash
java -version
# Debe mostrar: openjdk version "25"
```

### WebSocket no conecta
**Problema**: `WebSocket connection failed`
**Solución**: 
1. Verificar que el servidor está corriendo en http://localhost:8080
2. Revisar la consola del navegador para errores específicos
3. Verificar que los IDs de usuario y contacto son correctos

### Mensajes no se ven
**Problema**: Los mensajes se guardan pero no aparecen en la interfaz
**Solución**: 
- Ya corregido: El método `onOpen()` ahora es `void` y usa `broadcaster.broadcastSync()`
- Verificar logs del servidor: "History sent to session {id}"
- Verificar logs del cliente: "Historial recibido: X mensajes"

### No aparecen usuarios en la búsqueda
**Problema**: El buscador no muestra resultados
**Solución**:
1. Verificar que el servidor tenga los usuarios de prueba cargados
2. Usar nombres exactos: alice, bob, charlie, diana, eve
3. Revisar la respuesta de GET /api/auth/users en la consola del navegador

## Mejoras Implementadas (Enero 2026)

### ✅ Arquitectura
- **3 ActorSystems independientes**: chat-room-system, user-manager-system, conversation-manager-system
- **Scheduler único compartido**: Todos los actores usan el mismo scheduler para eficiencia
- **Optimización de broadcast**: Map<userId, List<Sessions>> para O(1) lookup
- **Sealed interfaces**: Comandos y respuestas separados en paquetes domain/command y domain/response
- **Refactorización de clases anidadas**: Todas las clases Command/Response extraídas a archivos independientes
- **MessageType enum extraído**: Separado del modelo Message a domain/model/MessageType.java
- **RoomState extraído**: Estado de salas movido a domain/model/RoomState.java

### ✅ Type Safety con Java Records
- **Pattern matching exhaustivo**: Switch expressions con sealed interfaces
- **Records como mensajes**: Todos los comandos y respuestas son public records
- **Permits clauses**: Sealed interfaces declaran explícitamente los tipos permitidos
- **Accessor methods**: Acceso a campos mediante cmd.field() en lugar de cmd.field

### ✅ Validación
- **Bean Validation**: @Valid, @NotBlank, @Size en DTOs
- **Validación de mensajes**: Máximo 5000 caracteres, no vacíos
- **Validación de usuarios**: Username 3-20 caracteres, password mínimo 6 caracteres

### ✅ Rate Limiting
- **Resilience4j**: 10 mensajes por segundo por usuario
- **Configuración**: 100ms timeout, período de 1 segundo
- **Cleanup automático**: Limpieza de limiters al desconectar

### ✅ Logging Estructurado
- **Logback + Logstash**: JSON file appender con rotación diaria
- **MDC Context**: userId, sessionId, conversationId en todos los logs
- **Niveles por paquete**: DEBUG para com.beepit.server.*, WARN para io.netty

### ✅ Testing
- **JUnit 5 + Akka TestKit**: 9 tests unitarios para actores
- **Cobertura**: UserManagerActor, ConversationManagerActor
- **Verificación**: Pre-loaded users, message flow, conversation consistency

### ✅ Thymeleaf
- **Separación MVC**: HTML extraído de Java a template
- **Template dinámico**: test-chat.html con variables apiBase/wsBase
- **Estilo moderno**: Diseño con colores Beepit (amarillo/negro)

### ✅ WebSocket Mejorado
- **@OnClose implementado**: Cleanup de sesiones, maps, y rate limiters
- **@OnError con MDC**: Logging contextual de errores
- **Refactorización onOpen**: 8 métodos con responsabilidad única
- **Timeouts configurables**: 2s (fast), 3s (default), 5s (complex)

## Mejoras Futuras

### Almacenamiento
- [ ] Migrar de memoria a PostgreSQL
- [ ] Implementar R2DBC para mantener la reactividad
- [ ] Agregar Redis para caché de sesiones

### Seguridad
- [x] ~~Rate limiting para prevenir spam~~ ✅ Implementado con Resilience4j
- [ ] Implementar JWT para autenticación
- [ ] Encriptación end-to-end de mensajes
- [ ] HTTPS obligatorio en producción

### Funcionalidades
- [ ] Chat grupal funcional (ya existe el actor base)
- [ ] Envío de archivos (imágenes, documentos)
- [ ] Llamadas de voz y video (WebRTC)
- [ ] Mensajes de voz
- [ ] Reacciones a mensajes (emoji)
- [ ] Edición y eliminación de mensajes
- [ ] Confirmación de lectura visual (doble check)

### DevOps
- [ ] Dockerización del backend
- [ ] CI/CD con GitHub Actions
- [ ] Despliegue en Kubernetes
- [ ] Métricas con Prometheus + Grafana
- [x] ~~Logs centralizados con ELK Stack~~ ✅ Parcialmente implementado (JSON logs listos para Logstash)

## Licencia

Este proyecto es de código abierto y está disponible bajo la licencia Apache 2.0.

## Contribuciones

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Contacto

Para preguntas o sugerencias, abre un issue en el repositorio.

---

**Versión actual**: 1.1.0  
**Creación**: 10 de Enero de 2026  
**Creadores**: Julian Ismael Luna Arecha  
**Última actualización**: 11 de Enero de 2026  
**Estado**: Funcional y probado con arquitectura refactorizada
