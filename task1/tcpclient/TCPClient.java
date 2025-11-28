package task1.tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    
    public TCPClient() {
    }

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
            //Buffer to recive segments from server
            byte[] dataBuffer = new byte[1024];
            int numberOfBytesReadFromBuffer;
            //If toServerBytes contains data it will be sent to server before it responds
            if (toServerBytes != null && toServerBytes.length > 0) {
                dataToServer.write(toServerBytes);
                dataToServer.flush();
            }
            //Read response from server and store in dynamic ByteArrayOutputStream
            //InputStream.read() will return -1 when buffer is empty i.e. server has finished responding
            while((numberOfBytesReadFromBuffer = readingFromServer.read(dataBuffer)) != -1){
                dataFromServer.write(dataBuffer, 0, numberOfBytesReadFromBuffer);
            }
            //Return server response in form of a byte array
            return dataFromServer.toByteArray();
        } catch(IOException exception) {
            throw new IOException("Error occured while communicating with server", exception);
        }
    }

    public byte[] askServer(String hostname, int port) throws IOException {
        return askServer(hostname, port, null);
    }

}