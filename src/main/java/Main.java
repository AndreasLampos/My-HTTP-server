import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class Main {
    public static void handleHttpRequest(Socket clientSocket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String requestLine = reader.readLine(); // Read the request line

        if (requestLine != null) {
            System.out.println("Request Line: " + requestLine);

            // Split the request line into parts
            String[] parts = requestLine.split("\\s+");

            if (parts.length >= 2) {
                String method = parts[0];
                String path = parts[1];
                System.out.println("Method: " + method);
                System.out.println("Path: " + path);
                // Read the headers
                String headerLine;
                String userAgent = null;
                String data =  null;
                int contentLength = 0;
                StringBuilder body = new StringBuilder();
                while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                    System.out.println("Header: " + headerLine);
                    if (headerLine.startsWith("User-Agent:")) {
                        userAgent = headerLine.substring(12).trim();
                        System.out.println("User-Agent: " + userAgent);
                    }
                    if (headerLine.startsWith("Content-Length")){
                        contentLength = Integer.parseInt(headerLine.substring(16).trim()) + 2;
                    }
                    if (headerLine.startsWith("Content-Type")) {
                        char[] buffer = new char[contentLength];
                        reader.read(buffer, 0, contentLength);
                        body.append(buffer);
                        System.out.println("Body: " + body);
                        break;
                    }
                }

                switch (method){
                    case "GET":  // Send HTTP/1.1 200 OK response
                        sendHttpResponse(clientSocket, path, userAgent);
                        break;
                    case "POST":
                        handlePostRequest(clientSocket, path,body.toString().trim());
                        break;
                }
            }
        }
    }


    private static void handlePostRequest(Socket clientSocket, String path, String body) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream,true);
        String httpResponse = "HTTP/1.1 201 Created\r\n\r\n";
        String[] render = path.split("/");
        File file = new File("/tmp/data/codecrafters.io/http-server-tester/" + render[2].trim());
        // Use FileWriter and BufferedWriter to write to the file
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw)) {
            // Write content to the file
            bw.write(body);
            System.out.println("File written successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("Body: " + body);
        writer.println(httpResponse);
        writer.flush();
    }

    public static void sendHttpResponse(Socket clientSocket, String path,String userAgent) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);
        // Prepare HTTP response
        String httpResponse = "";
        String[] render = path.trim().split("/");
        if (path.equals("/")){
            httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
        }else {
            if (render.length > 2) {
                if (render[1].equals("files")){
                    try {
                        String fileName = render[2].trim();
                        File file = new File("/tmp/data/codecrafters.io/http-server-tester/" + fileName);
                            byte[] fileBytes = Files.readAllBytes(file.toPath());
                            String body = new String(fileBytes);

                            httpResponse = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/octet-stream\r\n" +
                                    "Content-Length: " + fileBytes.length + "\r\n" +
                                    "\r\n" +
                                    body;

                    }catch (Exception e){
                        httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                    }
                }else {
                    String toRender = render[2].trim();
                    httpResponse = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + toRender.length() + "\r\n" +
                            "\r\n" +
                            toRender;
                }
            }else if ((path.equals("/user-agent"))) {
                httpResponse = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + userAgent.length() + "\r\n" +
                        "\r\n" +
                        userAgent;
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        }
        // Send HTTP response
        writer.println(httpResponse);
        writer.flush();
    }


}