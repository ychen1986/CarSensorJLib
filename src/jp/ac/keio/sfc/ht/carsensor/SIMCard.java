/**
 * Copyright (C) 2015  @author Yin Chen 
 * Keio University, Japan
 */

package jp.ac.keio.sfc.ht.carsensor;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SIMCard implements Closeable {

	static private String phoneNumber = null;
	static private String ICCID = null;
	// private String IMEI;
	private static String SIMInfoFile = "/tmp/.flags/.sim_exist";
	private BufferedReader reader = null;
	private static boolean debug = false;

	private SIMCard(String _SIMInfoFile) throws IOException {
		if (_SIMInfoFile != null) {
			SIMInfoFile = _SIMInfoFile;
		}
		Path path = Paths.get(SIMInfoFile);
		reader = Files.newBufferedReader(path);
	}

	public static String getPhoneNumber(String _SIMFile, boolean _debug) throws Exception {
		debug = _debug;
		if (phoneNumber != null) {
			return phoneNumber;
		}

		SIMCard sim = null;
		String phoneNumber = null;
		try {
			sim = new SIMCard(_SIMFile);
			sim.readSIM();
			phoneNumber = SIMCard.phoneNumber;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (sim != null)
				sim.close();
		}

		if (phoneNumber == null) {
			Exception e = new Exception("Phone number is not obtained.");
			throw e;
		}

		return phoneNumber;
	}

	public static String getICCID(String _SIMFile, boolean _debug) throws Exception {
		getPhoneNumber(_SIMFile, _debug);
		return ICCID;
	}

	public static String getICCID(String _SIMFile) throws Exception {
		return getICCID(_SIMFile, false);
	}

	public static String getICCID(boolean _debug) throws Exception {
		return getICCID(SIMInfoFile, _debug);
	}

	public static String getICCID() throws Exception {
		return getICCID(SIMInfoFile, false);
	}

	public static String getSIMInfo() {
		return getSIMInfo(false);
	}

	public static String getSIMInfo(boolean _debug) {
		try {
			return "ICCID: " + getICCID(_debug) + ",CNUM: " + getPhoneNumber(_debug);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "SIM information is unavailble!";
		}

	}

	public static String getPhoneNumber(boolean _debug) throws Exception {
		return getPhoneNumber(null, _debug);
	}

	public static String getPhoneNumber() throws Exception {
		return getPhoneNumber(null, false);
	}

	@Override
	public void close() {

		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readSIM() {
		String s = "";

		try {

			while ((s = reader.readLine()) != null) {
				if (debug) {
					System.out.println("[SIMCard]: A new line " + s);
				}
				if (s.startsWith(PHONE_NUMBER + ":")) {
					String[] resp = s.split(",", 3);
					String[] numSentence = resp[1].split("\"", 3);
					phoneNumber = numSentence[1];
					if (debug) {
						System.out.println("[SIMCard]: Phone number sentence split");
						for (String i : numSentence) {
							System.out.println(i);
						}
						System.out.println("[SIMCard]: Phone number obtained " + phoneNumber);
					}
					continue;
				}
				if (s.startsWith(PHONE_ICCID + ":")) {
					String[] resp = s.split(" ", 2);
					ICCID = resp[1];
					if (debug) {

						System.out.println("[SIMCard]: Phone ICCID obtained " + phoneNumber);
					}
					continue;
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return;

	}

	@SuppressWarnings("unused")
	final static private String AT_PREFIX = "AT";
	final static private String PHONE_NUMBER = "+CNUM";
	@SuppressWarnings("unused")
	final static private String PHONE_IMEI = "+CGSN";
	final static private String PHONE_ICCID = "+CCID";

}
