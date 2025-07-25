import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class SSLClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8443;
    
    public static void main(String[] args) {
        try {
            // Create SSL context that trusts all certificates (for testing)
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            }, new SecureRandom());
            
            // Create SSL socket factory
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            System.out.println("Attempting to connect to SSL server...");
            
            // Establish SSL connection to server
            try (SSLSocket sslSocket = (SSLSocket) factory.createSocket(SERVER_HOST, SERVER_PORT);
                 PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                 BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
                
                // Force SSL handshake
                sslSocket.startHandshake();
                
                System.out.println("SSL Client connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
                System.out.println("SSL Session: " + sslSocket.getSession().getCipherSuite());
                System.out.println("Protocol: " + sslSocket.getSession().getProtocol());
                System.out.println("Peer Host: " + sslSocket.getSession().getPeerHost());
                System.out.println("=== SSL Handshake Complete - Communication is now encrypted ===");
                System.out.println("Type messages to send (type 'bye' to quit):");
                
                String inputLine;
                while ((inputLine = userInput.readLine()) != null) {
                    // Send encrypted message to server
                    System.out.println("Sending encrypted: " + inputLine);
                    out.println(inputLine);
                    
                    // Read encrypted response from server
                    String response = in.readLine();
                    if (response != null) {
                        System.out.println("Received encrypted: " + response);
                    }
                    
                    if ("bye".equalsIgnoreCase(inputLine)) {
                        System.out.println("Closing encrypted connection...");
                        break;
                    }
                }
                
            } catch (ConnectException e) {
                System.err.println("Could not connect to server. Make sure the SSL server is running.");
                System.err.println("Try running: java SSLServer");
            }
            
        } catch (Exception e) {
            System.err.println("SSL Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
