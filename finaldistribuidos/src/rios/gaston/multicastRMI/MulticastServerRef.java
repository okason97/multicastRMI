package rios.gaston.multicastRMI;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.*;
import java.rmi.server.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import sun.rmi.runtime.Log;
import sun.rmi.server.*;
import sun.rmi.transport.*;
import sun.rmi.transport.tcp.TCPTransport;

public class MulticastServerRef extends MulticastRef implements ServerRef, Dispatcher {
    public static final boolean logCalls = (Boolean)AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
        return Boolean.getBoolean("java.rmi.server.logCalls");
    });
    public static final Log callLog;
    private static final boolean wantExceptionLog;
    private static final boolean suppressStackTraces;
    private final transient ObjectInputFilter filter;
    private transient Map hashToMethod_Map;
    private static final WeakClassHashMap<Map<Long, Method>> hashToMethod_Maps;
    private final AtomicInteger methodCallIDCount;

    public MulticastServerRef() {
        this.hashToMethod_Map = null;
        this.methodCallIDCount = new AtomicInteger(0);
        this.filter = null;
    }

    public MulticastServerRef(LiveRef ref, ObjectInputFilter filter) {
        super(new MulticastLiveRef());
        this.hashToMethod_Map = null;
        this.methodCallIDCount = new AtomicInteger(0);
        this.filter = filter;
    }

    public MulticastServerRef(int port) {
        super(new MulticastLiveRef());
        this.hashToMethod_Map = null;
        this.methodCallIDCount = new AtomicInteger(0);
        this.filter = null;
    }

    public Remote exportObject(Remote impl, boolean permanent) throws RemoteException {
        Class implClass = impl.getClass();

        Remote stub;
        try {
            stub = Util.createProxy(implClass, this.getClientRef(), false);
        } catch (IllegalArgumentException var7) {
            throw new ExportException("remote object implements illegal remote interface", var7);
        }

        System.out.println("exporting multicast");
        this.multicastRef.listen(new WeakReference<>(impl).get(), this);
        this.hashToMethod_Map = hashToMethod_Maps.get(implClass);
        return stub;
    }

    @Override
    public RemoteStub exportObject(Remote remote, Object o) throws RemoteException {
        throw new ExportException("deprecated.");
    }

    public String getClientHost() throws ServerNotActiveException {
        return TCPTransport.getClientHost();
    }

    public void dispatch(Remote obj, RemoteCall call) throws IOException {
        try {
            ObjectInput in;
            try {
                in = call.getInputStream();
            } catch (Exception var36) {
                throw new UnmarshalException("error unmarshalling call header", var36);
            }

            long op;
            try {
                op = in.readLong();
            } catch (Exception var35) {
                throw new UnmarshalException("error unmarshalling call header", var35);
            }

            Method method = (Method)this.hashToMethod_Map.get(op);
            if (method == null) {
                throw new UnmarshalException("unrecognized method hash: method not supported by remote object");
            }

            Class<?>[] types = method.getParameterTypes();
            Object[] params = new Object[types.length];

            try {
                this.unmarshalCustomCallData(in);

                for(int i = 0; i < types.length; ++i) {
                    params[i] = unmarshalValue(types[i], in);
                }
            } catch (AccessException var37) {
                throw var37;
            } catch (ClassNotFoundException | IOException var38) {
                throw new UnmarshalException("error unmarshalling arguments", var38);
            }

            Object result;
            try {
                result = method.invoke(obj, params);
            } catch (InvocationTargetException var34) {
                throw var34.getTargetException();
            }

            try {
                ObjectOutput out = call.getResultStream(true);
                Class<?> rtype = method.getReturnType();
                if (rtype != Void.TYPE) {
                    marshalValue(rtype, result, out);
                }
            } catch (IOException var33) {
                throw new MarshalException("error marshalling return", var33);
            }
        } catch (Throwable var40) {
            Throwable e = var40;
            this.logCallException(var40);
            ObjectOutput out = call.getResultStream(false);
            if (var40 instanceof Error) {
                e = new ServerError("Error occurred in server thread", (Error)var40);
            } else if (var40 instanceof RemoteException) {
                e = new ServerException("RemoteException occurred in server thread", (Exception)var40);
            }

            if (suppressStackTraces) {
                clearStackTraces((Throwable)e);
            }

            out.writeObject(e);
            if (var40 instanceof AccessException) {
                throw new IOException("Connection is not reusable", var40);
            }
        } finally {
            call.releaseInputStream();
            call.releaseOutputStream();
            call.done();
        }

    }

    protected void unmarshalCustomCallData(ObjectInput in) throws IOException, ClassNotFoundException {
        if (this.filter != null && in instanceof ObjectInputStream) {
            ObjectInputStream ois = (ObjectInputStream)in;
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                ois.setObjectInputFilter(this.filter);
                return null;
            });
        }

    }

    public static void clearStackTraces(Throwable t) {
        for(StackTraceElement[] empty = new StackTraceElement[0]; t != null; t = t.getCause()) {
            t.setStackTrace(empty);
        }

    }

    private void logCallException(Throwable e) {
        if (callLog.isLoggable(Log.BRIEF)) {
            String clientHost = "";

            try {
                clientHost = "[" + this.getClientHost() + "] ";
            } catch (ServerNotActiveException var6) {
            }

            callLog.log(Log.BRIEF, clientHost + "exception: ", e);
        }

        if (wantExceptionLog) {
            PrintStream log = System.err;
            synchronized(log) {
                log.println();
                ObjID var10001 = this.multicastRef.getObjID();
                log.println("Exception dispatching call to " + var10001 + " in thread \"" + Thread.currentThread().getName() + "\" at " + new Date() + ":");
                e.printStackTrace(log);
            }
        }

    }

    public String getRefClass(ObjectOutput out) {
        return "MulticastServerRef";
    }

    protected RemoteRef getClientRef() {
        return new MulticastRef(new MulticastLiveRef());
    }

    public void writeExternal(ObjectOutput out) throws IOException {
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.multicastRef = null;
    }

    static {
        callLog = Log.getLog("sun.rmi.server.call", "RMI", logCalls);
        wantExceptionLog = (Boolean)AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            return Boolean.getBoolean("sun.rmi.server.exceptionTrace");
        });
        suppressStackTraces = (Boolean)AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            return Boolean.getBoolean("sun.rmi.server.suppressStackTraces");
        });
        hashToMethod_Maps = new MulticastServerRef.HashToMethod_Maps();
    }

    private static class HashToMethod_Maps extends WeakClassHashMap<Map<Long, Method>> {
        HashToMethod_Maps() {
        }

        protected Map<Long, Method> computeValue(Class<?> remoteClass) {
            Map<Long, Method> map = new HashMap();

            for(Class cl = remoteClass; cl != null; cl = cl.getSuperclass()) {
                Class[] var4 = cl.getInterfaces();
                int var5 = var4.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    Class<?> intf = var4[var6];
                    if (Remote.class.isAssignableFrom(intf)) {
                        Method[] var8 = intf.getMethods();
                        int var9 = var8.length;

                        for(int var10 = 0; var10 < var9; ++var10) {
                            final Method method = var8[var10];
                            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                                public Void run() {
                                    method.setAccessible(true);
                                    return null;
                                }
                            });
                            map.put(MulticastUtil.computeMethodHash(method), method);
                        }
                    }
                }
            }

            return map;
        }
    }
}
