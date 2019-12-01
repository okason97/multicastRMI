package rios.gaston.multicastRMI;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastConnection implements Connection {
    private MulticastSocket socket;
    private int port;
    private String multicastAddress;
    private InetAddress address;
    private DatagramPacket packet;
    private ByteArrayOutputStream out;
    private ObjectOutputStream outputStream;
    private static final int packageSize = 512;

    public MulticastConnection(int port, String address) {
        this.port = port;
        this.multicastAddress = address;
        this.out = new ByteArrayOutputStream();
        this.packet = null;
    }

    public String toString() {
        return this.address + ":" + this.port;
    }

    public ObjectOutputStream getOutputStream() throws IOException {
        this.outputStream = new ObjectOutputStream(this.out);
        return this.outputStream;
    }

    public void releaseOutputStream() throws IOException {
        byte[] byteBuf = new byte[packageSize];
        this.outputStream.flush();
        byte[] byteOut = this.out.toByteArray();
        System.arraycopy(byteOut,0,byteBuf,0,byteOut.length);
        DatagramPacket packet=new DatagramPacket(byteBuf,packageSize,this.address,this.port);
        this.socket.send(packet);
        this.out = new ByteArrayOutputStream();
        this.outputStream.close();
        this.outputStream = null;
    }

    public void close() throws IOException {
        this.out = new ByteArrayOutputStream();
        this.packet = null;
        this.socket.leaveGroup(address);
        this.socket.close();
    }

    public ObjectInputStream getInputStream() throws IOException {
        byte[] byteBuf = new byte[packageSize];
        this.packet = new DatagramPacket(byteBuf, byteBuf.length);
        socket.receive(this.packet);
        byte[] in = this.packet.getData();
        return new ObjectInputStream(new ByteArrayInputStream(in));
    }

    public InetAddress getRemoteAddress() throws IOException {
        if (this.packet != null) {
            return this.packet.getAddress();
        }else{
            throw new IOException("No remote");
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof MulticastConnection) {
            MulticastConnection ref = (MulticastConnection)obj;
            return this.port == ref.port && this.multicastAddress.equals(ref.multicastAddress);
        } else {
            return false;
        }
    }

    public void newConnection() throws IOException {
        this.socket = new MulticastSocket(port);
        this.address = InetAddress.getByName(this.multicastAddress);
        this.socket.joinGroup(address);
    }

    public void releaseInputStream() throws IOException {
    }

}