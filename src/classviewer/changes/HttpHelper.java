package classviewer.changes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpHelper {
	public static SimpleDateFormat dformats[] = {
		new SimpleDateFormat("yyyy-MM-dd"),
		new SimpleDateFormat("dd MMM yyyy"),
		new SimpleDateFormat("MMM dd yyyy"),
		new SimpleDateFormat("MMMMM yyyy"),
	};
	
	/** Read everything into a string buffer */
	public static StringBuffer readIntoBuffer(Reader reader) throws IOException {
		StringBuffer b = new StringBuffer();
		BufferedReader br = new BufferedReader(reader);
		String s = br.readLine();
		while (s != null) {
			b.append(s + "\n"); // keep /n there for easier debugging
			s = br.readLine();
		}
		return b;
	}

	// Borrowed from https://code.google.com/p/misc-utils/wiki/JavaHttpsUrl
	public static SSLSocketFactory makeAllTrustingManager() {
		// Create a trust manager that does not validate certificate chains
		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public void checkClientTrusted(final X509Certificate[] chain,
					final String authType) {
			}

			@Override
			public void checkServerTrusted(final X509Certificate[] chain,
					final String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts,
					new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			return sslContext.getSocketFactory();
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isSelfPaced(String str) {
		if (str == null)
			return false;
		if (str.toLowerCase().contains("self")) {
			return true;
		}
		return false;
	}
	
	public static Date parseDate(String str) {
		if (str == null || isSelfPaced(str))
			return null;
		str = str.replace(",", "");
		str = str.replace("Sept ", "Sep "); // More of these?
		
		for (SimpleDateFormat format : dformats) {
			try {
				return format.parse(str);
			} catch (Exception e) {
			}
		}
		try {
			return parseQuarter(str);
		} catch (Exception e) {
			System.err.println("Cannot parse date " + str);
		}
		return null;
	}

	private static Date parseQuarter(String str) throws ParseException {
		String[] parts = str.trim().split(" ");
		if (parts.length != 2)
			throw new ParseException("Need 2 parts for quater", 0);
		if (parts[0].toUpperCase().charAt(0) != 'Q')
			throw new ParseException("Quater should start with Q", 0);
		int quater = Integer.parseInt(parts[0].substring(1));
		if (quater < 1 || quater > 4)
			throw new ParseException("Quater should be between 1 and 4", 1);
		int year = Integer.parseInt(parts[1]);
			
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, year);
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.MONTH, 3*(quater-1));
		
		return c.getTime();
	}
}
