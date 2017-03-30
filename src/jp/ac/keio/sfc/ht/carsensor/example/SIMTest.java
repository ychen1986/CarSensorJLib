package jp.ac.keio.sfc.ht.carsensor.example;

import jp.ac.keio.sfc.ht.carsensor.SIMCard;

public class SIMTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String sim = null;
		try {
			sim = SIMCard.getSIMInfo();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("[SIMTest] " + sim);
	}

}
