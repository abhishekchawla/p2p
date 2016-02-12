Simplified P2P

Client contains the client.java file. A single file is needed for all the clients, the server will assign different numbers to each client.
Server containes the Server2.java file
Config.prop is the config files

Description of Config.prop:

1. "c5=12005" means that port number of Client 5 is 12005. "server=12000" means that port number of the server is 12000.
2. "c5_rec=c3" means that "Client 5 recieves data from Client 3". Similarly, "c4_snd=c1" means that "Client 4 sends data to Client 1".
3. "clients=5" means the the number of clients in the system are 5. the server will automatically shut down after distributing chunks to these many clients.
4. "filename=NIP.mp4" is the name of the file to be distriuted. It must be kept in the same folder as server.java.
5. "chunks=812" means the the number of 100kb chunks generated from the given file. it is set after the server splits the file into chunk and is set, of course, by the server.

test