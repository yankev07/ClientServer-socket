import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;


public class Client extends JFrame {

	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private static Client client;
	private String serverAddress = "localhost";
	private String globalClientName;
	private String line;
	private String httpMessage;
	private Socket socket;
	private int port = 32000;
	private int globalProcessId;
	private int sleepingTime;

	/**
	* Utility function that displays an Input Dialog and asks por the Client name
	*/
	private String getClientName() {
		return JOptionPane.showInputDialog(this, "Client Identification :", "Welcome!", JOptionPane.QUESTION_MESSAGE);
	}

	public Client() {
		initializeGUI();
	}

	/**
	* The function where all the processing is done
	* We will maintain an infinite loop and parse 
	*/
	private void clientLoop(String clientName, int processID) {

		globalClientName = clientName;
		globalProcessId = processID;

		
		try {
			socket = new Socket(serverAddress, port);
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			ClientData clientData = new ClientData(globalClientName, processID);

			while (true) {
				try {
					line = (String) ois.readObject();
				} 
				catch (Exception e) {
					e.printStackTrace();
					break;
				}

				System.out.println("Server Request>" + line);

				// In case the server requests the Client name, we will send him the client name and his process ID
				if (line.startsWith("SUBMITNAME")) {
					oos.writeObject(clientData);
					oos.flush();
				}
				// When the server notifies that the Client name has been accepted 
				else if (line.startsWith("NAMEACCEPTED")) {
					String server_response = line.split("#&#")[1];
					messageArea.setText(server_response + "\n");
					messageArea.append(" Your ProcessID is " + globalProcessId + "\n");
					messageArea.append(" Server Waiting for your Instructions...\n");
				}
				// The server notifies the Client before starting to sleep
				else if (line.startsWith("SERVER_SLEEP")) {
					String server_response = line.split("#&#")[1];
					messageArea.append(server_response + "\n");
				}
				// The server notifies the Client that it is available after sleeping
				else if (line.startsWith("SERVER_AWAKE")) {
					String client_name = line.split("#&#")[1];
					String sleepingTime = line.split("#&#")[2];
					String server_response = line.split("#&#")[4];
					messageArea.append(server_response + "\n");
				}
			}
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	* The main function! the code execution begins Here
	*/
	public static void main(String[] args) throws Exception {

		client = new Client();
		client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.setVisible(true);

		//Get ths Client name and store it in a local variable
		String client_name = client.getClientName();
		// The process ID will be generated as a random number between 1000 and 2000
		int client_processId = new Random().nextInt(1000 + 1)  + 1000;
		//Here we set the title of the GUI Window
		client.setTitle("Client : " + client_name + " / " + "Process ID : " + client_processId + "\n");
		client.clientLoop(client_name, client_processId);
	}


	/**
	* This function is called whenever the Client clicks
	* on the "Exit" button
	*/
	private void exitButtonClicked(MouseEvent e) {
		try {
			// We must notify the server that the Client has quitted
			// The server will then close the client's thread
			oos.writeObject("CLOSE_CLIENT" + "#&#" + globalClientName);
			oos.flush();
			System.exit(0);
		}
		catch (IOException e1) {
			e1.printStackTrace();
			try {
				oos.close();
				ois.close();
				socket.close();
			}
			catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}

	
	/**
	* This function will be called whenever the client clicks
	* on the "Sleep" button
	*/
	private void sleepButtonClicked(MouseEvent e) {
		try {
			// We will first generate a random integer between 5 and 15
			sleepingTime = new Random().nextInt(10 + 1)  + 5;
			// Then, we format the sleep request message 
			httpMessage = globalClientName + " requests Server Sleep for " + sleepingTime + " seconds";
			messageArea.append(" " + httpMessage + "\n");
			// We send the message to the server in HTTP format by binding the headers with the request
			oos.writeObject("SERVER_SLEEP" + "#&#" + sleepingTime + "#&#" + globalClientName + "#&#" + buildHttpMessage(httpMessage));
		} 
		catch (IOException e1) {
			e1.printStackTrace();
		}
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
		messageArea = new JTextArea();
		button1 = new JButton();
		button2 = new JButton();

		setTitle("Client");
		Container contentPane = getContentPane();
		contentPane.setLayout(null);

		{
			scrollPane1.setViewportView(messageArea);
		}
		contentPane.add(scrollPane1);
		scrollPane1.setBounds(5, 10, 385, 220);

		// Initialization of the "Exit" button
		button1.setText("Exit");
		button1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				exitButtonClicked(e);
			}
		});
		contentPane.add(button1);
		button1.setBounds(new Rectangle(new Point(235, 240), button1.getPreferredSize()));

		// Initialization of the "Sleep" button
		button2.setText("Sleep");
		button2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				sleepButtonClicked(e);
			}
		});
		contentPane.add(button2);
		button2.setBounds(new Rectangle(new Point(70, 240), button2.getPreferredSize()));

		// compute the GUI window size
		{ 
			Dimension preferredSize = new Dimension();
			for (int i = 0; i < contentPane.getComponentCount(); i++) {
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
	private JTextArea messageArea;
	private JButton button1;
	private JButton button2;

}