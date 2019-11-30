package rios.gaston.multicastRMI;

import java.io.*;
import java.rmi.MarshalException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteCall;

public class MulticastRemoteCall implements RemoteCall {
    private int remotePort;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    private TCPConnection tcpConnection;
    private MulticastConnection multicastConnection;
    private String id;

    public MulticastRemoteCall(MulticastConnection c, String id, long hash) throws RemoteException {
        try {
            this.tcpConnection = new TCPConnection();
            this.tcpConnection.openConnection(0);
            this.multicastConnection = c;
            this.id = id;
            this.out = getMulticastOutputStream();
            this.out.writeInt(80);
            this.out.writeInt(this.tcpConnection.getServerPort());
            this.out.writeUTF(this.id);
            this.out.writeLong(hash);
        } catch (IOException var7) {
            throw new MarshalException("Error marshaling call header", var7);
        }
    }

    public MulticastRemoteCall(MulticastConnection c, ObjectInputStream in, int port, String id) {
        this.id = id;
        this.in = in;
        this.remotePort = port;
        this.multicastConnection = c;
    }

    public void releaseOutputStream() throws IOException {
        this.tcpConnection.releaseOutputStream();
        this.out = null;
    }

    @Override
    public ObjectInputStream getInputStream() throws IOException {
        if (this.in == null){
            this.in = this.multicastConnection.getInputStream();
        }
        return this.in;
    }

    @Override
    public void releaseInputStream() throws IOException {
        this.multicastConnection.releaseInputStream();
        this.in = null;
    }

    @Override
    public ObjectOutput getResultStream(boolean success) throws IOException, StreamCorruptedException {
        this.tcpConnection = new TCPConnection();
        this.tcpConnection.newConnection(this.multicastConnection.getRemoteAddress(), this.remotePort);
        this.out = this.tcpConnection.getOutputStream();
        this.out.writeUTF(id);
        if (success){
            this.out.writeByte(0);
        }else{
            this.out.writeByte(1);
        }
        return this.out;
    }

    public void executeCall() throws Exception {
        try {
            this.multicastConnection.releaseOutputStream();
        } finally {
            this.out = null;
        }
    }

    @Override
    public void done() throws IOException {
        if (this.tcpConnection != null){
            this.tcpConnection.close();
        }
    }

    public void finish() throws IOException {
        if (this.tcpConnection != null){
            this.tcpConnection.close();
        }
        if (this.multicastConnection != null){
        this.multicastConnection.close();
        }
    }

    public ObjectOutputStream getOutputStream() throws IOException {
        if (this.out == null){
            this.out = this.tcpConnection.getOutputStream();
        }
        return this.out;
    }

    public ObjectOutputStream getMulticastOutputStream() throws IOException {
        if (this.out == null){
            this.out = this.multicastConnection.getOutputStream();
        }
        return this.out;
    }

    public ObjectInput getResultInputStream() throws IOException {
        if (this.in == null) {
            this.in = this.tcpConnection.getInputStream();
        }
        return this.in;
    }

    public void releaseResultInputStream() throws IOException {
        this.tcpConnection.releaseInputStream();
        this.in = null;
    }

    public void acceptTCPConnection() throws IOException {
        this.tcpConnection.acceptConnection();
    }

    /*
    public void callResult() throws Exception {
        byte returnType;
        MarshalInputStream inputStream;
        try {
            DataInputStream rd = new DataInputStream(this.getConnection().getInputStream());
            byte op = rd.readByte();
            if (op != 81) {
                throw new UnmarshalException("Transport return code invalid");
            }

            inputStream = (MarshalInputStream) this.getInputStream();
            returnType = inputStream.readByte();
            // inputStream.readID();
        } catch (UnmarshalException var11) {
            throw var11;
        } catch (IOException var12) {
            throw new UnmarshalException("Error unmarshaling return header", var12);
        }

        switch (returnType) {
            case 1:
                return;
            case 2:
                Object ex;
                try {
                    ex = inputStream.readObject();
                } catch (Exception var10) {
                    throw new UnmarshalException("Error unmarshaling return", var10);
                }

                if (!(ex instanceof Exception)) {
                    throw new UnmarshalException("Return type not Exception");
                } else {
                    this.exceptionReceivedFromServer((Exception) ex);
                }
            default:
                throw new UnmarshalException("Return code invalid");
        }
    }
     */
}
