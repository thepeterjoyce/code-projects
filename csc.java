/**************  ABOUT  ***************
  By PETER JOYCE
  The many to many chat client was an extension on one to one chat assignment; a common 
  Client/Server chat that uses TCP sockets to pass information. The major implementation 
  challenge to accomplish multi-client chat is the setup AND teardown of TCP sockets, the 
  passing of information across socket streams, and multi threading to deal with blocking 
  statements that occur during these processes.
  
  
  My software was designed off the assigned one to one chat model, but extended to connect, and
  allow connections, to/from many other clients.  The architecture of this program is sloppy,
  but my major focus was on creating a functional chat.  I used three classes for this
  assignment, csc, serverThread, and clientThread, the latter two are static classes inside
  the first simply because I wanted to keep this as a single file.  It is designed to have a 
  server thread constantly recieving connections, and the main thread polling user input. Client
  connections each spawn their own thread for listening purposes. 
  
  My program sucessfully connects, disconnects, and chats with other clients. I discovered 
  PrintStream class does not throw execptions when a client disconnect unexpectedly, so I 
  reworked my I/O stream which now sucessfully throws exceptions after expected and unexpected
  disconnects.
*/
import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class csc {

	//GLOBAL VARS
	static int numConnection=0;
	static int defaultPort=0;
	private static ArrayList<clientThread> al = new ArrayList<clientThread>();
	static serverThread svrClass;


	// The main method handles the keyboard input which controls the program this is one thread
	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Please enter default Port");
		}
		Scanner keyboard = new Scanner(System.in);
		defaultPort = keyboard.nextInt();
		
		
		// this is my second thread, an instance of serverThread class used to construct my svr thread
		svrClass = new serverThread(defaultPort);
		Thread svr = new Thread(svrClass);
		svr.start();

		//commands to control chat program
		while(1==1){
			System.out.println("\nEnter Command:");
			String cmd = keyboard.nextLine();
			String[] parse = cmd.split("\\s+");

			parse[0]=parse[0].toLowerCase();

			if(parse[0].equals("connect")){
				connect(Integer.parseInt(parse[2]),parse[1]);
			}
			else if(parse[0].equals("send")){
				send(cmd);
			}
			else if(parse[0].equals("disconnect")){
				disconnect();
			}
			else if(parse[0].equals("exit")){
				disconnect();
				System.out.println("Have a nice day");
				System.exit(0);
			}
			else{
				System.out.println("Please input one of the following:\nConnect \'IPaddress\' \'Port\'\nSend \'Message\'\nDisconnect\nExit");
			}
		}
	}


	// This is the method to connect to other messaging clients, after connecting the connection is added to a local list of connections used to echo and forward messages between clients
	public static void connect(int port, String ip) {

		try{
			Socket sock = new Socket(ip, port);
			clientThread t = new clientThread(sock, numConnection);
			numConnection++;
			al.add(t);
			new Thread (t).start();
			System.out.println("connected with host "+ sock.getInetAddress().getHostAddress());
		}
		catch(Exception e) {
			System.out.println("Could not connect error: "+e);
		}
	}

	//This is the most used method which allows clients to send messages to each other.
	public static void send(String message){
		if(al.size()==0){
			System.out.println("Please make a connection before messaging");
		}
		else{
			try{
				clientThread t = null;
				for(int i = 0; i < al.size(); ++i) {
					t = al.get(i);
					try {
						t.out.write(message);
						t.out.flush();
					}
					catch(Exception e) {
						System.out.println("Message error "+e);
					}
				}

				if(message.charAt(0)=='d'){
					System.out.println("Disconnecting...");
				}
				else{
					System.out.println("\""+message.substring(5)+"\" sent!");
				}
			}
			catch(Exception e) {
				System.out.println("Could not send error: "+e);
			}
		}
	}

	//The echo method is used to forward messages recieved on one client to other clients connected to it. This is the method that allows for clients to chat even when not directly connected to each other. This method does not echo back to the client that sent the original message preventing an endless loop of repeated messages.
	public static void echo(int conNum, String message){

		try{
			clientThread t = null;
			for(int i = 0; i < al.size(); ++i) {
				t = al.get(i);
				if(t.connectionNum!=conNum){
					try {
						t.out.write(message);
						t.out.flush();
					}
					catch(Exception e) {
						System.out.println("Echo error "+e);
					}
				}
			}
		}
		catch(Exception e) {
			System.out.println("Could not echo error: "+e);
		}


	}

	// This method allows a client to disconnect from a connection they initiated or recieved. THIS WILL NOT ALLOW DISCONNECTION FROM CLIENTS OUTSIDE OF CONNECTION ARRAYLIST
	public static void disconnect(){

		send("disconnect");

		try {
			for(int i = 0; i < al.size(); ++i) {
				clientThread t = al.get(i);
				t.disconnect();
				t.remove();
			}
			al.clear();
		}
		catch(Exception e) {
			System.out.println("Server close error "+e);
		}
	}
	

	

	//This is the server thread that allows a client to accept connections at a specific port and IP
	public static class serverThread implements Runnable {

		private int port;
		ServerSocket svrSock;
		boolean on=true;

		public serverThread(int p) {
			port = p;
		}

		public void interupt(){
			this.interupt();
		}
		public void close(){

			on=false;
			try {
				Socket a = new Socket(InetAddress.getLocalHost(), defaultPort);
				svrSock.close();
			} 
			catch (Exception e) {
				System.out.println("Error turning server off: "+e);
			}
		}

		public void run() {

			try{
				svrSock = new ServerSocket(port);
				System.out.println("Server on"); 
				InetAddress ip = InetAddress.getLocalHost();
				System.out.println("Chat hosted at " + ip.getHostAddress()+" on port "+port);
			}
			catch(Exception e) {
				System.out.println("ServerSocket constructor error: "+e);
			}

			while(1==1){

				while(!on){
					if (on) break;
				}
				System.out.println("Accepting Connections");

				while(on){
					try{
						Socket sock = svrSock.accept();
						if(!on){
							System.out.println("Server off"); 
							break;
						}
						System.out.println("Accepted connection from: "+sock.getInetAddress().getHostAddress());

						clientThread t = new clientThread(sock, numConnection);
						numConnection++;
						al.add(t);
						new Thread (t).start();
					}
					catch(Exception e) {
						System.out.println("Server error "+e);
					}
				}
			}
		}
	}


	//This is the thread that allows for new connections to be added and monitored for incoming messages without disabling the user from inputing commands or messages.
	public static	class clientThread implements Runnable {

		private int connectionNum;
		char[] cbuffer= new char[100];
		private Socket sock;
		InputStreamReader in;
		BufferedWriter out;
		// code to implement an ALIAS would go here and could replace connectionNum as an identifier as long as duplicate names where handled gracefully

		public clientThread(Socket skt, int num) {
			sock=skt;
			connectionNum = num;
		}

		public void interrupt() {
			this.interrupt();
		}

		public void run() {

			int bytesRead = 0;
			String message = null;

			try{
				in = new InputStreamReader(sock.getInputStream());
				out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			}
			catch(Exception e) {
				System.out.println("Could not construct stream I/O: "+e);
			}


			while(1==1){	
				try{	
					while (!in.ready()) {}
					bytesRead = in.read(cbuffer);

					//If read returns -1 stream has been lost, print connection broken and call disconnect method to close socket and I/O streams
					if(bytesRead==-1){
						System.out.println("Connection with \""+sock.getInetAddress().getHostAddress()+"\" lost!" );
						this.disconnect();
						break;
					}
					
					echo(connectionNum, new String(cbuffer,0,bytesRead));
					//construct message to string if Send command
					if(cbuffer[0]=='s'){
						message = new String(cbuffer,5,bytesRead-5);
						System.out.println("\""+message+"\" from: "+sock.getInetAddress().getHostAddress());
					}
					//print disconnecting if Disconnect command
					else if(cbuffer[0]=='d'){
						System.out.println("Client has disconnected");
						this.disconnect();
						this.remove();
					}
					else{
						System.out.println("WTF kind of message did you send?");
					}

					//System.out.println("message from connectionNum: " +connectionNum);	

				}
				catch(Exception e) {
					System.out.println("Connection: "+connectionNum+" removed");
					this.remove();
					this.disconnect();
					break;
				}
			}
		}

		private void remove(){
			try{
				clientThread t = null;
				for(int i = 0; i< al.size(); ++i){
					t = al.get(i);
					if(t.connectionNum==this.connectionNum){
						al.remove(i);
					}
				}
			}
			catch(Exception e){
				System.out.println("Remove client after disconnect error "+e);
			}
		}

		private void disconnect(){
			try {
				in.close();
				out.close();
				sock.close();
			}
			catch (IOException e) {
				System.out.println("disconnect error: "+e);
			}
		}
	}
}