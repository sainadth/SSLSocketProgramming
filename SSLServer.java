import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SSLServer {
    private static final int PORT = 8443;
    private static final String KEYSTORE_PATH = "server.keystore";
    private static final String KEYSTORE_PASSWORD = "password";
    
    public static void main(String[] args) {
        try {
            // Create a simple keystore using keytool command
            createKeystoreUsingKeytool();
            
            // Create SSL context manually to avoid default context issues
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(KEYSTORE_PATH), KEYSTORE_PASSWORD.toCharArray());
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
            
            // Create SSL server socket factory
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            
            // Create SSL server socket
            try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(PORT)) {
                
                System.out.println("SSL Server started on port " + PORT);
                System.out.println("Waiting for client connections...");
                
                while (true) {
                    // Accept SSL client connection
                    try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
                        
                        System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());
                        System.out.println("SSL Session: " + clientSocket.getSession().getCipherSuite());
                        
                        // Handle client communication
                        handleClient(clientSocket);
                        
                    } catch (IOException e) {
                        System.err.println("Error handling client: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("SSL Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void handleClient(SSLSocket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Client: " + inputLine);
                
                // Echo back the message with encryption
                String response = "Echo: " + inputLine;
                out.println(response);
                
                if ("bye".equalsIgnoreCase(inputLine)) {
                    System.out.println("Client disconnected");
                    break;
                }
            }
        }
    }
    
    private static void createKeystoreUsingKeytool() {
        try {
            // Check if keystore already exists
            File keystoreFile = new File(KEYSTORE_PATH);
            if (keystoreFile.exists()) {
                System.out.println("Keystore already exists: " + KEYSTORE_PATH);
                return;
            }
            
            // Create keystore using keytool command
            String[] keytoolCmd = {
                "keytool",
                "-genkeypair",
                "-alias", "server",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-keystore", KEYSTORE_PATH,
                "-storepass", KEYSTORE_PASSWORD,
                "-keypass", KEYSTORE_PASSWORD,
                "-dname", "CN=localhost, OU=Test, O=Test, L=Test, ST=Test, C=US",
                "-validity", "365"
            };
            
            ProcessBuilder pb = new ProcessBuilder(keytoolCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Keystore created successfully: " + KEYSTORE_PATH);
            } else {
                System.err.println("Failed to create keystore with keytool");
                // Create a basic keystore programmatically as fallback
                createBasicKeystore();
            }
            
        } catch (Exception e) {
            System.err.println("Error creating keystore with keytool: " + e.getMessage());
            // Fallback to basic keystore creation
            createBasicKeystore();
        }
    }
    
    private static void createBasicKeystore() {
        try {
            // Generate key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            
            // Create a simple keystore without certificate chain
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            
            // Create a dummy certificate (this is a simplified approach)
            // In a real application, you would use proper certificate generation
            System.out.println("Creating basic keystore without certificate validation...");
            System.out.println("Note: This is for testing purposes only!");
            
            // Save empty keystore first
            try (FileOutputStream fos = new FileOutputStream(KEYSTORE_PATH)) {
                keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
            }
            
            System.out.println("Basic keystore created: " + KEYSTORE_PATH);
            System.out.println("Warning: SSL connections may fail without proper certificates");
            
        } catch (Exception e) {
            System.err.println("Error creating basic keystore: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
