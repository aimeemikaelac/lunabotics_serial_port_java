package main;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;


public class SerialTest extends Thread implements SerialPortEventListener {
	private SerialPort serialPort;
	private int port;
	
	public SerialTest(int port) {
		this.port = port;
	}
	
        /** The port we're normally going to use. */
	private static final String PORT_NAMES[] = { 
			"/dev/tty.usbserial-A9007UX1", // Mac OS X
			"/dev/ttyUSB0", // Linux
			"COM3", // Windows
	};
	/**
	* A BufferedReader which will be fed by a InputStreamReader 
	* converting the bytes into characters 
	* making the displayed results codepage independent
	*/
	private BufferedReader input;
	/** The output stream to the port */
	private static OutputStream output;
	protected static ServerSocket servSocket;
	protected static Socket connectedSocket;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;

	public void initialize() {
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String inputLine=input.readLine();
				System.out.println(inputLine);
			}
			catch (IOException io) {
				System.out.println("caught io");
			}
			catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		// Ignore all the other eventTypes, but you should consider the other ones.
	}
	
	public void run() {
		try {
			while(true) {
				
				servSocket = new ServerSocket(port);
				System.out.println("created socket on port:" + port);
				connectedSocket = servSocket.accept();
				InputStream receivedInputStream=connectedSocket.getInputStream();
				int numBytes = receivedInputStream.available();
				System.out.println("Received data on: "+ port);
//				for(int i=0; i<numBytes; i++) {
					byte currentBytes[]=new byte[numBytes];
					receivedInputStream.read(currentBytes);
					//Integer intVal = new Integer(currentByte);
					String received = new String(currentBytes, "US-ASCII");
					Integer intVal = Integer.parseInt(received.toString());
					char message = (char) intVal.intValue();
					output.write(message);
					String stringVal = String.valueOf(message);
					if(stringVal == "\n") {
						stringVal = "NULL";
					}
					System.out.println("Read byte with value: " + stringVal);//+ Integer.toBinaryString(currentByte));
//				}
				servSocket.close();
				connectedSocket.close();
			}
		} catch (IOException e) {
			System.out.println("Could not open ServerSocket on port" + port);
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		SerialTest main = new SerialTest(2000);
		main.initialize();
		
		main.start();
		System.out.println("Started");
	}
}