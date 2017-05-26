package jp.ac.keio.sfc.ht.carsensor.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jivesoftware.smack.SmackException.NotConnectedException;

import jp.ac.keio.sfc.ht.sox.protocol.TransducerValue;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;
import jp.ac.keio.sfc.ht.sox.soxlib.SoxDevice;

public class ReadPublishCarSensor {

	SoxConnection con;
	SoxDevice dev;

	int replay_delay = 1000;

	public static void main(String args[]) {

		new ReadPublishCarSensor(args);
	}

	public ReadPublishCarSensor(String[] args) {

		if(args.length!=3 && args.length!=4){
			System.out.println("usage: java -jar ReadPublishCarSensor SOX_SERVER_ADDRESS SOX_DEVICE_NAME CSV_FILE REPLAY_DELAY");
			System.out.println("or java ReadPublishCarSensor SOX_SERVER_ADDRESS SOX_DEVICE_NAME CSV_FILE REPLAY_DELAY");
			System.exit(0);
		}
		
		try {

			con = new SoxConnection(args[0], false);
			dev = new SoxDevice(con, args[1]);

			File csv = new File(args[2]); // CSVデータファイル

			if(args.length == 4){
			    replay_delay = Integer.parseInt(args[3]);
			}
	
			if (replay_delay > 1000 || replay_delay < 10){
			
		       throw new Exception("Invalid replay delay: "+replay_delay+". Replay delay must be 10~1000");

			}
			BufferedReader br = new BufferedReader(new FileReader(csv));

			String line = "";
			line = br.readLine(); // カラム名のレコード

			String acc_x = "";
			String acc_y = "";
			String acc_z = "";
			String vel_x = "";
			String vel_y = "";
			String vel_z = "";
			String geo_x = "";
			String geo_y = "";
			String geo_z = "";
			String airp = "";
			String temp = "";
			String light = "";
			String pm25 = "";
			String satnum = "";
			String lon = "";
			String lat = "";
			String alt = "";
			String speed = "";
			String cource = "";

			br.readLine();// 最初の１行は飛ばす

			while ((line = br.readLine()) != null) {

				String[] strs = line.split(",");

				alt = strs[3];
				light = strs[4];
				pm25 = strs[5];
				lat = strs[6];
				cource = strs[7];
				airp = strs[8];
				temp = strs[10];
				lon = strs[12];
				acc_z = strs[13];
				acc_x = strs[14];
				acc_y = strs[15];
				geo_x = strs[16];
				geo_y = strs[17];
				geo_z = strs[18];
				satnum = strs[21];
				vel_y = strs[22];
				vel_x = strs[23];
				vel_z = strs[24];
				speed = strs[25];

				// publish data
				this.publish(alt, light, pm25, lat, cource, airp, temp, lon, acc_z, acc_x, acc_y, geo_x, geo_y, geo_z,
						satnum, vel_y, vel_x, vel_z, speed);
				
				try{
					Thread.sleep(replay_delay);
				}catch(Exception e){
					e.printStackTrace();
				}

			}
			br.close();

		} catch (FileNotFoundException e) {
			// Fileオブジェクト生成時の例外捕捉
			e.printStackTrace();
		} catch (Exception e) {
			// BufferedReaderオブジェクトのクローズ時の例外捕捉
			e.printStackTrace();
		}
	}

