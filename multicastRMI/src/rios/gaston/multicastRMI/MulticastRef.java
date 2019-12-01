package rios.gaston.multicastRMI;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.rmi.MarshalException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.UUID;
import java.util.function.Consumer;

public class MulticastRef implements RemoteRef {
    protected MulticastLiveRef multicastRef;
    private static final int timeOut = 15;
    private Consumer<Object> resultOp = null;

    public MulticastRef() {
        this.multicastRef = new MulticastLiveRef();
    }

    public MulticastRef(MulticastLiveRef multicastLiveRef) {
        this.multicastRef = multicastLiveRef;
    }

    public void setResultOp(Consumer<Object> resultOp){
        this.resultOp = resultOp;
    }
    // HANDLER DE LO QUE INVOCA EL CLIENTE
    //
    public Object invoke(Remote obj, Method method, Object[] params, long opnum) throws Exception {
        final String uniqueID = UUID.randomUUID().toString();
        MulticastRemoteCall call;
        synchronized (this){
            this.multicastRef.newUDPConnection();
            call = new MulticastRemoteCall(this.multicastRef.getMulticastConnection(), uniqueID, opnum);;

            try {
                ObjectOutput out = call.getMulticastOutputStream();
                Object in = method.getParameterTypes();

                for(int i = 0; i < ((Object[])in).length; ++i) {
                    marshalValue((Class)((Object[])in)[i], params[i], out);
                }
            } catch (IOException var39) {
                throw new MarshalException("error marshalling arguments", var39);
            }

            call.executeCall();
            call.closeMulticast();
        }

        List<Object> result = Collections.synchronizedList(new ArrayList<>());
        Class<?> rtype = method.getReturnType();
        final MulticastRemoteCall finalCall = call;
        ExecutorService resultExecutor = Executors.newCachedThreadPool();
        CompletableFuture.runAsync(() -> {

            ExecutorService executor = Executors.newSingleThreadExecutor();

            Future<Integer> future = executor.submit(()->{
                ObjectInput input;
                while(true){
                    finalCall.acceptTCPConnection();
                    input = finalCall.getResultInputStream();
                    String remoteId = input.readUTF();
                    if (uniqueID.equals(remoteId)){
                        System.out.println("Got new result");
                        byte success = input.readByte();
                        if (success == (byte)0){
                            Object returnValue = unmarshalValue(rtype, input);
                            synchronized (result) {
                                if (this.resultOp != null)
                                    this.resultOp.accept(returnValue);
                                result.add(returnValue);
                            }
                        }else{
                            Throwable returnValue = (Throwable)input.readObject();
                            synchronized (result) {
                                if (this.resultOp != null)
                                    this.resultOp.accept(returnValue);
                                result.add(returnValue);
                            }
                        }
                    }
                    finalCall.releaseResultInputStream();
                }
            });

            try {
                future.get(timeOut, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            executor.shutdownNow();
        }, resultExecutor).whenCompleteAsync((s,t)->{
            if(t!=null){
                t.printStackTrace();
            }
        });
        return result;
    }

    @Override
    public RemoteCall newCall(RemoteObject remoteObject, Operation[] operations, int i, long l) throws RemoteException {
        return null;
    }

    @Override
    public void invoke(RemoteCall remoteCall) throws Exception {

    }

    @Override
    public void done(RemoteCall remoteCall) throws RemoteException {

    }

    protected static void marshalValue(Class<?> type, Object value, ObjectOutput out) throws IOException {
        if (type.isPrimitive()) {
            if (type == Integer.TYPE) {
                out.writeInt((Integer)value);
            } else if (type == Boolean.TYPE) {
                out.writeBoolean((Boolean)value);
            } else if (type == Byte.TYPE) {
                out.writeByte((Byte)value);
            } else if (type == Character.TYPE) {
                out.writeChar((Character)value);
            } else if (type == Short.TYPE) {
                out.writeShort((Short)value);
            } else if (type == Long.TYPE) {
                out.writeLong((Long)value);
            } else if (type == Float.TYPE) {
                out.writeFloat((Float)value);
            } else {
                if (type != Double.TYPE) {
                    throw new Error("Unrecognized primitive type: " + type);
                }

                out.writeDouble((Double)value);
            }
        } else {
            out.writeObject(value);
        }

    }

    protected static Object unmarshalValue(Class<?> type, ObjectInput in) throws IOException, ClassNotFoundException {
        if (type.isPrimitive()) {
            if (type == Integer.TYPE) {
                return in.readInt();
            } else if (type == Boolean.TYPE) {
                return in.readBoolean();
            } else if (type == Byte.TYPE) {
                return in.readByte();
            } else if (type == Character.TYPE) {
                return in.readChar();
            } else if (type == Short.TYPE) {
                return in.readShort();
            } else if (type == Long.TYPE) {
                return in.readLong();
            } else if (type == Float.TYPE) {
                return in.readFloat();
            } else if (type == Double.TYPE) {
                return in.readDouble();
            } else {
                throw new Error("Unrecognized primitive type: " + type);
            }
        } else {
            return in.readObject();
        }
    }

    public String getRefClass(ObjectOutput out) {
        return "";
    }

    @Override
    public int remoteHashCode() {
        return 0;
    }

    @Override
    public boolean remoteEquals(RemoteRef remoteRef) {
        return false;
    }

    @Override
    public String remoteToString() {
        return null;
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {

    }
}

