/*
 * Peer.java
 * v1.0
 * Author: Shreyas Jayanna
 * Date: 03/17/14
 */

// Import statements
import javax.swing.plaf.synth.SynthEditorPaneUI;
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
 * Class Peer
 * This class implements the functionalities of a Peer. This class implements the
 * RemoteInterface and extends UnicastRemoteObject
 */

public class Peer extends UnicastRemoteObject implements RemoteInterface{

    float[] coord;
    float[] zoneBoundary;
    String nodeID;
    String nodeIP;
    int port;
    Hashtable<String, String> neighbors; // Key - nodeID ::: Value - IP;Port;x;y
    HashSet<String> items;

    Registry registry;
    RemoteInterface bootstrap;
    RemoteInterface otherPeer;


    // Constructor
    Peer(String id, String ip, int port, String serverIP, int serverPort) throws RemoteException, AlreadyBoundException, NotBoundException {
        coord = new float[2];
        zoneBoundary = new float[8];

        nodeID = id;
        nodeIP = ip;
        this.port = port;
	this.neighbors = new Hashtable();
	this.items = new HashSet();

        registry = LocateRegistry.createRegistry(port);
        registry.bind(this.nodeID, this);

        Registry serverRegistry = LocateRegistry.getRegistry(serverIP, serverPort);
        bootstrap = (RemoteInterface)serverRegistry.lookup("bootstrap");
    }

    // To join a network
    public void joinNetwork() throws RemoteException, NotBoundException {
        float x;
        float y;

        Random random = new Random();
        x = random.nextInt(10);
        y = random.nextInt(10);

        coord[0] = x;
        coord[1] = y;

        System.out.println("My coordinates are: (" + x + "," + y + ")");

        String[] routeNodeInfo = bootstrap.join(this.nodeID, this.nodeIP, this.port,  x, y);

        if(routeNodeInfo[0].equals("NF")) {
            while(routeNodeInfo[4].equals("No")) {
                Registry otherPeerReg = LocateRegistry.getRegistry(routeNodeInfo[2], Integer.parseInt(routeNodeInfo[3]));
                this.otherPeer = (RemoteInterface)otherPeerReg.lookup(routeNodeInfo[1]);

		System.out.println("Routed through: " + routeNodeInfo[2]);

		routeNodeInfo = this.otherPeer.route(this.nodeID, this.nodeIP, this.port, x, y);
            }

            if(routeNodeInfo[0].equals("NF")) {
                if(routeNodeInfo[4].equals("Yes")) {
                    for(int i = 0; i < 8; i++) {
                        this.zoneBoundary[i] = Float.valueOf(routeNodeInfo[i+5]);
                    }
                }
            } else {
                System.out.println("Failure");
            }
        } else {
	    System.out.println("I'm the first node!");
            this.zoneBoundary[0] = 0;
            this.zoneBoundary[1] = 10;
            this.zoneBoundary[2] = 10;
            this.zoneBoundary[3] = 10;
            this.zoneBoundary[4] = 0;
            this.zoneBoundary[5] = 0;
            this.zoneBoundary[6] = 10;
            this.zoneBoundary[7] = 0;
        }

	System.out.println("I have joined the network");

    }

    // To update neighbor table when a new zone is formed
    @Override
    public void updateNeighbors(Hashtable<String,String> neighbors) throws RemoteException {
        this.neighbors = neighbors;
    }

    // Called from main when insert keyword is entered in command prompt
    public void insertFile(String filename) throws RemoteException, NotBoundException {
        float[] coord = hashCode(filename);
        if(coord[0] < this.zoneBoundary[2] && coord[0] > this.zoneBoundary[0] &&
                coord[1] < this.zoneBoundary[1] && coord[1] > this.zoneBoundary[5]) {
            // If x,y lies in this zone
            if(this.items.add(filename)) {
                System.out.println("The file is stored at: " + this.nodeIP);
            } else {
                System.out.println("Failure");
                System.out.println("The file could not be stored");
            }
        } else {
            List<String> insertResult = new ArrayList<String>();

            if(neighbors.size() > 0) {
                // If point doesn't lie in own zone and at least one neighbor present
                List<String> neighborNodes = new ArrayList<String>(neighbors.keySet());
                List<String> nNValues = new ArrayList<String>();
                for(String node : neighborNodes) {
                    nNValues.add(neighbors.get(node));
                }

                int index = 0;
                double min = 999;

                String[] nodeValues = new String[4];
                for(int i = 0; i < nNValues.size(); i++) {
                    String nodeInfo = nNValues.get(i);
                    String[] nodeDetails = nodeInfo.split(";");
                    float xCoord = Float.parseFloat(nodeDetails[2]);
                    float yCoord = Float.parseFloat(nodeDetails[3]);
                    double distance = Math.sqrt(((coord[0] - xCoord) * (coord[0] - xCoord)) +
                            ((coord[1] - yCoord) * (coord[1] - yCoord)));
                    if(distance < min) {
                        index = i;
                        min = distance;
                        nodeValues = nodeDetails;
                    }
                }
                Registry registry1 = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
                this.otherPeer = (RemoteInterface) registry1.lookup(neighborNodes.get(index));
                insertResult.addAll(otherPeer.insert(filename, coord[0], coord[1]));

                System.out.println("Path followed: ");
                for(String IP : insertResult) {
                    System.out.println(IP);
                }
                if(insertResult.get(insertResult.size()-1).equals("Failure")) {
                    System.out.println("Failure");
                } else {
                    System.out.println("The file is stored at: " + insertResult.get(insertResult.size()-1));
                }
            }

        }
    }

