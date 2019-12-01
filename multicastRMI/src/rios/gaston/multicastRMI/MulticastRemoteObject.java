package rios.gaston.multicastRMI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.*;

public class MulticastRemoteObject extends RemoteObject {
    private int port;

    protected MulticastRemoteObject(int port) throws RemoteException {
        super();
        this.port = port;
        exportObject(this, port);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.reexport();
    }

    public Object clone() throws CloneNotSupportedException {
        try {
            MulticastRemoteObject cloned = (MulticastRemoteObject)super.clone();
            cloned.reexport();
            return cloned;
        } catch (RemoteException var2) {
            throw new ServerCloneException("Clone failed", var2);
        }
    }

    private void reexport() throws RemoteException {
        exportObject(this, this.port);
    }

    public static Remote exportObject(Remote obj, int port) throws RemoteException {
        return exportObject(obj, new MulticastServerRef(port));
    }

    private static Remote exportObject(Remote obj, MulticastServerRef sref) throws RemoteException {
        if (obj instanceof MulticastRemoteObject) {
            ((MulticastRemoteObject)obj).ref = sref;
        }

        return sref.exportObject(obj, false);
    }
}
