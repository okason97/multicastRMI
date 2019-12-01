package rios.gaston.server;

import rios.gaston.interfaces.IfaceRemoteClass;
import rios.gaston.multicastRMI.MulticastRemoteObject;
import rios.gaston.multicastRMI.StubExporter;

public class StartRemoteObject{
    public static void main (String args[]){
        try{
            /* Start listening for calls to the remote method and get the stub to export it */
            IfaceRemoteClass stub = (IfaceRemoteClass) MulticastRemoteObject.exportObject(new RemoteClass(), 0);

            /* Export the stub */
            StubExporter.export(stub, "remote");
        } catch (Exception e) {
            System.out.println("Hey, an error occurred at Naming.rebind");
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}