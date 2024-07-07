[![progress-banner](https://backend.codecrafters.io/progress/http-server/24aa7562-2b8a-477e-8ab6-dae4765065eb)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

This Java program implements a basic HTTP server that listens on port 4221 and serves requests from clients. <br>The server handles GET and POST requests and can return different types of responses based on the request URL.

Directory Setting: The server can be started with a --directory argument to specify the directory for file operations.

Request Handling:<br><br>
• " GET / " : Returns a simple HTTP 200 OK response.<br>
•"GET /echo/<message> " : Returns the <message> in the response body. Supports gzip compression if requested by the client.<br>
•"GET /user-agent " : Returns the user-agent string from the client's request headers.<br>
•"GET /files/<filename> " : Returns the contents of the specified file from the configured directory. Returns a 404 response if the file is not found.<br>
•"POST /files/<filename> " : Saves the request body content to the specified file in the configured directory and returns a 201 Created response.<br>
•"Response Headers : Appropriate HTTP response headers, including Content-Length and Content-Type, are included based on the request and response content.<br>
•Error Handling : Handles 404 Not Found for invalid paths and 500 Internal Server Error for server-side issues.<br>

The server uses basic socket programming for network communication and handles multiple client connections in a loop.

