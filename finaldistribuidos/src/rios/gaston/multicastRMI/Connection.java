package rios.gaston.multicastRMI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface Connection {
    void close() throws IOException;
    ObjectOutputStream getOutputStream() throws IOException;
    void releaseOutputStream() throws IOException;
    ObjectInputStream getInputStream() throws IOException ;
}
