package rios.gaston.client;
/*
* AskRemote.java
* a) Looks up for the remote object
* b) "Makes" the RMI
*/

import rios.gaston.interfaces.IfaceRemoteClass;
import rios.gaston.multicastRMI.StubExporter;

import java.lang.reflect.Array;
import java.rmi.Naming;
import java.rmi.registry.Registry; /* REGISTRY_PORT */
import java.util.Arrays;
import java.util.List;

public class AskRemote{
    public static void main(String[] args){
        /* Look for hostname and msg length in the command line */
        if (args.length != 1){
            System.out.println("1 argument needed: (remote) hostname");
            System.exit(1);
        }
        try {
            /*
            String rname = "//" + args[0] + ":" + Registry.REGISTRY_PORT + "/remote";
            IfaceRemoteClass remote = (IfaceRemoteClass) Naming.lookup(rname);
            */
            String rname = "remote";
            int bufferlength = 100;
            byte[] buffer = new byte[bufferlength];
            Arrays.fill(buffer, (byte)1);

            // You can create an operation to be cast for each received result
            IfaceRemoteClass remote = (IfaceRemoteClass) StubExporter.lookup(rname,
                    (result)->{System.out.println(Arrays.equals(buffer, (byte[])result));});
            remote.sendThisBack(buffer);

            // Or you can wait for the result to be received
            byte[] arrayBuffer = new byte[bufferlength];
            Arrays.fill(arrayBuffer, (byte)2);
            remote = (IfaceRemoteClass) StubExporter.lookup(rname);
            List<byte[]> results = (List<byte[]>) remote.sendThisBack(arrayBuffer);
            Thread.sleep(2000);
            for (byte[] result:
                 results) {
                System.out.println(Arrays.equals(arrayBuffer, result));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}