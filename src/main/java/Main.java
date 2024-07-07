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
        try (
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream outputStream = clientSocket.getOutputStream()
        ) {
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
            String httpResponse = getHttpResponse(httpMethod, urlPath, headers, body);
            System.out.println("Sending response: " + httpResponse);
            
            // Write the response headers and body
            outputStream.write(httpResponse.getBytes("UTF-8"));
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
        StringBuilder responseHeaders = new StringBuilder();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String httpResponse = "";
        
        if ("GET".equals(httpMethod)) {
            if ("/".equals(urlPath)) {
                responseHeaders.append("HTTP/1.1 200 OK\r\n\r\n");
            } else if (urlPath.startsWith("/echo/")) {
                String echoStr = urlPath.substring(6); // Extract the string after "/echo/"
                String contentEncoding = headers.get("Accept-Encoding");
                OutputStream responseBodyStream = byteArrayOutputStream;
                if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
                    // Add Content-Encoding: gzip header if gzip is supported
                    responseHeaders.append("HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Type: text/plain\r\n");
                    responseBodyStream = new GZIPOutputStream(byteArrayOutputStream);
                } else {
                    responseHeaders.append("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n");
                }
                responseBodyStream.write(echoStr.getBytes("UTF-8"));
                responseBodyStream.close(); // Close the stream to finalize the gzip compression if used
                
                byte[] responseBody = byteArrayOutputStream.toByteArray();
                responseHeaders.append("Content-Length: ")
                        .append(responseBody.length)
                        .append("\r\n\r\n");
                
                // Prepare the full response
                httpResponse = responseHeaders.toString();
            } else if ("/user-agent".equals(urlPath)) {
                String userAgent = headers.get("User-Agent");
                if (userAgent == null) userAgent = "";
                responseHeaders.append("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ")
                        .append(userAgent.length())
                        .append("\r\n\r\n");
                httpResponse = responseHeaders.toString() + userAgent;
            } else if (urlPath.startsWith("/files/")) {
                String filename = urlPath.substring(7); // Extract the filename after "/files/"
                File file = new File(directory, filename);
                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    responseHeaders.append("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: ")
                            .append(fileContent.length)
                            .append("\r\n\r\n");
                    httpResponse = responseHeaders.toString();
                } else {
                    responseHeaders.append("HTTP/1.1 404 Not Found\r\n\r\n");
                    httpResponse = responseHeaders.toString();
                }
            } else {
                responseHeaders.append("HTTP/1.1 404 Not Found\r\n\r\n");
                httpResponse = responseHeaders.toString();
            }
        } else if ("POST".equals(httpMethod) && urlPath.startsWith("/files/")) {
            String filename = urlPath.substring(7); // Extract the filename after "/files/"
            File file = new File(directory, filename);
            if (!file.getCanonicalPath().startsWith(new File(directory).getCanonicalPath())) {
                responseHeaders.append("HTTP/1.1 403 Forbidden\r\n\r\n");
                httpResponse = responseHeaders.toString();
            } else {
                // Get the length of the request body
                if (headers.containsKey("Content-Length")) {
                    int contentLength = Integer.parseInt(headers.get("Content-Length"));
                    if (contentLength == body.length()) {
                        // Write the request body to the file
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                            writer.write(body);
                        }
                        responseHeaders.append("HTTP/1.1 201 Created\r\n\r\n");
                        httpResponse = responseHeaders.toString();
                    } else {
                        responseHeaders.append("HTTP/1.1 500 Internal Server Error\r\n\r\n");
                        httpResponse = responseHeaders.toString();
                    }
                } else {
                    responseHeaders.append("HTTP/1.1 400 Bad Request\r\n\r\n");
                    httpResponse = responseHeaders.toString();
                }
            }
        } else {
            responseHeaders.append("HTTP/1.1 404 Not Found\r\n\r\n");
            httpResponse = responseHeaders.toString();
        }
        
        return httpResponse;
    }
}
