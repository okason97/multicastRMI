package rios.gaston.multicastRMI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.lang.reflect.Proxy;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class StubExporter {
    private static final int defaultPort = 8080;
    private static final String defaultAddress = "228.5.6.7";

    private StubExporter(){}

    public static void export(Remote stub, String name) throws IOException {
        final MulticastConnection conn = new MulticastConnection(defaultPort, defaultAddress);
        conn.newConnection();
        ExecutorService executor = Executors.newCachedThreadPool();
        CompletableFuture.runAsync(() -> {
            while(true){
                try {
                    ObjectInputStream in = conn.getInputStream();

                    int op = in.readInt();
                    if (op == 1){
                        System.out.println("correct operation");
                        String remoteName = in.readUTF();
                        if (remoteName.equals(name)){
                            System.out.println("correct name, begin sending stub");
                            final TCPConnection tcpConn = new TCPConnection();
                            tcpConn.newConnection(conn.getRemoteAddress(), in.readInt());
                            ObjectOutputStream out = tcpConn.getOutputStream();
                            out.writeInt(1);
                            out.writeObject(stub);
                            tcpConn.releaseOutputStream();
                            tcpConn.close();
                            System.out.println("stub sent");
                        }
                    }else{
                        throw new IOException("unknown transport op " + op);
                    }
                } catch (IOException e) {
                    System.out.println("Failure listening stub request!");
                }
            }
        },executor);
    }

    public static Object lookup(String rname) throws IOException, ClassNotFoundException, ExecutionException, InterruptedException {
        TCPConnection tcpConn = new TCPConnection();
        tcpConn.openConnection(0);
        CompletableFuture ulforce = CompletableFuture.runAsync(() -> {
            try {
                tcpConn.acceptConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        final MulticastConnection conn = new MulticastConnection(defaultPort, defaultAddress);
        conn.newConnection();
        ObjectOutputStream out = conn.getOutputStream();
        out.writeInt(1);
        out.writeUTF(rname);
        out.writeInt(tcpConn.getServerPort());
        conn.releaseOutputStream();
        conn.close();
        ulforce.get();
        ObjectInputStream in = tcpConn.getInputStream();
        int op = in.readInt();
        if (op == 1){
            Object stub = in.readObject();
            tcpConn.close();
            return stub;
        }else{
            tcpConn.close();
            throw new IOException("unknown transport op " + op);
        }
    }

    public static Object lookup(String rname, Consumer<Object> op) throws IOException, ClassNotFoundException, ExecutionException, InterruptedException {
        Object stub = lookup(rname);
        RemoteObjectInvocationHandler handler = (RemoteObjectInvocationHandler)Proxy.getInvocationHandler(stub);
        MulticastRef ref = (MulticastRef)handler.getRef();
        ref.setResultOp(op);
        return stub;
    }

}