    // Called from a remote Peer for routing while inserting the file
    @Override
    public List<String> insert(String filename, float x, float y) throws RemoteException, NotBoundException {
        List<String> path = new ArrayList<String>();

        if(((coord[0] < zoneBoundary[0]) && (coord[0] > zoneBoundary[2]))
                && ((coord[1] < zoneBoundary[5]) && (coord[1] > zoneBoundary[1]))) {
            // If point doesn't lie in own zone and at least one neighbor present
            List<String> neighborNodes = new ArrayList<String>(neighbors.keySet());
            List<String> nNValues = new ArrayList<String>();
            for(String node : neighborNodes) {
                nNValues.add(neighbors.get(node));
            }

            int index = 0;
            double min = 999;

            String[] nodeValues = new String[4];
            for(int i = 0; i < nNValues.size(); i++) {
                String nodeInfo = nNValues.get(i);
                String[] nodeDetails = nodeInfo.split(";");
                float xCoord = Float.parseFloat(nodeDetails[2]);
                float yCoord = Float.parseFloat(nodeDetails[3]);
                double distance = Math.sqrt(((coord[0] - xCoord) * (coord[0] - xCoord)) +
                        ((coord[1] - yCoord) * (coord[1] - yCoord)));
                if(distance < min) {
                    index = i;
                    min = distance;
                    nodeValues = nodeDetails;
                }
            }
            Registry registry1 = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
            this.otherPeer = (RemoteInterface) registry1.lookup(neighborNodes.get(index));
            path.addAll(otherPeer.insert(filename, coord[0], coord[1]));
        } else {
            if(this.items.add(filename)) {
                path.add(this.nodeIP);
            } else {
                path.add("Failure");
            }
        }
        return path;
    }

    // Called from main when insert keyword is entered in command prompt
    public void searchFile(String filename) throws RemoteException, NotBoundException {
        float[] coord = hashCode(filename);
        if(coord[0] <= this.zoneBoundary[2] && coord[0] >= this.zoneBoundary[0] &&
                coord[1] <= this.zoneBoundary[1] && coord[1] >= this.zoneBoundary[5]) {
            if(this.items.contains(filename)) {
                System.out.println(this.nodeIP);
            } else {
                System.out.println("Failure");
                System.out.println("The file was not found");
            }
        } else {
            List<String> insertResult = new ArrayList<String>();

            if(neighbors.size() > 0) {
                // If point doesn't lie in own zone and at least one neighbor present
                List<String> neighborNodes = new ArrayList<String>(neighbors.keySet());
                List<String> nNValues = new ArrayList<String>();
                for(String node : neighborNodes) {
                    nNValues.add(neighbors.get(node));
                }

                int index = 0;
                double min = 999;

                String[] nodeValues = new String[4];
                for(int i = 0; i < nNValues.size(); i++) {
                    String nodeInfo = nNValues.get(i);
                    String[] nodeDetails = nodeInfo.split(";");
                    float xCoord = Float.parseFloat(nodeDetails[2]);
                    float yCoord = Float.parseFloat(nodeDetails[3]);
                    double distance = Math.sqrt(((coord[0] - xCoord) * (coord[0] - xCoord)) +
                            ((coord[1] - yCoord) * (coord[1] - yCoord)));
                    if(distance < min) {
                        index = i;
                        min = distance;
                        nodeValues = nodeDetails;
                    }
                }

                Registry registry1 = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
                this.otherPeer = (RemoteInterface) registry1.lookup(neighborNodes.get(index));
                insertResult.addAll(otherPeer.search(filename, coord[0], coord[1]));

                System.out.println("Path followed: ");
                for(String IP : insertResult) {
                    System.out.println(IP);
                }
                if(insertResult.get(insertResult.size()-1).equals("Failure")) {
                    System.out.println("The file was not found!");
                }
            }

        }
    }

