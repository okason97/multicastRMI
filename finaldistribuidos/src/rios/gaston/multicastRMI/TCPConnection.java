package rios.gaston.multicastRMI;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPConnection implements Connection{
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public void newConnection(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.clientSocket = this.serverSocket.accept();
    }

    public void openConnection(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public void acceptConnection() throws IOException {
        this.clientSocket = this.serverSocket.accept();
    }

    public int getServerPort(){
        return this.serverSocket.getLocalPort();
    }

    public void newConnection(String ip, int port) throws IOException {
        this.clientSocket = new Socket(ip, port);
    }

    public void newConnection(InetAddress ip, int port) throws IOException {
        this.clientSocket = new Socket(ip, port);
    }

    @Override
    public void close() throws IOException {
        if (this.in != null)
            this.in.close();
        if (this.out != null)
            this.out.close();
        if (this.clientSocket != null)
            this.clientSocket.close();
        if (this.serverSocket != null)
            this.serverSocket.close();
    }

    @Override
    public ObjectOutputStream getOutputStream() throws IOException {
        if (this.out == null)
            this.out = new ObjectOutputStream(this.clientSocket.getOutputStream());
        return this.out;
    }

    @Override
    public void releaseOutputStream() throws IOException {
        this.out.flush();
        this.out = null;
    }

    @Override
    public ObjectInputStream getInputStream() throws IOException {
        if (this.in == null)
            this.in = new ObjectInputStream(new BufferedInputStream(this.clientSocket.getInputStream()));
        return this.in;
    }

    public void releaseInputStream() throws IOException {
        this.clientSocket.close();
        this.clientSocket = null;
        this.in = null;
    }
}
