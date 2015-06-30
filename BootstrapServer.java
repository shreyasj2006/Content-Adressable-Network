/*
 * BootstrapServer.java
 * v1.0
 * Author: Shreyas Jayanna
 * Date: 03/17/14
 */

// Import statements
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * class BootstrapServer
 * This class implements the functionalities of a bootstrap server
 * This class extends UnicastRemoteObject and implements RemoteInterface
 */
public class BootstrapServer extends UnicastRemoteObject implements RemoteInterface{

    Hashtable<String, String> peers;
    Registry registry;
    int port;

    // Constructor
    BootstrapServer(int port) throws RemoteException, AlreadyBoundException {
        peers = new Hashtable();
        this.port = port;
        registry = LocateRegistry.createRegistry(port);
        registry.bind("bootstrap",this);
    }

    // REturns the hashcode of filename
    @Override
    public float[] hashCode(String key) throws RemoteException {
        float[] coord = new float[2];
        int keyLength = key.length();
        int xValue = 0;
        int yValue = 0;
        for(int i = 0; i < keyLength; i++) {
            if(i%2 == 0) {
                yValue += key.charAt(i);
            } else {
                xValue += key.charAt(i);
            }
        }

        coord[0] = xValue % 10;
        coord[1] = yValue % 10;

        return coord;
    }

    // Join - Picks up a random node form the network and returns the IP and port address
    // of that node to the new node. If no nodes are present in the network, informs the
    // new node accordingly
    @Override
    public String[] join(String nodeID, String nodeIP, int port, float x, float y) {
        String[] routeNodeInfo = new String[12];
        routeNodeInfo[0] = "F";
        if(peers.size() > 0) {
            Random random = new Random();
            List<String> keys = new ArrayList<String>(peers.keySet());
            String randomPeer = keys.get(random.nextInt(peers.size()));
            String randomInfo = (String) peers.get(randomPeer);

            String[] peerInfo = randomInfo.split(";");

            routeNodeInfo[0] = "NF";
            routeNodeInfo[1] = randomPeer;
	    routeNodeInfo[2] = peerInfo[0];
	    routeNodeInfo[3] = peerInfo[1];
	    routeNodeInfo[4] = "No";

	}
	String insertNode = nodeIP + ";" + port + ";" + x + ";" + y;
	peers.put(nodeID,insertNode);
        
        return routeNodeInfo;
    }

    // Implementation in Peer only
    @Override
    public String[] route(String nodeID, String IP, int port, float x, float y) {
        return new String[0];
    }

    // Implementation in Peer only
    @Override
    public void updateNeighbors(Hashtable<String,String> neighbors) throws RemoteException {
    }

    // Implementation in Peer only
    @Override
    public List<String> insert(String filename, float x, float y) throws RemoteException {
        return new ArrayList<String>();
    }

    // Implementation in Peer only
    @Override
    public List<String> search(String filename, float x, float y) throws RemoteException, NotBoundException {
        return null;
    }

    // Implementation in Peer only
    @Override
    public float[] getBoundary() throws RemoteException {
        return new float[0];
    }

    // Implementation in Peer only
    @Override
    public void replaceMeNeighbor(String me, String newNode) throws RemoteException {

    }

    // Implementation in Peer only
    @Override
    public void addNewNeighbor(String newNode) throws RemoteException {

    }

    //Implementation in Peer only
    @Override
    public void updateItems(HashSet<String> fileItems) throws RemoteException {
    }

    // View - If nodeID is passed, displays information about that node.
    // If no nodeID is passed, displays information about all the nodes in the network
    @Override
    public void view(String nodeID) throws RemoteException, NotBoundException {
        //Print node ID, IP, coord, neighbors, items
        if(nodeID == null) {
	    ArrayList<String> nodes = Collections.list(peers.keys());

            int index = 0;
	    System.out.println("********************************************************************");
            for(String nodeID1 : nodes) {
                System.out.println("Node ID: " + nodeID1);
		System.out.println("------------------------------------------");
                String[] nodeInfo = peers.get(nodeID1).split(";");
		/*
		for(String text : nodeInfo) {
		    System.out.println(text);
		}
		*/
                System.out.println("IP address: " + nodeInfo[0] );
		System.out.println("Coordinates: (" + nodeInfo[2] + "," + nodeInfo[3] + ")");

                Registry aRegistry = LocateRegistry.getRegistry(nodeInfo[0], Integer.parseInt(nodeInfo[1]));
                RemoteInterface aNode = (RemoteInterface) aRegistry.lookup(nodeID1);

                Hashtable<String,String> neighbors = aNode.getNeighbors();
		ArrayList<String> nodeNeighbors = Collections.list(neighbors.keys());
                System.out.println("Neighbors: " + nodeNeighbors);

                HashSet<String> items = aNode.getItems();
                Object[] nodeItems = items.toArray();
                if(nodeItems.length > 0) {
                    System.out.println("Items at this node: " );
                    for(Object aObject : nodeItems) {
                        System.out.println(aObject.toString());
                    }
                } else {
                    System.out.println("No items at this node");
                }
		System.out.println("###################################################################");
            }
        } else {
            if(this.peers.containsKey(nodeID)) {
		System.out.println("********************************************************************");
                System.out.println("Node ID: " + nodeID);
		System.out.println("------------------------------------------");
                String[] nodeInfo = peers.get(nodeID).split(";");

                System.out.println("IP address: " + nodeInfo[0] );
		System.out.println("Coordinates: (" + nodeInfo[2] + "," + nodeInfo[3] + ")");

                Registry aRegistry = LocateRegistry.getRegistry(nodeInfo[0], Integer.parseInt(nodeInfo[1]));
                RemoteInterface aNode = (RemoteInterface) aRegistry.lookup(nodeID);

                Hashtable<String,String> neighbors = aNode.getNeighbors();
		ArrayList<String> nodeNeighbors = Collections.list(neighbors.keys());
                System.out.println("Neighbors: " + nodeNeighbors);

                HashSet<String> items = aNode.getItems();
                Object[] nodeItems = items.toArray();
                if(nodeItems.length > 0) {
                    System.out.println("Items at this node: " );
                    for(Object aObject : nodeItems) {
                        System.out.println(aObject.toString());
                    }
                } else {
                    System.out.println("No items at this node");
                }
		System.out.println("###################################################################");
            }
        }
    }

    // Implementation in Peer only
    @Override
    public Hashtable<String, String> getNeighbors() throws RemoteException {
        return null;
    }

    // Implementation in Peer only
    @Override
    public HashSet<String> getItems() throws RemoteException {
        return null;
    }

    // The main function
    // This function creates an instace of the bootstrap server and starts listening at the specified port
    public static void main(String[] args) throws AlreadyBoundException, IOException, NotBoundException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("This is the Bootstrap server!");
        System.out.print("Enter the port number: ");
        int port = Integer.parseInt(br.readLine());
	System.out.println("Port number: " + port);
        BootstrapServer server = new BootstrapServer(port);

        System.out.println("The server is up and running at " + InetAddress.getLocalHost().getHostAddress() +
            " and listening at " + port);

        while(true) {
            String input = br.readLine();
            String[] inValues = input.split(" ");
            if(inValues.length > 0) {
                if(inValues[0].equals("view")) {
                    if(inValues.length > 1) {
                        server.view(inValues[1]);
                    } else {
                        server.view(null);
                    }
                } else {
                    System.out.println("Use only 'view' keyword");
                }
            }
        }
    }
} // End of BootstrapServer
