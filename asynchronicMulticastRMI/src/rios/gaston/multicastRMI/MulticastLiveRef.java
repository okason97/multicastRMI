package rios.gaston.multicastRMI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ObjID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sun.rmi.server.Dispatcher;

public class MulticastLiveRef {
    private MulticastConnection conn;
    private static final int defaultPort = 8080;
    private static final String defaultAddress = "228.5.6.7";
    private ObjID id;

    public MulticastLiveRef(){
        this(new ObjID(), new MulticastConnection(defaultPort, defaultAddress));
    }

    public MulticastLiveRef(ObjID objID, MulticastConnection conn) {
        this.id = objID;
        this.conn = conn;
    }

    public ObjID getObjID() {
        return id;
    }

    public void newUDPConnection() throws IOException {
        this.conn.newConnection();
    }

    public MulticastConnection getMulticastConnection() {
        return conn;
    }

    public String toString() {
        return "[connection:" + this.conn + "]";
    }

    public boolean equals(Object obj) {
        if (obj instanceof MulticastLiveRef) {
            MulticastLiveRef ref = (MulticastLiveRef)obj;
            return this.conn.equals(ref.conn);
        } else {
            return false;
        }
    }

    public void listen(Remote impl, Dispatcher disp) throws RemoteException {
        try{
            this.conn.newConnection();
        }catch (IOException e) {
            throw new RemoteException();
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        CompletableFuture.runAsync(() -> {
            while(true){
                try {
                    final ObjectInputStream in = this.conn.getInputStream();
                    int op = in.readInt();
                    if (op == 80){
                        System.out.println("Dispatching method");
                        int port = in.readInt();
                        String remoteId = in.readUTF();;
                        MulticastRemoteCall call = new MulticastRemoteCall(this.conn, in, port, remoteId);
                        disp.dispatch(impl, call);
                    }else{
                        System.out.println("No method: unknown transport op " + op);
                    }
                } catch (IOException e) {
                    // System.out.println("Failure listening method!");
                }
            }
        }, executor);
    }
}
