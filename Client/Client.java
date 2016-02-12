import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

class Chunk implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	byte[] data;
	int id;
	int size;

	Chunk() {
		data = null;
		id = 0;
		size = 0;
	}
}

class PropReader {
	static Properties properties = new Properties();
	static InputStream inputStream = null;
	static int clientNumber = 0;

	PropReader() {
		try {
			inputStream = new FileInputStream("../Config.prop");
			// load a properties file
			properties.load(inputStream);
		} catch (FileNotFoundException ie) {
			System.out.println("ConfigFileNotFound");
		} catch (IOException ie) {
			System.out.println("UnableToLoadConfigFile");
		}
	}

	public static int GetClientNumber() {
		return clientNumber;
	}

	public static void SetClientNumber(int n) {
		clientNumber = n;
	}

	public static int GetServerPort() {
		return Integer.parseInt(properties.getProperty("server"));
	}

	public static String GetFileName() {
		return properties.getProperty("filename");
	}

	public static int GetPort() {
		return Integer.parseInt(properties.getProperty("c" + clientNumber));
	}

	public static int GetSendersPort() {
		String str = properties.getProperty("c" + clientNumber + "_rec");
		return Integer.parseInt(properties.getProperty(str));
	}

	public static int GetReceiversPort() {
		String str = properties.getProperty("c" + clientNumber + "_snd");
		return Integer.parseInt(properties.getProperty(str));
	}

	public static String GetSendersName() {
		return properties.getProperty("c" + clientNumber + "_rec");
	}

	public static String GetReceiversName() {
		return properties.getProperty("c" + clientNumber + "_snd");
	}

	public static int GetNoOfChunks() {
		return Integer.parseInt(properties.getProperty("chunks"));
	}
}

public class Client {

	static PropReader propReader = new PropReader();
	static boolean _bSendingDone = false;

	// static int _noOfChunks = 5; //to be configured from outside
	int counter = 0;
	Socket requestSocket; // socket connect to the server
	ObjectOutputStream out; // stream write to the socket
	ObjectInputStream in; // stream read from the socket
	String message; // message send to the server
	String MESSAGE; // capitalized message read from the server
	static List<Chunk> _Chunks = Collections.synchronizedList(new ArrayList<Chunk>());
	static Hashtable<Integer, Boolean> _availableIDs = new Hashtable<Integer, Boolean>();
	static int _totNoOfChunks = propReader.GetNoOfChunks();

	Client() {
		// for(int i = 0 ; i < noOfChunks; i++)
		{
			// _availableIDs.put(i, 0);
			// _requiredIDs.put(i,1);
		}
	}

