Project Iteration 4:
####################

Authors:
========
	Nic McCallum
	Niko Robidoux
	Abbas Zahed
	Kevin Miller

Description:
============
	The program is expected to communicate between a client, or multiple clients, and a server. The communication can be
	either a read or write request of a file.


Import the project into eclipse, this can be done by:
=====================================================
	1) Create a new project by selecting: File -> New Project
	2) When the New Project Wizard appears uncheck the "Use default location" checkbox
	3) Click browse and select the project folder
	4) Click finish and the project is now imported into eclipse.

Steps for running on different PCs:
===================================
    Note: The error sim can reside on any computer, but most testing was done with the error sim on the same
          pc as the client, or on the same pc as the server.

    1) Run Server.java on one of the PCs. Note the address that the program displays at startup as the broadcast address.
       This will be the address you enter into the client and error sim.

       Note: You can also find the address by opening cmd and entering "ipconfig /all" and finding the Ethernet IPv4 address.

    2) Run ErrorSim.java. Enter the address from the server above when it prompts for the server address. Note the address the
       ErrorSim prints, this will be entered in the client.

    3) Run Client.java. Enter the server address when it prompts for it.
       Note: If the address of the server has changed, or connecting to a different address you can enter the command:
                connect IP_ADDRESS
             to connect to the new server address.
             
    4) Select the mode you want to run in. The default mode the client runs in is normal mode (client directly to server).
       If this is the mode you want, you can run commands right away. If you want to go to test mode (client to error sim)
       then enter the command :
            mode test IP-Address-Of-ErrorSim

       If you want to go back to normal mode enter the command:
            mode normal

       Both should display the updated mode the client is running in.

    5) Use steps 5-8 below (Steps for running) to run the commands.

Steps for running:
==================
    1) Run ErrorSim.java
    2) Run Server.java
    3) Run Client.java
    4) Follow on screen format and menus in the client to send requests to the server
	5) By default the output is not shown. To show the output from the Server or Client you can type the command:
			-> "verbose true"
    6) A write request can be performed using the command -> write C:/file/location/filename_on_server.txt C:/file/location/on/client.txt octet|netascii
    7) A read equest can be performed using the command -> read C:/file/location/filename_on_server.txt C:/file/location/on/client.txt octet|netascii
	8) The ErrorSim will prompt you for an error to simulate. To select one of the options you select the code from the menu and enter the block number
	   that the error should be performed on afterwards. If you want the request to complete without error you can use: "00 0" at the prompt.
			-> example: "08 7" (This will lose packet 7)
	
	NOTE: The sever and client can save anywhere on the file system as long as the path is correct.

Testing:
========
	Iteration 4:
	------------
		NOTE: Packets received/sent are not shown by default. To see these enter the following command into the server and client: verbose true 
		
		1) Reading a file that does not exist on the server to the client. This was done using the following steps:
			a) Enter Command into Client: read filename_that_doesnt_exist.txt save_file.txt octet -> This sends read request to server with non existent file name.
			b) Enter Command into ErrorSim: 00 0 -> This sets operation mode to normal.
			
		2) Writing a file that does not exist on the client to the server. This was done using the following steps:
			a) Enter Command into Client: write server_save.txt filename_that_doesnt_exist.txt octet -> This sends write request to server with non existent client file.
			b) Enter Command into ErrorSim: 00 0 -> This sets operation mode to normal.
		
		3) Reading a file that already exists on the client. This was done using the following steps:
			a) Create initial file to try and overwrite. Note the save location and use it for the next step.
			b) Enter Command into Client: read server_file.txt file_that_already_exists.txt octet -> This attempts to read server file to client file that already exists.

		4) Writing a file that already exists on the client. This was done using the following steps:
			a) Write intial file to server using the command in the client: write server_file.txt client_file.txt octet
			b) Enter Command into Client: write server_file.txt overwrite.txt octet -> This attempts to overwrite the file in the server.
			c) Enter Command into ErrorSim: 00 0 -> This sets operation mode to normal.
		
		5) Reading a file to a disk that is full. This was done using the following steps:
			a) Using a USB drive, fill the drive so there is not enough space left.
			b) Enter Command into Client: read server_file.txt E:\usb.txt octet -> This attempts to read the server_file.txt to the USB drive file where the USB drive is "E:\".
			c) Enter Command into ErrorSim: 00 0 -> This sets operation mode to normal.
			
		6) Writing a file to a disk that is full. This was done using the following steps:
			a) Using a USB drive, fill the drive so there is not enough space left.
			b) Enter Command into Client: write E:\usb.txt client_file.txt octet -> This attempts to write the client_file.txt to the USB drive file where the USB drive is "E:\".
			c) Enter Command into ErrorSim: 00 0 -> This sets operation mode to normal.
			
		7) Reading a protected file that does not have read priviliges on the server to the client. This was done using the following steps:
			a) Create a text file and set the properties by right clicking the file -> Security Tab -> Edit -> Check all checkboxes for deny.
			b) Enter Command into Client: read server_protected.txt client_file.txt octet -> This attempts to read the server_protected.txt to the client at client_file.txt
			c) Enter Command into ErrorSim: 00 0 -> This sets operation mode to normal.
			
		8) Writing a protected server on the client side to the server. This was done using the following steps:
			a) Create a text file and set the properties by right clicking the file -> Security Tab -> Edit -> Check all checkboxes for deny.
			b) Enter Command into Client: write server_file.txt client_protected.txt octet -> This attempts to write the client_protected.txt to the server at server_file.txt
			c) Enter Command into ErrorSim: 00 0 -> This sets operation mode to normal.
			
	Iteration 3:
	------------
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

Test Cases:
===========
	Iteration 4:
	------------
		1) Reading a file that does not exist on the server to the client.
		2) Writing a file that does not exist on the client to the server.
		3) Reading a file that already exists on the client.
		4) Writing a file that already exists on the client.
		5) Reading a file to a disk that is full. This was done using a USB stick that was full and had no space left.
		6) Writing a file to a disk that is full. Used the same method above.
		7) Reading a protected file that does not have read priviliges on the server to the client.
		8) Writing a protected server on the client side to the server.
	
	Iteration 3:
	------------
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
==========
	Niko: Coding, Testing
	Abbas: Coding, Timing Diagrams
	Nic: Testing, Coding
	Kevin: Timing Diagrams, Testing

    	