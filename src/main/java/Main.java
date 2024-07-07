import lombok.ToString;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ToString
public class HttpRequest {
    private static final Logger LOGGER = Logger.getLogger("HttpRequest");
    private RequestType requestType;
    private String path;
    private Protocol protocol;
    private double version;
    private String requestBody;
    private Map<String, String> headers = new HashMap<>();

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getRequestTarget() {
        return path;
    }

    public void setRequestTarget(String path) {
        this.path = path;
    }

    public String getProtocolVersion() {
        return protocol + "/" + version;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public double getVersion() {
        return version;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    public HttpRequest addHeader(String key, String value) {
        assert key != null : "key cannot be null";
        assert value != null : "value cannot be null";
        headers.put(key, value);
        return this;
    }

    public String getHeader(String key) {
        if (!headers.containsKey(key))
            throw new IllegalArgumentException("Key " + key + " doesn't exist");
        return headers.get(key);
    }

    public boolean containsHeader(String key) {
        return headers.containsKey(key);
    }

    public void readRequestLine(String requestLine) {
        String[] requestLineArr = requestLine.split(" ");
        assert requestLineArr.length==3 : "Request Line should have three values: RequestType, RequestTarget and HttpVersion";
        this.setRequestType(RequestType.valueOf(requestLineArr[0]));
        this.setRequestTarget(requestLineArr[1]);
        this.setProtocol(Protocol.valueOf(requestLineArr[2].substring(0, requestLineArr[2].indexOf("/"))));
        assert requestLineArr[2].indexOf("/")+1 < requestLineArr.length : "HttpVersion string should have version number";
        this.setVersion(Double.parseDouble(requestLineArr[2].substring(requestLineArr[2].indexOf("/")+1)));
    }


    public static HttpRequest  fromInputStream(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            HttpRequest httpRequest = new HttpRequest();

            //Read RequestLine
            String requestLine = reader.readLine();
            if (requestLine != null && !requestLine.isEmpty()) {
                httpRequest.readRequestLine(requestLine);
            }

            //Read Headers
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String[] keyValuePair = line.split(": ");
                if (keyValuePair.length == 2)
                    httpRequest.addHeader(keyValuePair[0], keyValuePair[1]);
            }

            // Read Body
            StringBuffer bodyBuffer = new StringBuffer();
            while (reader.ready()) {
                bodyBuffer.append((char) reader.read());
            }
            String body = bodyBuffer.toString();

            httpRequest.setRequestBody(body);

            return httpRequest;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}