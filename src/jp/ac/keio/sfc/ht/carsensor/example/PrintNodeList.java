package jp.ac.keio.sfc.ht.carsensor.example;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jp.ac.keio.sfc.ht.sox.soxlib.SoxConnection;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

public class PrintNodeList {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			new PrintNodeList();
		} catch (SmackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
public PrintNodeList() throws SmackException, IOException, XMPPException {
		
		
		SoxConnection con;
		con = new SoxConnection("soxfujisawa.ht.sfc.keio.ac.jp","guest","miroguest", true);
		List<String> nodeList = con.getAllSensorList();
		Collections.sort(nodeList);
		for(String node:nodeList){
			System.out.println(node);
			
		}
				
		
		
	}

}
