package rios.gaston.server;/*
* RemoteClass.java
* Just implements the RemoteMethod interface as an extension to
* UnicastRemoteObject
*
*/
/* Needed for implementing remote method/s */

import rios.gaston.interfaces.IfaceRemoteClass;

import java.rmi.RemoteException;

/* This class implements the interface with remote methods */
public class RemoteClass implements IfaceRemoteClass {
    
    protected RemoteClass() throws RemoteException{
        super();
    }

    /* Remote method implementation */
    public byte[] sendThisBack(byte[] data) throws RemoteException{
        System.out.println("Data back to client");
        return data;
    }
}