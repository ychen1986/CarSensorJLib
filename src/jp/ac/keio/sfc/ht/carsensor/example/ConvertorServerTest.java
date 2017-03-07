package jp.ac.keio.sfc.ht.carsensor.example;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.ac.keio.sfc.ht.carsensor.protocol.RawSensorData;

public class ConvertorServerTest implements Runnable, AutoCloseable {

	RawSensorData data = null;
	Socket socket = null;
	ObjectOutputStream out = null;
	static String hostname = "carsensor.ht.sfc.keio.ac.jp";
	static int port = 6223;
	static int endingNO = 100;
	static int startingNO = 0;

	public ConvertorServerTest(byte[] _cmd, long _time) {
		// TODO Auto-generated constructor stub
		data = new RawSensorData(_cmd, _time);

		try {
			socket = new Socket(hostname, port);
			// socket = new Socket("localhost", 6222);

			out = new ObjectOutputStream(socket.getOutputStream());

		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	protected static void parseOptions(String[] args) {

		String usage = "Usage: java -jar ConvertorServerTest.jar  -o <port> -h <host> -s <startingNO> -e <endingNO>";
		if (args.length == 0) {
			System.err.println("ERROR: arguments required!");
			System.err.println(usage);
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-h")) {
				hostname = args[++i];
			} else if (args[i].equals("-o")) {
				port = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-s")) {
				startingNO = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-e")) {
				endingNO = Integer.parseInt(args[++i]);
			} else {
				System.err.println("ERROR: invalid option " + args[i]);
				System.err.println(usage);
				System.exit(1);
			}
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		parseOptions(args);

		ExecutorService executor = Executors.newFixedThreadPool(endingNO - startingNO + 1);
		ConvertorServerTest sensor = null;

		byte[] cmd_proto = { (byte) 0x8A, 0x00, 0x00, (byte) 0xC5, 0x12, 0x00, 0x00, 0x15, 0x00, 0x19, 0x00,
				(byte) 0xA0, 0x07, 0x13, 0x00, 0x06, 0x00, 0x05, 0x00, (byte) 0xC8, 0x00, 0x4A, 0x00, (byte) 0xF0,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xE3, 0x1B, 0x0E, (byte) 0xB7, 0x0C, 0x02, 0x00, 0x12, 0x00, 0x26, 0x02

		};
		for (int i = startingNO; i <= endingNO; i++) {
			byte[] cmd = new byte[cmd_proto.length];
			System.arraycopy(cmd_proto, 0, cmd, 0, cmd_proto.length);
			cmd[1] = (byte) i;
			if (i > 254) {
				cmd[2] = (byte) 1;
			}
			sensor = new ConvertorServerTest(cmd, System.currentTimeMillis());
			executor.execute(sensor);

		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		while (true) {
			try {
				Thread.sleep(10);

				out.writeObject(new RawSensorData(data.cmd, System.currentTimeMillis()));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}

	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		if (socket != null) {
			socket.close();
			out.close();
		}
	}

}
