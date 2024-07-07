import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Main {
    private static String directory;

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length > 1 && args[0].equals("--directory")) {
            directory = args[1];
        } else {
            // Default directory if not provided
            directory = ".";
        }

        System.out.println("Logs from your program will appear here!");
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                System.out.println("Accepted new connection");
                // Handle each client connection in a separate thread.
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            // Read the request line
            String requestLine = inputStream.readLine();
            if (requestLine == null) return;  // Early exit if no request line

            String httpMethod = requestLine.split(" ")[0];
            String urlPath = requestLine.split(" ")[1];

            // Read all the headers from the HTTP request.
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while (!(headerLine = inputStream.readLine()).isEmpty()) {
                String[] headerParts = headerLine.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            // Extract the request body (if there is one)
            StringBuilder bodyBuilder = new StringBuilder();
            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] buffer = new char[contentLength];
                inputStream.read(buffer, 0, contentLength);
                bodyBuilder.append(buffer);
            }
            String body = bodyBuilder.toString();

            // Write the HTTP response to the output stream.
            OutputStream outputStream = clientSocket.getOutputStream();
            String httpResponse = getHttpResponse(httpMethod, urlPath, headers, body);
            System.out.println("Sending response: " + httpResponse);

            // Write the response header and body
            outputStream.write(httpResponse.getBytes("UTF-8"));

            // Close the input and output streams.
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            // Close the client socket.
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    private static String getHttpResponse(String httpMethod, String urlPath,
                                          Map<String, String> headers, String body)
            throws IOException {
        String httpResponse;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream responseStream;

        if ("GET".equals(httpMethod)) {
            if ("/".equals(urlPath)) {
                httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
            } else if (urlPath.startsWith("/echo/")) {
                String echoStr = urlPath.substring(6); // Extract the string after "/echo/"
                String acceptEncoding = headers.get("Accept-Encoding");
                boolean supportsGzip = false;
                if (acceptEncoding != null) {
                    String[] encodings = acceptEncoding.split(",");
                    for (String encoding : encodings) {
                        if ("gzip".equalsIgnoreCase(encoding.trim())) {
                            supportsGzip = true;
                            break;
                        }
                    }
                }

                if (supportsGzip) {
                    // Compress the response body using gzip
                    responseStream = new GZIPOutputStream(byteArrayOutputStream);
                    responseStream.write(echoStr.getBytes("UTF-8"));
                    responseStream.close(); // Important to close the stream to complete compression
                    byte[] gzipData = byteArrayOutputStream.toByteArray();
                    httpResponse =
                            "HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Type: text/plain\r\nContent-Length: " +
                            gzipData.length + "\r\n\r\n";
                } else {
                    httpResponse =
                            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                            echoStr.length() + "\r\n\r\n" + echoStr;
                }
            } else if ("/user-agent".equals(urlPath)) {
                String userAgent = headers.get("User-Agent");
                if (userAgent == null) userAgent = "";
                httpResponse =
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                        userAgent.length() + "\r\n\r\n" + userAgent;
            } else if (urlPath.startsWith("/files/")) {
                String filename = urlPath.substring(7); // Extract the filename after "/files/"
                File file = new File(directory, filename);
                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    httpResponse =
                            "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                            fileContent.length + "\r\n\r\n";
                    byteArrayOutputStream.write(fileContent);
                } else {
                    httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        } else if ("POST".equals(httpMethod) && urlPath.startsWith("/files/")) {
            String filename = urlPath.substring(7); // Extract the filename after "/files/"
            File file = new File(directory, filename);
            if (!file.getCanonicalPath().startsWith(new File(directory).getCanonicalPath())) {
                httpResponse = "HTTP/1.1 403 Forbidden\r\n\r\n";
            } else {
                // Get the length of the request body
                if (headers.containsKey("Content-Length")) {
                    int contentLength = Integer.parseInt(headers.get("Content-Length"));
                    if (contentLength == body.length()) {
                        // Write the request body to the file
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                            writer.write(body);
                        }
                        httpResponse = "HTTP/1.1 201 Created\r\n\r\n";
                    } else {
                        httpResponse = "HTTP/1.1 500 Internal Server Error\r\n\r\n";
                    }
                } else {
                    httpResponse = "HTTP/1.1 400 Bad Request\r\n\r\n";
                }
            }
        } else {
            httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        }

        byteArrayOutputStream.flush();
        byte[] responseBytes = byteArrayOutputStream.toByteArray();
        return httpResponse + new String(responseBytes, "UTF-8");
    }
}