    // To search for a file in the network
    @Override
    public List<String> search(String filename, float x, float y) throws RemoteException, NotBoundException {
        List<String> path = new ArrayList<String>();

        if(((coord[0] < zoneBoundary[0]) && (coord[0] > zoneBoundary[2]))
                && ((coord[1] < zoneBoundary[5]) && (coord[1] > zoneBoundary[1]))) {
            // If point doesn't lie in own zone and at least one neighbor present
            List<String> neighborNodes = new ArrayList<String>(neighbors.keySet());
            List<String> nNValues = new ArrayList<String>();
            for(String node : neighborNodes) {
                nNValues.add(neighbors.get(node));
            }

            int index = 0;
            double min = 999;

            String[] nodeValues = new String[4];
            for(int i = 0; i < nNValues.size(); i++) {
                String nodeInfo = nNValues.get(i);
                String[] nodeDetails = nodeInfo.split(";");
                float xCoord = Float.parseFloat(nodeDetails[2]);
                float yCoord = Float.parseFloat(nodeDetails[3]);
                double distance = Math.sqrt(((coord[0] - xCoord) * (coord[0] - xCoord)) +
                        ((coord[1] - yCoord) * (coord[1] - yCoord)));
                if(distance < min) {
                    index = i;
                    min = distance;
                    nodeValues = nodeDetails;
                }
            }
            Registry registry1 = LocateRegistry.getRegistry(nodeValues[0], Integer.parseInt(nodeValues[1]));
            this.otherPeer = (RemoteInterface) registry1.lookup(neighborNodes.get(index));
            path.addAll(otherPeer.search(filename, coord[0], coord[1]));
        } else {
            if(this.items.contains(filename)) {
                path.add(this.nodeIP);
            } else {
                path.add("Failure");
            }
        }

        return path;
    }

    // Returns boundary of the zone
    @Override
    public float[] getBoundary() throws RemoteException {
        return this.zoneBoundary;
    }

    // Removes 'me' from neighbor table and adds 'newNode'
    @Override
    public void replaceMeNeighbor(String me, String newNode) throws RemoteException {
        this.neighbors.remove(me);
        String[] nodeInfo = newNode.split(";");
        String nodeValues = newNode.substring(nodeInfo[0].length() + 1);
        this.neighbors.put(nodeInfo[0],nodeValues);
    }

    // Adds 'newNode' in the neighbor table
    @Override
    public void addNewNeighbor(String newNode) throws RemoteException {
        String[] nodeInfo = newNode.split(";");
        String nodeValues = newNode.substring(nodeInfo[0].length() + 1);
        this.neighbors.put(nodeInfo[0],nodeValues);
    }


    // View - Displays information about this node
    @Override
    public void view(String nodeID) throws RemoteException, NotBoundException {
        System.out.println("Node ID: " + nodeID);
        System.out.println("IP address: " + nodeIP);
        System.out.println("Coordinates: (" + coord[0] + "," + coord[1] + ")");

	if(this.neighbors.size() > 0) {
	    ArrayList<String> nodeNeighbors = Collections.list(this.neighbors.keys());
	    System.out.println("Neighbors: " + nodeNeighbors);
	} else {
	    System.out.println("No neighbors");
	}

	if(this.items.size() > 0) {
	    Object[] nodeItems = this.items.toArray();
	    if(nodeItems.length > 0) {
		System.out.println("Items at this node: " );
		for(Object aObject : nodeItems) {
		    System.out.println(aObject.toString());
		}
	    } else {
		System.out.println("No items at this node");
	    }
	} else {
	    System.out.println("No items");
	}
    }

    // Returns neighbors table when called from the bootstrap server
    @Override
    public Hashtable<String, String> getNeighbors() throws RemoteException {
        return neighbors;
    }

    // Returns the Items table when called from the bootstrap server
    @Override
    public HashSet<String> getItems() throws RemoteException {
        return items;
    }

    // Returns the hashcode for a filename
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

    // Implementation only in the bootstrap server
    @Override
    public String[] join(String nodeID, String nodeIP, int port, float x, float y) {
        return new String[0];
    }

