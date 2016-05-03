package rogatkin.mobile.data.pertusin;

import java.io.IOException;
import java.util.Calendar;

import rogatkin.mobile.data.pertusin.NetAssistant.Message;

public class Main {
	public static boolean __debug = false;
	
	public static void main(String ...params) {		
		System.out.printf("Pertusin (c) %d Dmitriy Rogatkin%n", Calendar.getInstance().get(Calendar.YEAR));
		if (params.length > 0) {
			switch(params[0].charAt(0)) {
			case 'm':
				try {
					new NetAssistant(NetAssistant.argsArrayToProps(1, params)).receive(-1, new NetAssistant.Visitor<Message>() {

						public boolean visit(Message m) {
							System.out.printf("Message: %s%n", m);
							return false;
						}
					});
				} catch (IOException e) {				
					e.printStackTrace();
				}
				
				break;
			case 's':
				NetAssistant.testSend(1, params);
				break;
			}
		}
	}
}
