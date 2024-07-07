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
        new Thread(new ClientHandler(clientSocket)).start();
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

  static class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
      this.clientSocket = socket;
    }

    @Override
    public void run() {
      try {
        InputStream inputStream = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        System.out.println(line);
        String[] httpPath = line.split(" ", 0);
        String path = httpPath[1];

        OutputStream output = clientSocket.getOutputStream();

        if (path.startsWith("/files/")) {
          String filename = path.substring(7);  // Remove "/files/" from the path
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
        } else {
          output.write(("HTTP/1.1 404 Not Found\r\n\r\n").getBytes());
        }

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
  }
}
