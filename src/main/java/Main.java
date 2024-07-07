import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
public class Main {
  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    String CRLF = "\r\n";
    String http200 = "HTTP/1.1 200 OK";
    String http404 = "HTTP/1.1 404 Not Found";
    String directory = "";
    if (args.length > 1 && args[0].equals("--directory")) {
      directory = args[1];
      System.out.println(args[1]);
    }
    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      while (true) {
        clientSocket =
            serverSocket.accept(); // Wait for connection from client.
        // client side conversion of bytes into data.
        BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));
        OutputStream outputStream = clientSocket.getOutputStream();
        String request; // Read the request
        ArrayList<String> HttpRequest = new ArrayList<>();
        // read request completely HTTP requests don't end with EOF but with
        // blank line.
        while (!(request = bufferedReader.readLine()).equals(""))
          HttpRequest.add(request);
        System.out.println(HttpRequest);
        // Striping URL from the HTTP req
        String[] URL = HttpRequest.get(0).split(" ", 0);
        if (URL[0].equals("POST")) {
          StringBuffer data = new StringBuffer();
          while (bufferedReader.ready())
            data.append((char)bufferedReader.read());
          String body = data.toString();
          Path path = Paths.get(directory, URL[1].split("/")[2]);
          Files.write(path, body.getBytes());
          outputStream.write(("HTTP/1.1 201 Created" + CRLF + CRLF)
                                 .getBytes(StandardCharsets.UTF_8));
        } else {
          if (URL[1].equals("/")) {
            String response = http200 + CRLF + CRLF;
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
          } else if (URL[1].startsWith("/echo/")) {
            String[] path = URL[1].split("/", 0);
            boolean flag = false;
            String response = http200 + CRLF + "Content-Type: text/plain" +
                              CRLF + "Content-Length:" + path[2].length() +
                              CRLF + CRLF + path[2];
            for (String s : HttpRequest) {
              if (s.startsWith("Accept-Encoding")) {
                String[] encoding = s.split(": ")[1].split(",");
                for (String encode : encoding) {
                  if (encode.trim().startsWith("gzip")) {
                    // Compress the response body using gzip
                    ByteArrayOutputStream byteArrayOutputStream =
                        new ByteArrayOutputStream();
                    try (GZIPOutputStream gzipOutputStream =
                             new GZIPOutputStream(byteArrayOutputStream)) {
                      gzipOutputStream.write(
                          path[2].getBytes(StandardCharsets.UTF_8));
                    }
                    byte[] gzipData = byteArrayOutputStream.toByteArray();
                    response = http200 + CRLF + "Content-Encoding:gzip" + CRLF +
                               "Content-Type: text/plain" + CRLF +
                               "Content-Length:" + gzipData.length + CRLF +
                               CRLF;
                    flag = true;
                    outputStream.write(response.getBytes());
                    outputStream.write(gzipData);
                  }
                }
              }
            }
            if (flag == false)
              outputStream.write(response.getBytes());
          } else if (URL[1].startsWith("/user-agent")) {
            String[] userAgent = new String[2];
            for (String s : HttpRequest)
              if (s.startsWith("User-Agent"))
                userAgent = s.split(": ");
            String response = http200 + CRLF + "Content-Type: text/plain" +
                              CRLF + "Content-Length:" + userAgent[1].length() +
                              CRLF + CRLF + userAgent[1];
            outputStream.write(response.getBytes());
          } else {
            if (URL[1].startsWith("/files")) {
              String filename = URL[1].split("/", 0)[2];
              File file = new File(directory, filename);
              System.out.println(file.toPath());
              if (file.exists()) {
                byte[] fileContent =
                    Files.readAllBytes(file.toPath()); // Reading byte content
                String response =
                    http200 + CRLF + "Content-Type: application/octet-stream" +
                    CRLF + "Content-Length: " + fileContent.length + CRLF +
                    CRLF + new String(fileContent);
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
              } else {
                String response = http404 + CRLF + CRLF;
                outputStream.write(response.getBytes());
              }
            } else {
              String response = http404 + CRLF + CRLF;
              outputStream.write(response.getBytes());
            }
          }
          clientSocket.close();
          System.out.println("accepted new connection");
        }
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}