Project Iteration 4:

Authors:
	Nic McCallum
	Niko Robidoux
	Abbas Zahed
	Kevin Miller

Description:
	The program is expected to communicate between a client, or multiple clients, and a server. The communication can be
	either a read or write request of a file.

Steps for running:
    1) Run ErrorSim.java
    2) Run Server.java
    3) Run Client.java
    4) Follow on screen format
	The format is:
        read [filename on server] [file location] [mode] - Reads the file from the server under filename and saves
                                                 the file under the path specified under the root where the
                                                 project is being run under directory "client_files".
        write [filename on server] [file location] [mode] - Writes file at location to filename on server.
        verbose [true|false] - Changes server display mode to verbose or silent mode.
        help - Prints options screen.
        quit - Quits the client program.
        
        NOTE: The file location is saved under client_files for the client and root for the server.
              Client files can have sub directories by entering the directory under the root you want, ie:
          		
          		read wrt.txt clientx\readfiles\r.txt octet
          		
          This will save the file under the client_files\clientx\readfiles directory.
    
    5) write a request to server(FOR EXAMPLE: write out.txt C:/512.txt octet)
    6) select error options 00-09
    7) read a request to server (FOR EXAMPLE: read out.txt client1/read.txt octet)
    8) select error options 00-09


Testing:
    The testing was done was by writing a file to the server and reading a file from the server by using the 512.txt.
    1) In the client console type write written.txt C:\512.txt octet - This will write the file located at C:\512.txt to the server as written.txt
                                            under the project root directory.
                                            
    2) Selecting TEST mode on the ErrorSim by entering 00-09 at the prompt. This will complete the request without editing 
       any packets being sent for this request.
    
    3) In the client console type read written.txt read.txt octet - This will read the file written.txt from the server and save it to the location
                                         client_files under the run directory with the name given.
                                         
    4) Selecting TEST mode on the ErrorSim by entering 00-09 at the prompt. This will complete the request without editing 
       any packets being sent for this request.
                                         
    5) quit - Quit the client program
    
    6) quit - On the Server UI to close all new requests and wait for current ones to finish
    
    NOTE: The server/client output can seen by turning verbose mode on -> this is done by using the command
          "verbose true" on either the server or client.
          
    The testing was also done on the ErrorSim by editing portions of the packets that are being sent
    in between the client and server. One of the errors that was tested was...
    
    

    Test Cases:
    	1) Client sends read request to ErrorSim -> ErrorSim edits first opcode byte -> Server responds to invalid data request
    	   and closes the request -> Server sends back to ErrorSim and Client closes.
    	   
    	2) Client sends read request to ErrorSim -> ErrorSim edits second opcode byte -> Server responds to invalid data request
    	   and closes the request -> Server sends back to ErrorSim and Client closes.
    	   
    	3) Client sends read request -> ErrorSim edits mode (ie not octet/netascii/wait) -> Server responds to invalid data request 
    	   and closes the request -> Client closes.
    	   
    	4) Client sends read request -> ErrorSim Address/port -> Server responds to invalid address and closes the request -> Client closes.
    	
    	5) Client sends read request -> Server Responds -> Client Responds -> Server Responds -> ErrorSim edits the Port/Address ->
    	   client closes and sends error packet to server -> Server closes request thread.
    	
    	6) Client sends read request -> Server Responds -> ErrorSim edits the OP code -> client closes and sends error packet to server -> 
    	   Server closes request thread. 
    Breakdown:
    	Niko: Coding, Testing
        Abbas: Coding, Timing Diagrams
        Nic: Testing, Coding
        Kevin: Timing Diagrams, Testing
    	