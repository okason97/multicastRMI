package rios.gaston.client;
/*
* AskRemote.java
* a) Looks up for the remote object
* b) "Makes" the RMI
*/

import rios.gaston.interfaces.IfaceRemoteClass;
import rios.gaston.multicastRMI.StubExporter;

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
            IfaceRemoteClass remote = (IfaceRemoteClass) StubExporter.lookup(rname);
            System.out.println(remote);

            int bufferlength = 100;
            byte[] buffer = new byte[bufferlength];
            List<Object> a = (List<Object>)remote.sendThisBack(buffer);
            System.out.println(a);
            System.out.println("Done");
            Thread.sleep(2000);
            System.out.println(a);
            System.out.println(Arrays.equals(buffer,(byte[]) a.get(0)));
            System.out.println(Arrays.equals(buffer,(byte[]) a.get(1)));
            System.out.println(Arrays.equals(buffer,(byte[]) a.get(2)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}