	public void publish(String alt, String light, String pm25, String lat, String cource, String airp, String temp,
			String lon, String acc_z, String acc_x, String acc_y, String geo_x, String geo_y, String geo_z,
			String satnum, String vel_y, String vel_x, String vel_z, String speed) {
		List<TransducerValue> valueList = new ArrayList<TransducerValue>();

		TransducerValue v1 = new TransducerValue();
		v1.setId("Acceleration X");
		v1.setRawValue(acc_x);
		v1.setTypedValue(acc_x);
		v1.setCurrentTimestamp();

		TransducerValue v2 = new TransducerValue();
		v2.setId("Acceleration Y");
		v2.setRawValue(acc_y);
		v2.setTypedValue(acc_y);
		v2.setCurrentTimestamp();

		TransducerValue v3 = new TransducerValue();
		v3.setId("Acceleration Z");
		v3.setRawValue(acc_z);
		v3.setTypedValue(acc_z);
		v3.setCurrentTimestamp();

		TransducerValue v4 = new TransducerValue();
		v4.setId("Angular Velocity X");
		v4.setRawValue(vel_x);
		v4.setTypedValue(vel_x);
		v4.setCurrentTimestamp();

		TransducerValue v5 = new TransducerValue();
		v5.setId("Angular Velocity Y");
		v5.setRawValue(vel_y);
		v5.setTypedValue(vel_y);
		v5.setCurrentTimestamp();

		TransducerValue v6 = new TransducerValue();
		v6.setId("Angular Velocity Z");
		v6.setRawValue(vel_z);
		v6.setTypedValue(vel_z);
		v6.setCurrentTimestamp();

		TransducerValue v7 = new TransducerValue();
		v7.setId("Geomagnetism X");
		v7.setRawValue(geo_x);
		v7.setTypedValue(geo_x);
		v7.setCurrentTimestamp();

		TransducerValue v8 = new TransducerValue();
		v8.setId("Geomagnetism Y");
		v8.setRawValue(geo_y);
		v8.setTypedValue(geo_y);
		v8.setCurrentTimestamp();

		TransducerValue v9 = new TransducerValue();
		v9.setId("Geomagnetism Z");
		v9.setRawValue(geo_z);
		v9.setTypedValue(geo_z);
		v9.setCurrentTimestamp();

		TransducerValue v10 = new TransducerValue();
		v10.setId("Atmospheric Pressure");
		v10.setRawValue(airp);
		v10.setTypedValue(airp);
		v10.setCurrentTimestamp();

		TransducerValue v11 = new TransducerValue();
		v11.setId("Atmospheric Temperature");
		v11.setRawValue(temp);
		v11.setTypedValue(temp);
		v11.setCurrentTimestamp();

		TransducerValue v12 = new TransducerValue();
		v12.setId("Illuminance");
		v12.setRawValue(light);
		v12.setTypedValue(light);
		v12.setCurrentTimestamp();

		TransducerValue v13 = new TransducerValue();
		v13.setId("PM2.5");
		v13.setRawValue(pm25);
		v13.setTypedValue(pm25);
		v13.setCurrentTimestamp();

		TransducerValue v14 = new TransducerValue();
		v14.setId("Satellite Number");
		v14.setRawValue(satnum);
		v14.setTypedValue(satnum);
		v14.setCurrentTimestamp();

		TransducerValue v15 = new TransducerValue();
		v15.setId("Longitude");
		v15.setRawValue(lon);
		v15.setTypedValue(lon);
		v15.setCurrentTimestamp();

		TransducerValue v16 = new TransducerValue();
		v16.setId("Latitude");
		v16.setRawValue(lat);
		v16.setTypedValue(lat);
		v16.setCurrentTimestamp();

		TransducerValue v17 = new TransducerValue();
		v17.setId("Altitude");
		v17.setRawValue(alt);
		v17.setTypedValue(alt);
		v17.setCurrentTimestamp();

		TransducerValue v18 = new TransducerValue();
		v18.setId("Speed");
		v18.setRawValue(speed);
		v18.setTypedValue(speed);
		v18.setCurrentTimestamp();

		TransducerValue v19 = new TransducerValue();
		v19.setId("Cource");
		v19.setRawValue(speed);
		v19.setTypedValue(speed);
		v19.setCurrentTimestamp();

		
		valueList.add(v1);
		valueList.add(v2);
		valueList.add(v3);
		valueList.add(v4);
		valueList.add(v5);
		valueList.add(v6);
		valueList.add(v7);
		valueList.add(v8);
		valueList.add(v9);
		valueList.add(v10);
		valueList.add(v11);
		valueList.add(v12);
		valueList.add(v13);
		valueList.add(v14);
		valueList.add(v15);
		valueList.add(v16);
		valueList.add(v17);
		valueList.add(v18);
		valueList.add(v19);
		

		try {
			dev.publishValues(valueList);
		} catch (NotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
