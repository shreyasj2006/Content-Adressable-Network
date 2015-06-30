/*
 * RemoteInterface.java
 * v1.0
 * Author: Shreyas Jayanna
 * Date: 03/17/14
 */

// Import statements
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

/**
 * RemoteInterface
 * This interface declares the methods to be implemented by the bootstrap server
 * and the peers in a CAN.
 */
public interface RemoteInterface extends Remote {
    // Implementation in both Bootstrap and Peer
    float[] hashCode(String key) throws RemoteException;
    void view(String nodeID) throws RemoteException, NotBoundException;

    // Implementation only in bootstrap server
    String[] join(String nodeID, String nodeIP, int port, float x, float y) throws RemoteException;

    // Implementation only in the peer
    String[] route(String nodeID, String IP, int port, float x, float y) throws RemoteException, NotBoundException;
    void updateNeighbors(Hashtable<String,String> neighbors) throws RemoteException;
    List<String> insert(String filename, float x, float y) throws RemoteException, NotBoundException;
    List<String> search(String filename, float x, float y) throws RemoteException, NotBoundException;
    float[] getBoundary() throws RemoteException;
    void replaceMeNeighbor(String me, String newNode) throws RemoteException;
    void addNewNeighbor(String newNode) throws RemoteException;
    Hashtable<String,String> getNeighbors() throws RemoteException;
    HashSet<String> getItems() throws RemoteException;
    void updateItems(HashSet<String> fileItems) throws RemoteException;
} // End of RemoteInterface