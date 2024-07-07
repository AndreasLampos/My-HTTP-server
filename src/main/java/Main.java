import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  public static void main(String[] args) {
    String directory = null;
    if ((args.length == 2) && (args[0].equalsIgnoreCase("--directory"))) {
      directory = args[1];
    } else {
      System.out.println("Usage: java Main --directory <directory-path>");
      return;
    }

    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        final String finalDirectory = directory;

        new Thread(() -> {
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
               OutputStream outputStream = clientSocket.getOutputStream()) {

            // Read the request line
            String requestLine = reader.readLine();
            if (requestLine == null) {
              clientSocket.close();
              return;
            }
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String path = requestParts[1];

            // Read the headers
            String userAgent = "";
            int contentLength = 0;
            String line;
            while (!(line = reader.readLine()).equals("")) {
              if (line.startsWith("User-Agent: "))
                userAgent = line.substring(12);
              else if (line.startsWith("Content-Length: "))
                contentLength = Integer.parseInt(line.substring(16));
            }

            if (method.equals("POST") && path.startsWith("/files/")) {
              // Handle POST request for file upload
              String fileName = path.substring(7);
              Path filePath = Paths.get(finalDirectory, fileName);
              byte[] fileBytes = new byte[contentLength];
              reader.read(fileBytes, 0, contentLength);
              Files.write(filePath, fileBytes);
              String response = "HTTP/1.1 201 Created\r\n\r\n";
              outputStream.write(response.getBytes());
            } else if (method.equals("GET") && path.startsWith("/files/")) {
              String fileName = path.substring(7);
              Path filePath = Paths.get(finalDirectory, fileName);
              if (Files.exists(filePath)) {
                byte[] fileBytes = Files.readAllBytes(filePath);
                String response = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + fileBytes.length + "\r\n\r\n";
                outputStream.write(response.getBytes());
                outputStream.write(fileBytes);
              } else {
                outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
              }
            } else if (method.equals("GET") && path.equals("/user-agent")) {
              String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + userAgent.length() + "\r\n\r\n" + userAgent;
              outputStream.write(response.getBytes());
            } else if (method.equals("GET") && path.startsWith("/echo/")) {
              String echoMessage = path.substring(6);
              String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + echoMessage.length() + "\r\n\r\n" + echoMessage;
              outputStream.write(response.getBytes());
            } else if (method.equals("GET") && path.equals("/")) {
              outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
            } else {
              outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }
            outputStream.flush();
          } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
          } finally {
            try {
              clientSocket.close();
            } catch (IOException e) {
              System.out.println("IOException: " + e.getMessage());
            }
          }
        }).start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
