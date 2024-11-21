import java.io.*; 
import java.net.*; 
import java.util.Scanner; 

public class ChatClient {
    private Socket socket; 
    private PrintWriter out; 
    private BufferedReader in; 
    private Scanner scanner; 
    private boolean running = true; 

    // Initializes ChatClient and establish connection to server
    public ChatClient(String host, int port) {
        try {
            // Initialize input/output streams and establish connection to server
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            scanner = new Scanner(System.in);

            // If user isn't authenticated, exit
            if (!handleAuthentication()) {
                System.out.println("Authentication failed. Exiting...");
                return; 
            }

            // Thread starts to receive messages from server
            new Thread(this::receiveMessages).start();

            // Sends messages to server
            while (running) {
                String message = scanner.nextLine(); 
                if (message.equalsIgnoreCase("/quit")) { 
                    running = false; 
                    out.println("/quit"); 
                    break; 
                } else {
                    out.println(message); 
                }
            }
        } catch (IOException e) {
            System.out.println("Client Error: " + e.getMessage()); 
        } finally {
            closeConnection(); // Ensure the connection is closed when done
        }
    }

    // Method to handle authentication with the server
    private boolean handleAuthentication() throws IOException {
        String response;
        while ((response = in.readLine()) != null) { 
            if (response.startsWith("/auth")) { 
                String prompt = response.substring(6); 
                System.out.println(prompt); 
                String input = scanner.nextLine(); 
                out.println(input); 
                
                // Check if authentication was successful
                if (prompt.contains("successful")) {
                    return true; 
                }
                if (prompt.contains("Invalid credentials") || prompt.contains("already exists")) {
                    return false; 
                }
            }
        }
        return false; 
    }

    // Receives and prints messages from the server
    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) { 
                System.out.println(message); 
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("Connection to server lost."); 
            }
        }
    }

    // Closes the connection to the server
    private void closeConnection() {
        try {
            running = false; 
            socket.close(); 
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage()); 
        }
    }

    // Starts ChatClient
    public static void main(String[] args) {
        ChatClient client = new ChatClient("localhost", 12345); 
    }
}
