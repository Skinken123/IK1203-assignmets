import java.net.*;
import java.io.*;

public class TCPClient {
    //Class variables
    boolean shutdown;
    Integer timeout;
    Integer byteLimit;

    //Constructor
    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.byteLimit = limit;
    }

    //Client method to open communication with servers and close them when needed
    public byte[] askServer(String hostname, int port, byte [] toServerBytes) throws IOException {
        //"Try with resources" statement used to ensure that if an error occurs all resourses will be closed befor catch block
        try(
            //Establish socket and dynamic array for server response
            Socket clientWebSocket = new Socket(hostname, port);
            ByteArrayOutputStream dataFromServer = new ByteArrayOutputStream();
            //Get input and output stream of socket connecting client and server
            InputStream readingFromServer = clientWebSocket.getInputStream();
            OutputStream dataToServer = clientWebSocket.getOutputStream();
        ){
            //Set read timeout for socket if a timeout value was provided
            if (this.timeout != null){
                clientWebSocket.setSoTimeout(this.timeout);
            }
            //Buffer to recive segments from server
            byte[] dataBuffer = new byte[1024];
            int numberOfBytesReadFromBuffer;
            int totalReceivedBytes = 0;
            //If toServerBytes contains data it will be sent to server before it responds
            if (toServerBytes != null && toServerBytes.length > 0) {
                dataToServer.write(toServerBytes);
                dataToServer.flush();
            }
            //Close connection in outgoing direction if shutdown is true
            if (shutdown){
                clientWebSocket.shutdownOutput();
            }
            //Read response from server and store in dynamic ByteArrayOutputStream
            //InputStream.read() will return -1 when buffer is empty i.e. server has finished responding
            //If read does not respond with data with in timer it will timeout and return
            //If received bytes go past the limit the loop will break and only the correct number of bytes will be written to dataFromServer
            try{
                while((numberOfBytesReadFromBuffer = readingFromServer.read(dataBuffer)) != -1){
                    totalReceivedBytes += numberOfBytesReadFromBuffer;
                    if (this.byteLimit != null && totalReceivedBytes > this.byteLimit) {
                        dataFromServer.write(dataBuffer, 0, (numberOfBytesReadFromBuffer - (totalReceivedBytes - this.byteLimit)));
                        break; 
                    }
                    dataFromServer.write(dataBuffer, 0, numberOfBytesReadFromBuffer);
                }
            } catch(SocketTimeoutException exception) {
                System.out.println("Timeout triggered, data was not received quick enough");
            }
            //Return server response in form of a byte array
            return dataFromServer.toByteArray();
        } catch(IOException exception) {
            throw new IOException("Error occured while communicating with server", exception);
        }
    }
}