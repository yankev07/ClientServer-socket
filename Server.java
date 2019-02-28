import java.awt.*;
import java.awt.event.*;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.*;


public class Server extends JFrame {

	Map<String,ClientData> clientDictionary = new HashMap<String, ClientData>();
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private boolean ServerOn = true;
	private String httpMessage;
	private int port = 32000;
	static DataInputStream din;
	static DataOutputStream dout;
	ClientData clientData;
	ObjectInputStream ois;

	
	public Server() {
		initializeGUI();
	}

	
	/**
	* This function is called to initiate the server
	*/
	private void initiateConnection() {
		textArea1.append(" 1001675766 DS_Lab1 Server Initiated\n");
		textArea1.append(" Waiting for Clients to Connect.....\n");
		try {
			// Create a socket for the server and turn it on
			serverSocket = new ServerSocket(port);
			while (ServerOn) {
				socket = serverSocket.accept();
				ClientHandler clientThread = new ClientHandler(socket);
				clientThread.start();   
			}

			serverSocket.close();
			textArea1.append("Server Stopped..\n");
		}
		catch(Exception exec){
			exec.printStackTrace();
		}
	}

	
	/**
	* This function is called whenever the Client clicks
	* on the "Exit" button
	*/
	private void exitMouseClicked(MouseEvent e) {
		try {
			
			if(socket == null ){
				// Turn the server off, then close its socket
				ServerOn = false;
				serverSocket.close();
				System.exit(0);
			}
			else{
				socket.close();
				serverSocket.close();
				ServerOn = false;
				System.exit(0);
			}
		} 
		catch (IOException e1) {
			System.out.println("Force Closed Sockets");
			textArea1.append("Server Stopped..");
		}
	}

	
	/**
	* The main function! the code execution begins Here
	*/
	public static void main(String[] args) {
		Server server = new Server();
		server.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		server.setVisible(true);
		server.initiateConnection();
	}
	

	/**
	* This function builds the HTTP message that will be sent to the server
	* In fact, it takes as argument the actual message that will be sent
	* and attaches the HTTP headers
	*/
	private String buildHttpMessage(String message){

		String header_tmp1 = " POST / HTTP/1.1";
		String header_tmp2 = " Host: http://localhost/";
		String header_tmp3 = " Date: ";
		String header_tmp4 = " User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36";
		String header_tmp5 = " Content-Type: text/html,application/xhtml+xml,application/xml;q=0.9";
		String header_tmp6 = " Content-Length: ";
		String body = " Message: " + message;

		// Compute the current time (time when the HTTP message is sent)
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd / HH:mm:ss");
		String currentTime = LocalDateTime.now().format(formatter);

		// Compute the message content's lenght
		int message_length = message.length();

		// Generate the full HTTP message by binding the headers with the client's request
		String fullMessage = header_tmp1 + "\n" + header_tmp2 + "\n" + header_tmp3 + currentTime + "\n" + header_tmp4 + "\n" + header_tmp5 + "\n" + header_tmp6 + message_length + "\n\n" + body;

		return fullMessage;
	}
	

