/*
 * SSLServer.java
 * This program implements a simple SSL server that listens for client connections,
 * handles encrypted communication, and echoes back messages.
 * Modified by Sainadth Pagadala on 2023-10-30.
 */

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.SecureRandom;

public class SSLServer {
    // Server details
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
                
                while (true) {
                    System.out.println("\nWaiting for client connections...");
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
            
            /* 
             * keytool command generates a self-signed certificate and stores it in the keystore.
             * The key algorithm is RSA with a key size of 2048 bits.
             * The keystore is valid for 365 days.
             */
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
            
            // creates child process to run keytool command
            ProcessBuilder pb = new ProcessBuilder(keytoolCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            // join the process to wait for it to finish
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Keystore created successfully: " + KEYSTORE_PATH);
            } else {
                throw new RuntimeException("Error executing keytool command. Please ensure Java keytool is installed and accessible.");
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
