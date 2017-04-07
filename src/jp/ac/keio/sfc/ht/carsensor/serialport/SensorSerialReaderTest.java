package jp.ac.keio.sfc.ht.carsensor.serialport;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPortEvent;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.TooManyListenersException;

public class SensorSerialReaderTest extends SensorSerialReader {

	public SensorSerialReaderTest(String _portName) throws UnsupportedCommOperationException,
			NoSuchPortException, PortInUseException, IOException, TooManyListenersException {
		super(_portName);
		// TODO Auto-generated constructor stub
	}

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see gnu.io.SerialPortEventListener#serialEvent(gnu.io.SerialPortEvent)
	 */
	public void serialEvent(SerialPortEvent event) {
		printBytesData(in);

	}

	void printBytesData(InputStream in) {
		int data;

		try {
			while ((data = in.read()) > -1) {

				String hex = byteToHexString((byte) data);
				if (hex.equals("9A")) {
					System.out.print('\n');
				}
				System.out.print(hex);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
