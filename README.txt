***************************************
README for CAN implementation
-------------------------------
Version : 1.0
Author  : Shreyas Jayanna
Date	: 03/17/14
__________________________________________________________________________

Files included:
---------------------------
1. RemoteInterface.java
2. BootstrapServer.java
3. Peer.java
__________________________________________________________________________

Compilation instructions:
---------------------------
Place all the above mentioned files in the same directory and compile each.
___________________________________________________________________________

Execution instructions:
---------------------------
Execute in the following order:
1. Execute BootstrapServer
	a. Make sure to enter the port number when asked for
	***The server IP and Port number will be displayed on the prompt 
	once the server is up and running

2. Execute Peer for how many ever number of Peers
	a. Make sure to enter the port number when asked for
	b. Make sure to enter the BootstrapServer IP and Port number.
	c. Make sure to enter 'join' keyword for the peer to join the n/w.
___________________________________________________________________________

Please consider the following while executing:
--------------------------------------------------
1. Leave operation is not implemented. Hence, make sure not to crash or kill
   any node which has already joined the network.

2. Operations implemented are:
	a. join (only peer)
	b. view (from both server and peer)
	c. insert (only peer)
	d. insert (only peer)

	*** Only view operation is implemented in both server and peer to 
	    lookup peer information.

	*** In peer while using view operation, make sure to enter that peerID
	    only or enter only the view keyword.

	*** Operations provided in Peer only are done so becasue those operations
	    seemed to be appropriate for Peer only.
________________________________________________________________________________

############################### END OF README ##################################

