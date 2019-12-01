package rios.gaston.client;

import rios.gaston.interfaces.IfaceRemoteClass;
import rios.gaston.multicastRMI.StubExporter;

import java.util.Arrays;
import java.util.List;

public class AskRemote{
    public static void main(String[] args){
        try {
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