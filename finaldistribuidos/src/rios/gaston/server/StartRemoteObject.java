package rios.gaston.server;/*
* StartRemoteObject.java
* Starts the remote object. More specifically:
* 1) Creates the object which has the remote methods to be invoked
* 2) Registers the object so that it becomes avaliable
*/
import rios.gaston.interfaces.IfaceRemoteClass;
import rios.gaston.multicastRMI.MulticastRemoteObject;
import rios.gaston.multicastRMI.StubExporter;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry; /* REGISTRY_PORT */
import java.rmi.server.UnicastRemoteObject;

public class StartRemoteObject{
    public static void main (String args[]){
        try{
            /* Create ("start") the object which has the remote method */
            IfaceRemoteClass stub = (IfaceRemoteClass) MulticastRemoteObject.exportObject(new RemoteClass(), 0);
            /* Register the object using Naming.rebind(...) */

            StubExporter.export(stub, "remote");
            /*
            String rname = "//localhost:" + Registry.REGISTRY_PORT + "/remote";
            Naming.rebind(rname, stub);
            */
        } catch (Exception e) {
            System.out.println("Hey, an error occurred at Naming.rebind");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}