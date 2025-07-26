### How to execute

Compile the server:

```bash
javac SSLServer.java
```

Run the server:

```bash
java SSLServer
```

The server will:

1. Create a keystore automatically if it doesn't exist
2. Start listening on port 8443
3. Accept SSL client connections

### Testing with OpenSSL client:

```bash
openssl s_client -connect localhost:8443
```

Compile the client:

```bash
javac SSLClient.java
```

Run the client in a new terminal:

```bash
java SSLClient
```

Send messages from client to server

Terminate connection by sending `bye` as the message
