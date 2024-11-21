import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class ChatServer {
    private static final int PORT = 12345; 
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>()); 
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss"); 
    private static Map<String, String> userCredentials = new HashMap<>(); 

    // Starts the server and acccepts client connections
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            printServerMessage("Chat Server is running on port " + PORT);
            printServerMessage("Waiting for clients...");
            
            // Start a handler for each client, and start a new thread for each handler
            while (true) {
                Socket clientSocket = serverSocket.accept();
                printServerMessage("New connection from: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler handler = new ClientHandler(clientSocket, clients, userCredentials);
                new Thread(handler).start(); 
            }
        } catch (IOException e) {
            printServerMessage("Server Error: " + e.getMessage());
        }
    }

    // Server message is logged with the time stamp
    public static void printServerMessage(String message) {
        System.out.println("[SERVER " + timeFormat.format(new Date()) + "] " + message);
    }
}

// ClientHandler class that handles individual client connections
class ClientHandler implements Runnable {
    private Socket socket; 
    private PrintWriter out; 
    private BufferedReader in; 
    private String username; 
    private Set<ClientHandler> clients; 
    private Map<String, String> userCredentials; 
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss"); 

    // Client Handler is initialized with the client socket, set of clients, and user credentials
    public ClientHandler(Socket socket, Set<ClientHandler> clients, Map<String, String> userCredentials) {
        this.socket = socket;
        this.clients = clients;
        this.userCredentials = userCredentials;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            logMessage("Error: " + e.getMessage());
        }
    }

    // This method handles client interactions
    @Override
    public void run() {
        try {
            if (!handleAuthentication()) {
                socket.close(); 
                return;
            }

            clients.add(this); 
            logMessage(username + " has joined the chat");
            broadcast(username + " has joined the chat!");

            // Welcome message with the commands to use for client
            out.println("Welcome " + username + "! Commands available:");
            out.println("/file <filepath> - Send a file");
            out.println("/users - List connected users");
            out.println("/quit - Exit the chat");

            String message; 
            // Reads the messages from the client
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/")) {
                    handleCommand(message); 
                } else {
                    logMessage(username + ": " + message);
                    broadcast(username + ": " + message);
                }
            }
        } catch (IOException e) {
            logMessage(username + " disconnected due to error: " + e.getMessage());
        } finally {
            disconnect(); 
        }
    }

    // Handles user authentication to login or register
    private boolean handleAuthentication() throws IOException {
        out.println("/auth Choose action (1: Login, 2: Register):");
        String choice = in.readLine(); 

        if ("2".equals(choice)) { 
            out.println("/auth Enter new username:");
            String newUsername = in.readLine(); 
            if (userCredentials.containsKey(newUsername)) {
                out.println("/auth Username already exists!");
                return false; 
            }
            out.println("/auth Enter password:");
            String password = in.readLine();
            userCredentials.put(newUsername, password); 
            username = newUsername; 
            out.println("/auth Registration successful! Press Enter to Continue.");
            return true; 
        } else { 
            out.println("/auth Enter username:");
            String loginUsername = in.readLine(); 
            out.println("/auth Enter password:");
            String password = in.readLine(); 

            if (userCredentials.containsKey(loginUsername) && 
                userCredentials.get(loginUsername).equals(password)) {
                username = loginUsername; 
                out.println("/auth Login successful!");
                return true; 
            } else {
                out.println("/auth Invalid credentials!");
                return false; 
            }
        }
    }

    // Handler for all commands that are sent to the client
    private void handleCommand(String message) {
        String[] parts = message.split("\\s+", 2); 
        String command = parts[0].toLowerCase(); 

        switch (command) {
            case "/file":
                if (parts.length > 1) {
                    handleFileTransfer(parts[1]); 
                } else {
                    out.println("Usage: /file <filepath>");
                }
                break;
                
            case "/users":
                sendUserList(); 
                break;
                
            case "/quit":
                disconnect(); 
                break;
                
            default:
                out.println("Unknown command. Available commands:");
                out.println("/file <filepath> - Send a file");
                out.println("/users - List connected users");
                out.println("/quit - Exit the chat");
        }
    }

    // File transfer method
    private void handleFileTransfer(String filePath) {
        try {
            File file = new File(filePath); 
            if (!file.exists()) {
                out.println("File not found: " + filePath);
                return; 
            }

            // creates the upload directory if it doesn't exist
            File uploadsDir = new File("uploads");
            uploadsDir.mkdir(); 

            // reads the file and writes it to the uploads directory
            try (FileInputStream fileIn = new FileInputStream(file);
                 FileOutputStream fileOut = new FileOutputStream("uploads/" + file.getName())) {
                byte[] buffer = new byte[4096]; 
                int bytesRead; 
                
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead); 
                }
            }
            
            String message = username + " shared file: " + file.getName(); 
            logMessage(message); 
            broadcast(message); 
            
        } catch (IOException e) {
            logMessage("File transfer error from " + username + ": " + e.getMessage());
            out.println("Error sending file: " + e.getMessage());
        }
    }

    // Sends the list of all connected users to all clients
    private void sendUserList() {
        StringBuilder userList = new StringBuilder("Connected users:\n");
        synchronized(clients) { 
            for (ClientHandler client : clients) {
                userList.append("- ").append(client.username).append("\n"); 
            }
        }
        out.println(userList.toString()); 
    }

    // allows for broadcast messages to all clients
    private void broadcast(String message) {
        synchronized(clients) { 
            for (ClientHandler client : clients) {
                client.out.println(message); 
            }
        }
    }

    // logs message with the time stamp
    private void logMessage(String message) {
        System.out.println("[" + timeFormat.format(new Date()) + "] " + message); 
    }

    // Allows for disconnection of client from server
    private void disconnect() {
        try {
            clients.remove(this); 
            logMessage(username + " has left the chat"); 
            broadcast(username + " has left the chat!"); 
            socket.close(); 
        } catch (IOException e) {
            logMessage("Error disconnecting user: " + e.getMessage());
        }
    }
}
