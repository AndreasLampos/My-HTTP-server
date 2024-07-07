import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class Main {
  public static void main(String[] args) {
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
}

class ClientHandler implements Runnable {
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
      System.out.println(httpPath[1]);
      String[] HttpRequest = line.split(" ", 0);
      String[] str = HttpRequest[1].split("/");
      OutputStream output = clientSocket.getOutputStream();
      if (httpPath[1].equals("/")) {
        output.write(("HTTP/1.1 200 OK\r\n\r\n").getBytes());
      } else if (httpPath[1].startsWith("/echo/")) {
        String msg = HttpRequest[1].substring(6);
        String header = String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            msg.length(), msg);
        output.write(header.getBytes());
      } else if (str[1].equals("user-agent")) {
        reader.readLine();
        String useragent = reader.readLine().split("\\s+")[1];
        String reply = String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %s\r\n\r\n%s\r\n", useragent.length(), useragent);
        output.write(reply.getBytes());
      } else if ((str.length > 2 && str[1].equals("echo"))) {
        String responsebody = str[2];
        String finalstr = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/plain\r\n" + "Content-Length: " + responsebody.length() + "\r\n\r\n" + responsebody;
        output.write(finalstr.getBytes());
      } else {
        output.write(("HTTP/1.1 404 Not Found\r\n\r\n").getBytes());
      }
      System.out.println("accepted new connection");
      output.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
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