    // Routes a node to another node when joining the network.
    // If the new node coordinates lie in the same zone, splits the zone
    @Override
    public String[] route(String nodeID, String nodeIP, int port, float x, float y) 
	throws RemoteException, NotBoundException {
        String[] routeNodeInfo = new String[13];

        if((neighbors.size() > 0) && (((x < zoneBoundary[0]) && (x > zoneBoundary[2]))
                && ((y < zoneBoundary[5]) && (y>zoneBoundary[1])))) {
            // If point doesn't lie in own zone and at least one neighbor present
            List<String> neighborNodes = new ArrayList<String>(neighbors.keySet());
            List<String> nNValues = new ArrayList<String>(neighbors.values());

            int index = 0;
            double min = 999;

            String[] nodeValues = new String[4];
            for(int i = 0; i < nNValues.size(); i++) {
                String nodeInfo = nNValues.get(i);
                String[] nodeDetails = nodeInfo.split(";");
                float xCoord = Float.parseFloat(nodeDetails[2]);
                float yCoord = Float.parseFloat(nodeDetails[3]);
                double distance = Math.sqrt(((x - xCoord) * (x - xCoord)) + ((y - yCoord) * (y - yCoord)));
                if(distance < min) {
                    index = i;
                    min = distance;
                    nodeValues = nodeDetails;
                }
            }

            // Fill up routeInfo matrix
            routeNodeInfo[0] = "NF";
            routeNodeInfo[1] = neighborNodes.get(index);
            routeNodeInfo[2] = nodeValues[0];   // IP
            routeNodeInfo[3] = nodeValues[1];   // Port
            routeNodeInfo[4] = "No";
            // No need to fill rest of the array if index 4 is No

        } else {
            // If point lies in own zone 
            routeNodeInfo[0] = "NF";
            // No need to fill indexes 1,2 and 3 when index 4 is Yes
            routeNodeInfo[4] = "Yes";

            List<String> neighborNodes = new ArrayList<String>(neighbors.keySet());
            List<String> nNValues = new ArrayList<String>(neighbors.values());

            Hashtable newNodeNeigh = new Hashtable();

            double horizontal = Math.sqrt(((zoneBoundary[0] - zoneBoundary[2])*(zoneBoundary[0] - zoneBoundary[2]))
                    + ((zoneBoundary[1] - zoneBoundary[3])*(zoneBoundary[1] - zoneBoundary[3])));
            double vertical = Math.sqrt(((zoneBoundary[0] - zoneBoundary[4])*(zoneBoundary[0] - zoneBoundary[4]))
                    + ((zoneBoundary[1] - zoneBoundary[5])*(zoneBoundary[1] - zoneBoundary[5])));
            if(horizontal < vertical) {
                // When the zone to be split is a rectangle
                if(y > ((zoneBoundary[5] + vertical) / 2)) {
                    // If new point lies in the top part
                    routeNodeInfo[5] = String.valueOf(this.zoneBoundary[0]); //top left x
                    routeNodeInfo[6] = String.valueOf(this.zoneBoundary[1]); //top left y
                    routeNodeInfo[7] = String.valueOf(this.zoneBoundary[2]); //top right x
                    routeNodeInfo[8] = String.valueOf(this.zoneBoundary[3]); //top right y
                    routeNodeInfo[9] = String.valueOf(this.zoneBoundary[4]); //bottom left x
                    routeNodeInfo[10] = String.valueOf((float)(this.zoneBoundary[5] + vertical ) / 2); //bottom left y
                    routeNodeInfo[11] = String.valueOf(this.zoneBoundary[6]); //bottom right x
                    routeNodeInfo[12] = String.valueOf((float)(this.zoneBoundary[7] + vertical) / 2); //bottom right y

                    int index = 0;
                    for(String aNeighbor : neighborNodes) {
                        String[] neighborInfo = nNValues.get(index).split(";");
                        Registry aRegistry = LocateRegistry.getRegistry(neighborInfo[0], Integer.parseInt(neighborInfo[1]));
                        RemoteInterface neigh = (RemoteInterface)aRegistry.lookup(aNeighbor);
                        float[] neighBoundary = neigh.getBoundary();

                        // Bottom boundary of neighbor node overlaps with top boundary of new node
                        float m1 = (neighBoundary[5] - neighBoundary[7]) / (neighBoundary[4] - neighBoundary[6]);
                        if((this.zoneBoundary[1] - neighBoundary[5]) == (m1 * (this.zoneBoundary[0] - neighBoundary[4]))) {
                            if((this.zoneBoundary[3] - neighBoundary[7]) == (m1 * (this.zoneBoundary[2] - neighBoundary[6]))) {
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        }

                        // Right boundary of neighbor node and left boundary of new node
                        // m = (y1-y2)/(x1-x2)
                        float m2 = (neighBoundary[3] - neighBoundary[7]) / (neighBoundary[2] - neighBoundary[6]);
                        if((this.zoneBoundary[1] - neighBoundary[3]) == (m2 * (this.zoneBoundary[0] - neighBoundary[2]))) {
                            if((this.zoneBoundary[5] - neighBoundary[7]) == (m2 * (this.zoneBoundary[4] - neighBoundary[6]))) {
                                // The old neighbor is same for both old and new node
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            } else if(((this.zoneBoundary[5] + vertical / 2) - neighBoundary[7]) == (m2 * (this.zoneBoundary[4] - neighBoundary[6]))) {
                                //only new zone is neighbor
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        }

                        // Left boundary of neighbor node and right boundary of new node
                        float m3 = (neighBoundary[1] - neighBoundary[5]) / (neighBoundary[0] - neighBoundary[4]);
                        if((this.zoneBoundary[3] - neighBoundary[1]) == (m3 * (this.zoneBoundary[2] - neighBoundary[0]))) {
                            if((this.zoneBoundary[7] - neighBoundary[5]) == (m3 * (this.zoneBoundary[6] - neighBoundary[4]))) {
                                // The old neighbor is same for both old and new node
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            } else if(((this.zoneBoundary[7] + vertical / 2) - neighBoundary[5]) == (m2 * (this.zoneBoundary[6] - neighBoundary[4]))) {
                                //only new zone is neighbor
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        }

                    }

                    this.zoneBoundary[1] = (float) ((this.zoneBoundary[5] + vertical) / 2);
                    this.zoneBoundary[3] = (float) ((this.zoneBoundary[7] + vertical) / 2);

                } else {
                    // When the point lies in the bottom part
                    routeNodeInfo[5] = String.valueOf(this.zoneBoundary[0]); //top left x
                    routeNodeInfo[6] = String.valueOf((this.zoneBoundary[5] + vertical) / 2); //top left y
                    routeNodeInfo[7] = String.valueOf(this.zoneBoundary[2]); //top right x
                    routeNodeInfo[8] = String.valueOf((this.zoneBoundary[7] + vertical) / 2); //top right y
                    routeNodeInfo[9] = String.valueOf(this.zoneBoundary[4]); //bottom left x
                    routeNodeInfo[10] = String.valueOf(this.zoneBoundary[5]); //bottom left y
                    routeNodeInfo[11] = String.valueOf(this.zoneBoundary[6]); //bottom right x
                    routeNodeInfo[12] = String.valueOf(this.zoneBoundary[7]); //bottom right y

                    // Update neighbors
                    int index = 0;
                    for(String aNeighbor : neighborNodes) {
                        String[] neighborInfo = nNValues.get(index).split(";");
                        Registry aRegistry = LocateRegistry.getRegistry(neighborInfo[0], Integer.parseInt(neighborInfo[1]));
                        RemoteInterface neigh = (RemoteInterface)aRegistry.lookup(aNeighbor);
                        float[] neighBoundary = neigh.getBoundary();

                        // Top boundary of neighbor node overlaps with bottom boundary of new node
                        float m1 = (neighBoundary[1] - neighBoundary[3]) / (neighBoundary[0] - neighBoundary[2]);
                        if((this.zoneBoundary[5] - neighBoundary[1]) == (m1 * (this.zoneBoundary[4] - neighBoundary[0]))) {
                            if((this.zoneBoundary[7] - neighBoundary[1]) == (m1 * (this.zoneBoundary[6] - neighBoundary[0]))) {
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        }

                        // Right boundary of neighbor node and left boundary of new node
                        // m = (y1-y2)/(x1-x2)
                        float m2 = (neighBoundary[3] - neighBoundary[7]) / (neighBoundary[2] - neighBoundary[6]);
                        if((this.zoneBoundary[1] - neighBoundary[3]) == (m2 * (this.zoneBoundary[0] - neighBoundary[2]))) {
                            if((this.zoneBoundary[5] - neighBoundary[7]) == (m2 * (this.zoneBoundary[4] - neighBoundary[6]))) {
                                // The old neighbor is same for both old and new node
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            } else if(((this.zoneBoundary[5] + vertical / 2) - neighBoundary[7]) == (m2 * (this.zoneBoundary[4] - neighBoundary[6]))) {
                                //only new zone is neighbor
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        }

                        // Left boundary of neighbor node and right boundary of new node
                        float m3 = (neighBoundary[1] - neighBoundary[5]) / (neighBoundary[0] - neighBoundary[4]);
                        if((this.zoneBoundary[3] - neighBoundary[1]) == (m3 * (this.zoneBoundary[2] - neighBoundary[0]))) {
                            if((this.zoneBoundary[7] - neighBoundary[5]) == (m3 * (this.zoneBoundary[6] - neighBoundary[4]))) {
                                // The old neighbor is same for both old and new node
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            } else if(((this.zoneBoundary[7] + vertical / 2) - neighBoundary[5]) == (m2 * (this.zoneBoundary[6] - neighBoundary[4]))) {
                                //only new zone is neighbor
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        }

                    }

                    this.zoneBoundary[5] = (float)(this.zoneBoundary[5] + vertical) / 2;
                    this.zoneBoundary[7] = (float)(this.zoneBoundary[7] + vertical) / 2;
                }
            } else {
                //split vertically
                if(x > ((zoneBoundary[4] + horizontal) / 2)) {
                    routeNodeInfo[5] = String.valueOf((float)(this.zoneBoundary[0] + horizontal) / 2); //top left x
                    routeNodeInfo[6] = String.valueOf(this.zoneBoundary[1]); //top left y
                    routeNodeInfo[7] = String.valueOf(this.zoneBoundary[2]); //top right x
                    routeNodeInfo[8] = String.valueOf(this.zoneBoundary[3]); //top right y
                    routeNodeInfo[9] = String.valueOf((float)(this.zoneBoundary[4] + horizontal) / 2); //bottom left x
                    routeNodeInfo[10] = String.valueOf(this.zoneBoundary[5]); //bottom left y
                    routeNodeInfo[11] = String.valueOf(this.zoneBoundary[6]); //bottom right x
                    routeNodeInfo[12] = String.valueOf(this.zoneBoundary[7]); //bottom right y

                    // Update neighbors
                    int index = 0;
                    for(String aNeighbor : neighborNodes) {
                        String[] neighborInfo = nNValues.get(index).split(";");
                        Registry aRegistry = LocateRegistry.getRegistry(neighborInfo[0], Integer.parseInt(neighborInfo[1]));
                        RemoteInterface neigh = (RemoteInterface)aRegistry.lookup(aNeighbor);
                        float[] neighBoundary = neigh.getBoundary();

                        // Bottom boundary of neighbor overlaps with top boundary of node
                        float m1 = (neighBoundary[5] - neighBoundary[7]) / (neighBoundary[4] - neighBoundary[6]);
                        if((this.zoneBoundary[1] - neighBoundary[5]) == (m1 * (this.zoneBoundary[0] - neighBoundary[4]))) {
                            if((this.zoneBoundary[3] - neighBoundary[7]) == (m1 * (this.zoneBoundary[2] - neighBoundary[6]))) {
                                // Top neighbor is common to both zones
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            } else if((this.zoneBoundary[1] - neighBoundary[5]) == (m1 * ((this.zoneBoundary[0] + horizontal / 2) - neighBoundary[4]))) {
                                // Top neighbor applies to new zone only
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        } else if((this.zoneBoundary[1] - neighBoundary[5]) == (m1 * ((this.zoneBoundary[0] + horizontal / 2) - neighBoundary[4]))) {
                            if((this.zoneBoundary[3] - neighBoundary[7]) == (m1 * (this.zoneBoundary[2] - neighBoundary[6]))) {
                                // Top neighbor applies to new zone only and starts exactly from the new zone start
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            }
                        }

                        // Left boundary of neighbor overlaps with the right boundary of the new node
                        float m2 = (neighBoundary[1] - neighBoundary[5]) / (neighBoundary[0] - neighBoundary[4]);
                        if((this.zoneBoundary[3] - neighBoundary[1]) == (m2 * (this.zoneBoundary[2] - neighBoundary[0]))) {
                            if((this.zoneBoundary[7] - neighBoundary[5]) == (m2 * (this.zoneBoundary[6] - neighBoundary[4]))) {
                             //   if((neighBoundary[1] >= this.zoneBoundary[3]) && (this.zoneBoundary[7] <= neighBoundary[5])){
                                    // Update neighbor tables
                                    newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                    this.neighbors.remove(aNeighbor);
                                    String me = this.nodeID;
                                    String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                    neigh.replaceMeNeighbor(me,newNode);
				    this.neighbors.remove(aNeighbor);
                              //  }
                            }
                        }

                        // Top boundary of neighbor overlaps with bottom boundary of new zone
                        float m3 = (neighBoundary[1] - neighBoundary[3]) / (neighBoundary[0] - neighBoundary[2]);
                        if((this.zoneBoundary[5] - neighBoundary[1]) == (m3 * (this.zoneBoundary[4] - neighBoundary[0]))) {
                            if((this.zoneBoundary[7] - neighBoundary[3]) == (m3 * (this.zoneBoundary[6] - neighBoundary[2]))) {
                                if((this.zoneBoundary[4] >= neighBoundary[0]) && (this.zoneBoundary[6] <= neighBoundary[2])) {
                                    // both zones
                                    newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                    String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                    neigh.addNewNeighbor(newNode);
                                } else if((this.zoneBoundary[4] <= neighBoundary[0]) && (neighBoundary[2] <= this.zoneBoundary[6])) {
                                    if(neighBoundary[2] <= (this.zoneBoundary[4] + horizontal / 2)) {
                                        // only old zone - No action
                                    } else {
                                        // both zones
                                        newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                        String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                        neigh.addNewNeighbor(newNode);
                                    }
                                } else if(((this.zoneBoundary[4] + horizontal) / 2) >= neighBoundary[0] ) {
                                    // New zone only
                                    newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                    this.neighbors.remove(aNeighbor);
                                    String me = this.nodeID;
                                    String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                    neigh.replaceMeNeighbor(me,newNode);
				    this.neighbors.remove(aNeighbor);
                                }
                            }

                        }

                    }
                    this.zoneBoundary[0] = (float) ((this.zoneBoundary[0] + horizontal) / 2);
                    this.zoneBoundary[4] = (float) ((this.zoneBoundary[4] + horizontal) / 2);
                } else {
                    routeNodeInfo[5] = String.valueOf(this.zoneBoundary[0]); //top left x
                    routeNodeInfo[6] = String.valueOf(this.zoneBoundary[1]); //top left y
                    routeNodeInfo[7] = String.valueOf((this.zoneBoundary[0] + horizontal) / 2); //top right x
                    routeNodeInfo[8] = String.valueOf(this.zoneBoundary[3]); //top right y
                    routeNodeInfo[9] = String.valueOf(this.zoneBoundary[4]); //bottom left x
                    routeNodeInfo[10] = String.valueOf(this.zoneBoundary[5]); //bottom left y
                    routeNodeInfo[11] = String.valueOf((this.zoneBoundary[4] + horizontal) / 2); //bottom right x
                    routeNodeInfo[12] = String.valueOf(this.zoneBoundary[7]); //bottom right y

                    // Update neighbors
                    int index = 0;
                    for(String aNeighbor : neighborNodes) {
                        String[] neighborInfo = nNValues.get(index).split(";");
                        Registry aRegistry = LocateRegistry.getRegistry(neighborInfo[0], Integer.parseInt(neighborInfo[1]));
                        RemoteInterface neigh = (RemoteInterface)aRegistry.lookup(aNeighbor);
                        float[] neighBoundary = neigh.getBoundary();

                        // Bottom boundary of neighbor overlaps with top boundary of node
                        float m1 = (neighBoundary[5] - neighBoundary[7]) / (neighBoundary[4] - neighBoundary[6]);
                        if((this.zoneBoundary[1] - neighBoundary[5]) == (m1 * (this.zoneBoundary[0] - neighBoundary[4]))) {
                            if((this.zoneBoundary[3] - neighBoundary[7]) == (m1 * (this.zoneBoundary[2] - neighBoundary[6]))) {
                                // Top neighbor is common to both zones
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            } else if((this.zoneBoundary[1] - neighBoundary[5]) == (m1 * ((this.zoneBoundary[0] + horizontal / 2) - neighBoundary[4]))) {
                                // Top neighbor applies to new zone only
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                            }
                        } else if((this.zoneBoundary[1] - neighBoundary[5]) == (m1 * ((this.zoneBoundary[0] + horizontal / 2) - neighBoundary[4]))) {
                            if((this.zoneBoundary[3] - neighBoundary[7]) == (m1 * (this.zoneBoundary[2] - neighBoundary[6]))) {
                                // Top neighbor applies to new zone only and starts exactly from the new zone start
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.addNewNeighbor(newNode);
                            }
                        }

                        // Right boundary of neighbor overlaps with the left boundary of the new node
                        float m2 = (neighBoundary[1] - neighBoundary[5]) / (neighBoundary[0] - neighBoundary[4]);
                        if((this.zoneBoundary[1] - neighBoundary[3]) == (m2 * (this.zoneBoundary[0] - neighBoundary[2]))) {
                            if((this.zoneBoundary[5] - neighBoundary[7]) == (m2 * (this.zoneBoundary[4] - neighBoundary[6]))) {
                                //   if((neighBoundary[1] >= this.zoneBoundary[3]) && (this.zoneBoundary[7] <= neighBoundary[5])){
                                // Update neighbor tables
                                newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                this.neighbors.remove(aNeighbor);
                                String me = this.nodeID;
                                String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                neigh.replaceMeNeighbor(me,newNode);
				this.neighbors.remove(aNeighbor);
                                //  }
                            }
                        }

                        // Top boundary of neighbor overlaps with bottom boundary of new zone
                        float m3 = (neighBoundary[1] - neighBoundary[3]) / (neighBoundary[0] - neighBoundary[2]);
                        if((this.zoneBoundary[5] - neighBoundary[1]) == (m3 * (this.zoneBoundary[4] - neighBoundary[0]))) {
                            if((this.zoneBoundary[7] - neighBoundary[3]) == (m3 * (this.zoneBoundary[6] - neighBoundary[2]))) {
                                if((this.zoneBoundary[4] >= neighBoundary[0]) && (this.zoneBoundary[6] <= neighBoundary[2])) {
                                    // both zones
                                    newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                    String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                    neigh.addNewNeighbor(newNode);
                                } else if((this.zoneBoundary[4] <= neighBoundary[0]) && (neighBoundary[2] <= this.zoneBoundary[6])) {
                                    if(neighBoundary[2] <= (this.zoneBoundary[4] + horizontal / 2)) {
                                        // only old zone - No action
                                    } else {
                                        // both zones
                                        newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                        String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                        neigh.addNewNeighbor(newNode);
                                    }
                                } else if(((this.zoneBoundary[4] + horizontal) / 2) >= neighBoundary[0] ) {
                                    // New zone only
                                    newNodeNeigh.put(aNeighbor,nNValues.get(index));
                                    this.neighbors.remove(aNeighbor);
                                    String me = this.nodeID;
                                    String newNode = nodeID + ";" + nodeIP + ";" + port + ";" + x + ";" + y;
                                    neigh.replaceMeNeighbor(me,newNode);
				    this.neighbors.remove(aNeighbor);
                                }
                            }

                        }

                    }

                    this.zoneBoundary[0] = (float)(this.zoneBoundary[0] + horizontal) / 2;
                    this.zoneBoundary[4] = (float)(this.zoneBoundary[4] + horizontal) / 2;
                }
            }

	    this.neighbors.put(nodeID,nodeIP+";"+port+";"+x+";"+y);
	    String me = this.nodeIP+";"+this.port+";"+this.coord[0]+";"+this.coord[1];
	    newNodeNeigh.put(this.nodeID,me);

	    HashSet<String> fileItems = new HashSet();
	    Object[] fileObjects = this.items.toArray();
	    String[] files = new String[fileObjects.length];
	    int index = 0;
	    for(Object object : fileObjects) {
		files[index] = String.valueOf(object);
		index++;
	    }
	    for(String file : files) {
		float[] fileCoord = hashCode(file);
		if(((coord[0] < zoneBoundary[0]) && (coord[0] > zoneBoundary[2]))
		   && ((coord[1] < zoneBoundary[5]) && (coord[1] > zoneBoundary[1]))) {
		    fileItems.add(file);
		    this.items.remove(file);
		}
	    }
	    
	    Registry aRegistry = LocateRegistry.getRegistry(nodeIP, port);
	    RemoteInterface newNode = (RemoteInterface) aRegistry.lookup(nodeID);
	    
	    newNode.updateNeighbors(newNodeNeigh);
	    if(fileItems.size() > 0) {
		newNode.updateItems(fileItems);
	    }
        }

	return routeNodeInfo;
    }

    // Updates the items table with the passed table
    @Override
    public void updateItems(HashSet<String> fileItems) throws RemoteException {
	this.items = fileItems;
    }

    // The main function
    // Creates an object of Peer and keeps listening on the port for keywords
    public static void main(String[] args) throws IOException, AlreadyBoundException, NotBoundException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Please enter a Node ID: ");
        String nodeID = br.readLine();

        String nodeIP = String.valueOf(InetAddress.getLocalHost().getHostAddress());

        System.out.print("Enter Port: ");
        int port = Integer.parseInt(br.readLine());

        System.out.println("I'm up and running at " + nodeIP + " and listening at " + port + " port..");

        System.out.print("Please give me server IP to connect: ");
        String serverIP = br.readLine();

        System.out.print("Please give me server port to connect: ");
        int serverPort = Integer.parseInt(br.readLine());

        Peer aPeer = new Peer(nodeID, nodeIP, port, serverIP, serverPort);

        System.out.println("Please enter 'join' when you want me to join the network");
        while(true) {
            if(br.readLine().equals("join")) {
                System.out.println("Now trying to join the network...");
                aPeer.joinNetwork();
                break;
            } else {
                System.out.println(nodeID + " hasn't yet joined the network!");
            }
        }

        while(true) {
            String inputStr = br.readLine();
            String[] input = inputStr.split(" ");
            if(input[0].equals("insert")) {
                if(input.length > 1) {
                    aPeer.insertFile(input[1]);
                } else {
                    System.out.println("Please try again with filename..");
                    System.out.println("Syntax: insert <filename>");
                }
            } else if(input[0].equals("search")) {
                if(input.length > 1) {
                    aPeer.searchFile(input[1]);
                } else {
                    System.out.println("Please try again with filename..");
                    System.out.println("Syntax: insert <filename>");
                }
            } else if(input[0].equals("view")) {
                if(input.length > 1) {
                    if(input[1].equals(nodeID)) {
                        aPeer.view(nodeID);
                    } else {
                        System.out.println("This operation is not supported.. Please try this at the server!\n" +
                                "Information of only this peer can be viewed here. Either enter this peer's nodeID\n" +
                                "after the view keyword or enter just the keyword");
                    }
                } else {
                    aPeer.view(nodeID);
                }
            } else {
                System.out.println("Wrong keyword!");
            }
        }
    }
} // End of Peer