	void run() throws InterruptedException {
		try {
			// create a socket to connect to the server
			requestSocket = new Socket("localhost", propReader.GetServerPort());
			System.out.println("Connected to server in port : " + propReader.GetServerPort());
			// initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			propReader.SetClientNumber((int) in.readObject());

			boolean success = new File(String.valueOf(propReader.GetClientNumber())).mkdirs();
			if (!success) {
				System.out.println("Directory Creation failed");

				// Directory creation failed
			}
			while (true) {

				Chunk chunk = (Chunk) in.readObject();
				if (chunk.data == null)
					break;
				else {
					File newFile = new File(propReader.GetClientNumber() + "/" + propReader.GetFileName() + "."
							+ String.format("%03d", chunk.id));
					FileOutputStream out2 = new FileOutputStream(newFile);
					out2.write(chunk.data, 0, chunk.size);
					out2.close();
					_availableIDs.put(chunk.id, true);
				}

				System.out.println("Received Chunk : " + chunk.id + " from Server");
			}

			
			

			while (true) 
			{	
				
				if(_bSendingDone != true)
				{
					Thread.sleep(200);
					
					Thread thread = new Thread() {
						public void run() {
							try (ServerSocket listener = new ServerSocket(propReader.GetPort())) {
								System.out.println("Running thread");
								Handler handler = new Handler(listener.accept(), 2);
								handler.run();

							} catch (IOException ie) {
								System.out.println("Sending thread already running");
								// ie.printStackTrace();
							}
						}
					};
					thread.start();
				}
				// sending thread
				
				
				if (_availableIDs.size() != _totNoOfChunks) // continue
															// receiving chunks
															// as available chunks
															// are not eqaul to
															// the chunks to be
															// recieved
				{
					ReceiveData receiveData = new ReceiveData();
					receiveData.run();
				}

				
				if (_bSendingDone == true && _availableIDs.size() == _totNoOfChunks)
				{
					System.out.println("Done");
					break;
				}
					
			}

			MergeFiles();
			System.out.println("Chunks Merged");
			System.exit(0);
			
			// listenerThread.start();
		} catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			System.err.println("Is the error here?");
			ioException.printStackTrace();
		} finally {
			// Close connections
			try {
				in.close();
				out.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	// send a message to the output stream
	void sendMessage(String msg) {
		try {
			// stream write the message
			out.writeObject(msg);
			out.flush();
			System.out.println("Send message: " + msg);
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	public static void MergeFiles() throws IOException {
		try (FileOutputStream out = new FileOutputStream(
				propReader.GetClientNumber() + "/" + propReader.GetFileName())) {
			int sizeOfFiles = 1024 * 100;// 100KB
			byte[] buffer = new byte[sizeOfFiles];

			for (int i = 0; i < _totNoOfChunks; i++) {
				File newFile = new File(
						propReader.GetClientNumber() + "/" + propReader.GetFileName() + "." + String.format("%03d", i));

				try (BufferedInputStream buffInputStream = new BufferedInputStream(new FileInputStream(newFile))) {
					int len = 0;
					while ((len = buffInputStream.read(buffer)) > 0) {
						out.write(buffer, 0, len);
						// offset += sizeOfFiles;
					}
				}
			}
		}
	}

	// main method
	public static void main(String args[]) throws InterruptedException {
		Client client = new Client();
		client.run();
	}

	public Chunk GetChunk(int n) {

		if (n < _Chunks.size())
			return _Chunks.get(n);
		else
			return null;
	}

	public Chunk GetChunkById(int id) {
		for (int i = 0; i < _Chunks.size(); i++) {
			Chunk chunk = _Chunks.get(i);
			if (chunk.id == id) {
				return chunk;
			}
		}
		return null;
	}

	private class ReceiveData {
		private int sPort = propReader.GetSendersPort(); // Receive data from
															// this port
		Socket requestSocket; // socket connect to the server
		ObjectOutputStream out; // stream write to the socket
		ObjectInputStream in; // stream read from the socket

		public void run() throws ClassNotFoundException, InterruptedException, IOException {
			int retrycount = 0;
			// create a socket to connect to the server
			while (true) {
				try {
					requestSocket = new Socket("localhost", sPort);
					System.out.println("Connected to localhost in port " + sPort);
					break;
				} catch (IOException ie) {
					// ie.printStackTrace();

					Thread.sleep(300);

					System.out.println("Retrying connection...");
					if (retrycount > 5) {
						System.out.println("cannot connect");
						return;
					}

					retrycount++;
				}

			}

			//System.out.println("Connected to localhost in port " + propReader.GetSendersPort());
			// initialize inputStream and outputStream

			try
			{
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(requestSocket.getInputStream());
	
				Hashtable<Integer, Boolean> ht = (Hashtable<Integer, Boolean>) in.readObject();
	
				for (int key : ht.keySet()) {
					if (!_availableIDs.containsKey(key)) {
	
						out.writeObject(key);
						out.flush();
						byte[] buffer = (byte[]) in.readObject();
						int bufferLength = (int) in.readObject();
						File newFile = new File(propReader.GetClientNumber() + "/" + propReader.GetFileName() + "."
								+ String.format("%03d", key));
						FileOutputStream out2 = new FileOutputStream(newFile);
						out2.write(buffer, 0, bufferLength);
						out2.close();
						_availableIDs.put(key, true);
						System.out.println("Received Chunk " + key + " from : " + propReader.GetSendersName());
						counter++;
						Thread.sleep(10);
					}
				}
				System.out.println(counter + " chunks recieved from" + propReader.GetSendersName());
	
				if (_availableIDs.size() == _totNoOfChunks) // recieved all the
															// chunks, let the
															// sender know
					out.writeObject(_totNoOfChunks);
				else
					out.writeObject(-1);
				out.flush();
			}
			catch(IOException ie)
			{
				//System.out.println("Disconnect with Client " + propReader.GetSendersName());
				System.out.println("Connection reset by the Sender");
			}
		}
	}

	private class Handler extends Thread {
		private Socket connection;
		private ObjectInputStream in; // stream read from the socket
		private ObjectOutputStream out; // stream write to the socket

		public Handler(Socket connection, int no) {
			this.connection = connection;
		}

		public void run() {

			try {
				// initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				sendHT(_availableIDs);
				while (!_bSendingDone) {
					int index = (int) in.readObject(); // index of chunk
														// requested by the
														// client
					if (index == -1)					//Time to send a new hashtable and check
						break;
					else if (index == _totNoOfChunks)
					{
						_bSendingDone = true;
						break;
					}

					File newFile = new File(propReader.GetClientNumber() + "/" + propReader.GetFileName() + "."
							+ String.format("%03d", index));
					try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(newFile))) {
						int sizeOfChunk = 1024 * 100;// 100KB
						byte[] buffer = new byte[sizeOfChunk];

						int len = 0;
						len = bis.read(buffer);
						if (len > 0) {
							sendData(buffer);
							sendData(len);
						}

					}

					Thread.sleep(10);
				}
			} catch (

			IOException ioException) 
			{
				System.out.println("Disconnect with Client " + propReader.GetReceiversName());
				ioException.printStackTrace();
				_bSendingDone = true;
			} catch (

			ClassNotFoundException e)

			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally

			{
				// Close connections
				try {
					in.close();
					out.close();
					connection.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client " + propReader.GetReceiversName());
				}
			}

		}

		public void sendHT(Hashtable<Integer, Boolean> ht) {
			try {
				out.writeObject(ht);
				out.flush();
				System.out.println("HashTableSent");
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		public void sendData(byte[] buffer) {
			try {
				out.writeObject(buffer);
				out.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		public void sendData(int bufferLength) {
			try {
				out.writeObject(bufferLength);
				out.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

	}
}
