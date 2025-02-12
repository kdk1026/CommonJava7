package common.util.push;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.util.date.JodaTimeDateUtil;
import common.util.file.NioFileUtil;
import common.util.properties.PropertiesUtil;
import javapns.devices.Device;
import javapns.devices.implementations.basic.BasicDevice;
import javapns.notification.AppleNotificationServer;
import javapns.notification.AppleNotificationServerBasicImpl;
import javapns.notification.PushNotificationManager;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.PushedNotifications;

public class ApnsPushUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(ApnsPushUtil.class);

	private static List<String> regList;
	private static List<Map<String, Object>> rtnList;
	
	private ApnsPushUtil() {
		super();
	}
	
	private static final String APNS_PROP_CLASS_PATH = "/properties" + NioFileUtil.FOLDER_SEPARATOR + "apns" + NioFileUtil.FOLDER_SEPARATOR;
	private static final String APNS_PROPERTIES_PATH = "apns/apns.properties";
	private static final String APNS_CERTIFICATE_NAME = "APNSsslCertificateName";
	private static final String APNS_CERTIFICATE_PWD_NAME = "APNSsslCertificatePwd";
	
	public static PushNotificationManager pushManager;
	public static PushNotificationPayload payload;

	public static List<Map<String, Object>> sendPush(List<String> regIdList, String jsonStr) {

		try {
			Properties prop = PropertiesUtil.getPropertiesClasspath(APNS_PROPERTIES_PATH);
			
			if ( (prop != null) && (prop.containsKey(APNS_CERTIFICATE_NAME)) ) {
				String sPath = APNS_PROP_CLASS_PATH + prop.getProperty(APNS_CERTIFICATE_NAME);
				String sPwd = prop.getProperty(APNS_CERTIFICATE_PWD_NAME);
				
				InputStream is = ApnsPushUtil.class.getResourceAsStream(sPath);

				AppleNotificationServer server = new AppleNotificationServerBasicImpl(is, sPwd, true);
				pushManager = new PushNotificationManager();
				pushManager.initializeConnection(server);

				setMessage(jsonStr);
				regList = regIdList;
				sendNotifications();				
			} else {
				rtnList = new ArrayList<>();
			}

		} catch (Exception e) {
			logger.error("", e);
		}

		return rtnList;
	}

	private static void setMessage(String jsonStr) {
		payload = PushNotificationPayload.complex();

		try {
			payload.addAlert(jsonStr);
			payload.addBadge(1);

		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private static void sendNotifications() {
		rtnList = new ArrayList<>();
		Map<String, Object> rtnMap = null;

		List<Device> deviceList = new ArrayList<>(regList.size());

		for (String receiverId : regList) {
			Device device = new BasicDevice();
            device.setToken(receiverId);
            deviceList.add(device);
		}

		PushedNotifications notifications;
		try {
			notifications = pushManager.sendNotifications(payload, deviceList);

			if (notifications != null) {

				for (int i=0; i < notifications.size(); i++) {
					PushedNotification notification = notifications.get(i);

					int identifier = notification.getIdentifier();
					String sendDate = JodaTimeDateUtil.Today.getTodayString("yyyy-MM-dd HH:mm:ss");
					boolean isSuccess = false;

					rtnMap = new HashMap<>();
					rtnMap.put("regId", notification.getDevice().getToken());
					rtnMap.put("msgId", identifier);
					rtnMap.put("errMsg", notification.getException().getMessage());
					rtnMap.put("sendDt", sendDate);

					if (notification.isSuccessful()) {
						isSuccess = true;
					}
					rtnMap.put("resFlag", isSuccess);

					logger.info("[APNS PUSH] - {}", rtnMap);

					rtnList.add(rtnMap);
				}
			}

		} catch (Exception e) {
			logger.error("", e);
		}
	}

}
