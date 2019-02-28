import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class ClientData implements Serializable{	
	
	/**
	 * Getters and Setters of the Client and node's Data
	 */

	String name;
	int Process_id;
	ObjectOutputStream writer;
	ObjectInputStream reader;
	
	public ClientData(String clientName, int processID){
		super();
		this.name = clientName;
		Process_id = processID;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getProcess_id() {
		return Process_id;
	}
	public void setProcess_id(int process_id) {
		Process_id = process_id;
	}

	public ObjectOutputStream getWriter() {
		return writer;
	}

	public void setWriter(ObjectOutputStream writer) {
		this.writer = writer;
	}

	public ObjectInputStream getReader() {
		return reader;
	}

	public void setReader(ObjectInputStream reader) {
		this.reader = reader;
	}

}