	/**
	 * Initialization of the SWING GUI 
	 */
	private void initializeGUI() {
		
		scrollPane1 = new JScrollPane();
		textArea1 = new JTextArea();
		button1 = new JButton();

		setTitle("Server");
		Container contentPane = getContentPane();
		contentPane.setLayout(null);

		{
			scrollPane1.setViewportView(textArea1);
		}
		contentPane.add(scrollPane1);
		scrollPane1.setBounds(5, 5, 385, 230);

		// Initialization of the "Exit" button
		button1.setText("Exit");
		button1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				exitMouseClicked(e);
			}
		});
		contentPane.add(button1);
		button1.setBounds(new Rectangle(new Point(320, 240), button1.getPreferredSize()));

		// compute the GUI window size
		{ 
			Dimension preferredSize = new Dimension();
			for(int i = 0; i < contentPane.getComponentCount(); i++) {
				Rectangle bounds = contentPane.getComponent(i).getBounds();
				preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
				preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
			}
			Insets insets = contentPane.getInsets();
			preferredSize.width += insets.right;
			preferredSize.height += insets.bottom;
			contentPane.setMinimumSize(preferredSize);
			contentPane.setPreferredSize(preferredSize);
		}

		pack();
		setLocationRelativeTo(getOwner());
	}


	
	private JScrollPane scrollPane1;
	private JTextArea textArea1;
	private JButton button1;
	
	/**
	* The handler thread class. This class will be
	* responsible for a dealing with the client and broadcasting its messages.
	*/

	class ClientHandler extends Thread {
		private Socket socket;
		private ObjectOutputStream oos, writer;
		private ObjectInputStream ois;
		public ClientHandler() {
			super();
		}

		
		public ClientHandler(Socket s) {
			socket = s;
		}

		
		@Override
		public void run() {
			try {
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());

				// Request a name from this client. Keep requesting until a valid name is submitted
				while (true) {

					oos.writeObject("SUBMITNAME");
					oos.flush();
					
					// reading input stream object from client
					clientData = (ClientData) ois.readObject();

					clientData.setReader(ois);
					clientData.setWriter(oos);
					// Verify that the client hasn't enterred a blank name
					if (clientData.getName() == null || clientData.getName().isEmpty()) {
						return;
					}
					else{
						// Notify the client that his name was accepted
						// from now on, the client will be connected to the server and can receive messages
						clientDictionary.put(clientData.getName(), clientData);
						httpMessage = " Welcome! " + clientData.getName() + ", you are conected to the Server...";
						textArea1.append(printGuiSeparator() + buildHttpMessage(httpMessage) + "\n");
						oos.writeObject("NAMEACCEPTED" + "#&#" + httpMessage);
						break;
					}					
				} 

				// Server loop that handles interactions with the clients
				while(true){

					// reading input stream object from client
					String str = (String) ois.readObject();
					System.out.println("CLIENT SAYS >"+str);

					// In case the client sends a sleep request
					if(str.startsWith("SERVER_SLEEP")){
						// Parse the HTTP message from the client to extract information
						String sleepingTime = str.split("#&#")[1];
						String clientName = str.split("#&#")[2];
						String httpHeader = str.split("#&#")[3];

						// Convert the random number generated by the client into an integer
						int timer = Integer.parseInt(sleepingTime) * 1000;

						// Notify the client that the server is about to sleep
						textArea1.append(printGuiSeparator() + httpHeader + "\n");
						httpMessage = " Server Now Sleeping...";
						textArea1.append(printGuiSeparator() + buildHttpMessage(httpMessage) + "\n");
						oos.writeObject("SERVER_SLEEP" + "#&#" + httpMessage);

						Thread.sleep(timer);

						// Notify the client that the server woke up from sleep
						httpMessage = " Server waited " + sleepingTime + " seconds for Client : " + clientName;
						oos.writeObject("SERVER_AWAKE" + "#&#" + clientName + "#&#" + sleepingTime + "#&#" + buildHttpMessage("") + "#&#" + httpMessage);
						textArea1.append(printGuiSeparator() + buildHttpMessage(httpMessage) + "\n");
						
					}

					// In case the client wants to disconnect from the server
					else if(str.startsWith("CLOSE_CLIENT")){

						// Parse the HTTP message to extract information
						String clientName = str.split("#&#")[1];
						// Notify the server that the client disconnected
						clientDictionary.remove(clientName);
						textArea1.append(printGuiSeparator() + " Connection Lost with Client " + clientName + "\n Client Thread (ThreadID : " + Thread.currentThread().getId() + ") Closed.\n");
						// Close client thread
						Thread.currentThread().interrupt();
						return;
					}	
				}
			}
			catch (java.net.SocketException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
				System.out.println("Interupting this thread");
				return;
			}
			catch (IOException e) {
				System.out.println(e);
				e.printStackTrace();
			}
			catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			} 
			finally {
				try {
					socket.close();
				}
				catch (IOException e) {

				}
				try {
					ois.close(); 
					oos.close(); 
					this.socket.close(); 
				} 
				catch(IOException ioe) { 
					ioe.printStackTrace(); 
				} 
			}
		}
	}

	/**
	* Utility function to print a separator and
	* make the scroll panel easily readable
	*/
	private String printGuiSeparator(){
		return "===============================================================================================================\n";
	}
}
