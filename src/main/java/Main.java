import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class Main {
  private static String directory;

  public static void main(String[] args) {
    // Parse the command-line argument for the directory
    if (args.length == 2 && args[0].equals("--directory")) {
      directory = args[1];
    } else {
      System.out.println("Usage: java Main --directory <directory-path>");
      return;
    }

    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    System.out.println("Logs from your program will appear here!");

    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(new ClientHandler(clientSocket, directory)).start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}

class ClientHandler implements Runnable {
  private Socket clientSocket;
  private String directory;

  public ClientHandler(Socket socket, String directory) {
    this.clientSocket = socket;
    this.directory = directory;
  }

  @Override
  public void run() {
    try {
      InputStream inputStream = clientSocket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String line = reader.readLine();
      System.out.println(line);
      if (line == null) {
        clientSocket.close();
        return;
      }
      String[] httpPath = line.split(" ", 0);
      String path = httpPath[1];
      OutputStream output = clientSocket.getOutputStream();

      if (path.equals("/")) {
        output.write(("HTTP/1.1 200 OK\r\n\r\n").getBytes());
      } else if (path.startsWith("/files/")) {
        handleFileRequest(path.substring(7), output);
      } else if (path.startsWith("/echo/")) {
        String msg = path.substring(6);
        String header = String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            msg.length(), msg);
        output.write(header.getBytes());
      } else if (path.equals("/user-agent")) {
        reader.readLine();
        String useragent = reader.readLine().split(": ")[1];
        String reply = String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            useragent.length(), useragent);
        output.write(reply.getBytes());
      } else {
        output.write(("HTTP/1.1 404 Not Found\r\n\r\n").getBytes());
      }
      System.out.println("accepted new connection");
      clientSocket.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      if (clientSocket != null) {
        try {
          clientSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void handleFileRequest(String filename, OutputStream output) throws IOException {
    File file = new File(directory, filename);

    if (file.exists() && file.isFile()) {
      byte[] fileContent = new byte[(int) file.length()];
      FileInputStream fis = new FileInputStream(file);
      fis.read(fileContent);
      fis.close();

      String header = String.format(
          "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: %d\r\n\r\n",
          file.length());
      output.write(header.getBytes());
      output.write(fileContent);
    } else {
      output.write(("HTTP/1.1 404 Not Found\r\n\r\n").getBytes());
    }
  }
}
