import java.net.*;
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

public class ConcHTTPAsk {
    public static void main( String[] args) throws IOException{
        int portNumber = Integer.parseInt(args[0]);
        try(ServerSocket serverSocket = new ServerSocket(portNumber);){
            while (true) {
                //Can not put client socket creation code in try with resources statement,
                //since it will close the socket when try block has been executed, which will be immediately
                try {
                    Socket clientConnectionSocket = serverSocket.accept();
                    Thread newThread = new Thread(new MyRunnable(clientConnectionSocket));
                    newThread.start();
                } catch(IOException exception) {
                    System.err.println("Error establishing client connection " + exception.getMessage());
                } 
            }
        } catch(IOException exception) {
            System.err.println("Error starting server socket " + exception.getMessage());
        }
    }
}
