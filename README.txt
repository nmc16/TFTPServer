Project Iteration 1:
Authors:
Nic 
Niko
Abbas
Kevin

Description:
The program is expected to communicate between a client, or multiple clients, and a server. The communication can be
either a read or write request of a file.

Steps:
1) Run ErrorSim.java
2) Run Server.java
3) Run Client.java
4) Follow onscreen instructions
5) The format is:
read [filename] [mode] - Reads the file from the server under filename
write [filename] [file location] [mode] - Writes file at location to filename on server.
help - Prints options screen.
quit - Quits the client program

Testing:
The testing was done was by writing a file to the server and reading a file from the server by using the 512.txt.
1) write written.txt C:\512.txt octet
2) read written.txt
3) quit
