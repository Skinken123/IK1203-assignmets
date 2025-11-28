import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

/*
Client input: http://localhost:8888/ask?hostname=whois.iis.se&port=43&string=kth.se
Client socket input stream example:
GET /ask?hostname=whois.iis.se&port=43&string=kth.se HTTP/1.1\r\n
Host: localhost:8888\r\n
User-Agent: Mozilla/5.0 (Your Browser Details)\r\n
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,/*;q=0.8\r\n
Accept-Language: en-US,en;q=0.5\r\n
Accept-Encoding: gzip, deflate\r\n
Connection: keep-alive\r\n
\r\n
 */

public class HTTPAsk {
    static boolean shutdown = false;             // True if client should shutdown connection
	static Integer timeout = null;			     // Max time to wait for data from server (null if no limit)
	static Integer limit = null;			     // Max no. of bytes to receive from server (null if no limit)
	static String hostname = null;			     // Domain name of server
	static int port = 0;					     // Server port number
	static byte[] userInputBytes = new byte[0];  // Data to send to server
    public static void main( String[] args) throws IOException {
        int portNumber = Integer.parseInt(args[0]);
        try(ServerSocket serverSocket = new ServerSocket(portNumber);){
            while (true) {
                try(Socket clientConnectionSocket = serverSocket.accept();) {
                    //necessary tools to read client request
                    ByteArrayOutputStream requestData = new ByteArrayOutputStream();
                    byte[] inputBuffer = new byte[1024];
                    int nrOfBytesRead;
                    InputStream dataFromClient = clientConnectionSocket.getInputStream();
                    OutputStream sendData = clientConnectionSocket.getOutputStream();
                    String dataReceivedSoFar;
                    //Extract and store client request
                    while ((nrOfBytesRead = dataFromClient.read(inputBuffer)) != -1) {
                        requestData.write(inputBuffer, 0, nrOfBytesRead);

                        //"\r\n\r\n" signals that the HTTP GET request has ended
                        dataReceivedSoFar = requestData.toString("UTF-8");
                        if (dataReceivedSoFar.contains("\r\n\r\n")){
                            break;
                        }
                    }

                    //Parse request to get query params for server ask
                    String receivedRequest = requestData.toString("UTF-8");
                    try{
                        extractQueryParams(receivedRequest, sendData);
                    } catch (NumberFormatException exception) {
                        sendResponse(sendData, 400, "400 BAD REQUEST: wrong parameter type (port, limit, timeout)");
                    }

                     // Call TCPClient
                     try {
                        TCPClient tcpClient = new TCPClient(shutdown, timeout, limit);
                        byte[] responseData = tcpClient.askServer(hostname, port, userInputBytes);

                        sendResponse(sendData, 200, new String(responseData, StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        sendResponse(sendData, 404, "404 NOT FOUND: Error contacting server");
                    } 
                } catch(IOException exception) {
                    System.err.println("Error establishing client connection " + exception.getMessage());
                } 
            }
        } catch(IOException exception) {
            System.err.println("Error starting server socket " + exception.getMessage());
        }
    }

    private static void extractQueryParams(String clientRequest, OutputStream out) throws IOException, NumberFormatException{
        if (!(clientRequest.contains("\r\n\r\n"))){
            //Request was incomplete code 400 Bad Request
            sendResponse(out, 400, "400 BAD REQUEST: Incomplete request");
        }
        String[] requestParts = clientRequest.split("\r\n");
        String requestLine1 = requestParts[0];
        //Checks for all mandatory components of first request line
        if (!(requestLine1.contains("GET") && requestLine1.contains("hostname=") && requestLine1.contains("port=") && requestLine1.contains("HTTP/1.1"))){
            //Request was incomplete code 400 Bad Request
            sendResponse(out, 400, "400 BAD REQUEST: Wrong type of request (GET) or missing parameters (hostname, port) or no specified transport protocol (HTTP/1.1)");
        }
        if (!(requestLine1.contains("/ask?"))){
            sendResponse(out, 404, "404 NOT FOUND: URI does not contain /ask?");
        }
        //Extract URI components from request line 1
        String temp = requestLine1.replace("GET /ask?", "");
        String uriComponents = temp.replace(" HTTP/1.1", "");
        String[] componentArray = uriComponents.split("&");
        int numberOfElements = componentArray.length;
        String[] temporary;
        for (int i = 0; i < numberOfElements; i++){
            if (componentArray[i].contains("hostname")) {
                temporary = componentArray[i].split("=");
                hostname = temporary[1];
            } else if (componentArray[i].contains("port")) {
                temporary = componentArray[i].split("=");
                port = Integer.parseInt(temporary[1]);
            } else if (componentArray[i].contains("string")) {
                temporary = componentArray[i].split("=");
                userInputBytes = temporary[1].getBytes(StandardCharsets.UTF_8); 
            } else if (componentArray[i].contains("shutdown")) {
                temporary = componentArray[i].split("=");
                if (temporary[1].contains("true")) shutdown = true;
                else shutdown = false;
            } else if (componentArray[i].contains("limit")) {
                temporary = componentArray[i].split("=");
                limit = Integer.parseInt(temporary[1]);
            } else if (componentArray[i].contains("timeout")) {
                temporary = componentArray[i].split("=");
                timeout = Integer.parseInt(temporary[1]);
            } else {
                //Request has invalid parameter code 400 Bad Request
                sendResponse(out, 400, "400 BAD REQUEST: Invalid parameter type");
            }
        }
    }

    private static void sendResponse(OutputStream outputStream, int statusCode, String message) throws IOException {
        String statusLine = "HTTP/1.1 " + statusCode + " " + getStatusMessage(statusCode) + "\r\n";
        String headers = "Content-Type: text/plain; charset=utf-8\r\n" +
                         "Content-Length: " + message.length() + "\r\n" +
                         "\r\n";
        outputStream.write(statusLine.getBytes(StandardCharsets.UTF_8));
        outputStream.write(headers.getBytes(StandardCharsets.UTF_8));
        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static String getStatusMessage(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 400 -> "BAD REQUEST";
            case 404 -> "NOT FOUND";
            default -> "INTERNAL SERVER ERROR";
        };
    }
}