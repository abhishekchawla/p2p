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

	public void SetData(byte[] buffer) {
		data = Arrays.copyOf(buffer, size);
	}
}

class FileSplitter {
	private static List<Chunk> chunkList = Collections.synchronizedList(new ArrayList<Chunk>());

	public static void splitFile(File f) throws IOException {

		try (BufferedInputStream buffInputStream = new BufferedInputStream(new FileInputStream(f))) {

			int sizeOfChunk = 1024 * 100;// 100KB
			byte[] buffer = new byte[sizeOfChunk];

			int len = 0;
			while ((len = buffInputStream.read(buffer)) > 0) {

				Chunk chunk = new Chunk();
				chunk.size = len;
				chunk.SetData(buffer);

				chunkList.add(chunk);
			}
		}
	}

	public static Chunk GetChunk(int n) {

		if (n < chunkList.size())
			return chunkList.get(n);
		else {
			Chunk chunk = new Chunk();
			chunk.data = null;
			return chunk;
		}

	}

	public static int GetNumberOfChunks() {

		return chunkList.size();
	}
}

class PropReader {
	static Properties properties = new Properties();
	static InputStream inputStream = null;
	static String fileName = "../Config.prop";

	PropReader() {
		try {
			inputStream = new FileInputStream(fileName);
			// load a properties file
			properties.load(inputStream);
		} catch (FileNotFoundException ie) {
			System.out.println("ConfigFileNotFound");
		} catch (IOException ie) {
			System.out.println("UnableToLoadConfigFile");
		}
	}

	public static int GetServerPort() {
		return Integer.parseInt(properties.getProperty("server"));
	}

	public static int GetNumberOfClients() {
		return Integer.parseInt(properties.getProperty("clients"));
	}

	public static String GetFileName() {
		return properties.getProperty("filename");
	}

	public static void SetNoOfChunks(int n) throws IOException {
		properties.setProperty("chunks", Integer.toString(n));
		File configFile = new File(fileName);
		FileWriter writer = new FileWriter(configFile);
		properties.store(writer, "");
		writer.close();
	}

}

public class Server2 {
	static PropReader propReader = new PropReader();
	static FileSplitter fileSplitter = new FileSplitter();
	private static final int sPort = propReader.GetServerPort(); // The server
																	// will be
																	// listening
																	// on
	static int clientNum = 0;
	// this port number
	// private static List<Thread> _threads = new ArrayList<Thread>();

	public static void main(String[] args) throws Exception {
		fileSplitter.splitFile(new File(propReader.GetFileName()));
		/* fileSplitter.MergeFile("HW1.pdf"); */
		propReader.SetNoOfChunks(fileSplitter.GetNumberOfChunks());

		System.out.println("The server is running.");
		ServerSocket listener = new ServerSocket(sPort);

		try {
			while (true) {

				if (clientNum == propReader.GetNumberOfClients())
					break;

				Handler handler = new Handler(listener.accept(), clientNum);
				handler.SetNumberOfClients(propReader.GetNumberOfClients());
				(new Thread(handler)).start();

				System.out.println("Client " + clientNum + " is connected!");
				clientNum++;
			}
		} finally {
			listener.close();
		}

	}

	/**
	 * A handler thread class. Handlers are spawned from the listening loop and
	 * are responsible for dealing with a single client's requests.
	 */

	private static class Handler implements Runnable {
		private Socket connection;
		private ObjectInputStream in; // stream read from the socket
		private ObjectOutputStream out; // stream write to the socket
		private int no; // The index number of the client
		private int nClients = 0;
		private int _nChunkCounter = 0;

		public void SetNumberOfClients(int n) {
			nClients = n;
		}

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
			_nChunkCounter = no; // nth client will get nth chunk
		}

		public void start() {
		}

		public void run() {
			try {
				// initialize Input and Output streams
				System.out.println("Running thread");
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				out.writeObject((int) no + 1);
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				// try
				// {
				while (true) {
					// receive the message sent from the client
					// message = (String)in.readObject();
					// show the message to the user
					// System.out.println("Receive message: " + message + " from
					// client " + no);
					// Capitalize all letters in the message
					// MESSAGE = message.toUpperCase();
					// send MESSAGE back to the client
					// sendMessage(MESSAGE);
					Chunk chunk = fileSplitter.GetChunk(_nChunkCounter);
					if (chunk.data != null) {
						chunk.id = _nChunkCounter;
						SendData(chunk);
					} else {
						chunk.id = -1;
						SendData(chunk);
						break;
					}

					// sendData(FileSplitter.GetChunk(_nChunkCounter));
					_nChunkCounter += nClients;// = nClients;
				}
				// }
				// catch(ClassNotFoundException classnot)
				// {
				// System.err.println("Data received in unknown format");
				// }
			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + no);
			} finally {
				// Close connections
				try {
					in.close();
					out.close();
					connection.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client " + no);
				}
			}
		}

		public void SendData(Chunk chunk) {
			try {
				out.writeObject(chunk);
				out.flush();
				System.out.println("Chunk sent : " + chunk.id);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		public void SendData(byte[] chunk) {
			try {
				out.writeObject(chunk);
				out.flush();
				System.out.println("Chunk sent : " + _nChunkCounter);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

	}

}
