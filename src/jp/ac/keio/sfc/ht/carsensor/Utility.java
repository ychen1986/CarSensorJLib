/**
 * Copyright (C) 2015,  @author Yin Chen
 * Keio University, Japan
 */
package jp.ac.keio.sfc.ht.carsensor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utility {
	public static String getCurrentTimestamp() {
		String timestamp = "";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setTimeZone(TimeZone.getDefault());
		int offset = df.getTimeZone().getRawOffset() / 3600000;
		if (offset >= 10) {
			timestamp = df.format(new Date()) + "+" + offset + ":00";
		} else if (offset >= 0) {
			timestamp = df.format(new Date()) + "+0" + offset + ":00";
		} else if (offset > -10) {
			timestamp = df.format(new Date()) + "-0" + (-offset) + ":00";
		} else {
			timestamp = df.format(new Date()) + "-" + (-offset) + ":00";
		}
		return timestamp;
	}

	public static long getUNIXTime(String timeStamp) throws ParseException {

		timeStamp = timeStamp.substring(0, timeStamp.length() - 6);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setTimeZone(TimeZone.getDefault());
		Date date = null;

		date = df.parse(timeStamp);

		return date.getTime();

	}

	public static String getFormatedTimestamp(long time) {
		String timestamp = "";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setTimeZone(TimeZone.getDefault());
		int offset = df.getTimeZone().getRawOffset() / 3600000;
		if (offset >= 10) {
			timestamp = df.format(new Date(time)) + "+" + offset + ":00";
		} else if (offset >= 0) {
			timestamp = df.format(new Date(time)) + "+0" + offset + ":00";
		} else if (offset > -10) {
			timestamp = df.format(new Date(time)) + "-0" + (-offset) + ":00";
		} else {
			timestamp = df.format(new Date(time)) + "-" + (-offset) + ":00";
		}
		return timestamp;
	}

	public static void reverseBytes(byte[] a) {
		int l = a.length;
		for (int j = 0; j < l / 2; j++) {
			byte temp = a[j];
			a[j] = a[l - j - 1];
			a[l - j - 1] = temp;
		}
	}
}
