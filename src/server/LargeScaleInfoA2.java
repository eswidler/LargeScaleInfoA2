package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class LargeScaleInfoA2 extends HttpServlet {
	//Servlet metadata
	private static final long serialVersionUID = 1L;

	//Cookie expiration duration, in minutes
	private static final int cookieDuration = 1;

	private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


	//Hashtable of sessionIDs to a table of information on their message, location, and expiration data.
	Hashtable<String,Hashtable<String,String>> sessionTable = new Hashtable<String,Hashtable<String,String>>();

	//Cookie name that is searched for in this project
	String a2CookieName = "CS5300PROJ1SESSION";
	GarbageCollector janitorThread = new GarbageCollector("name");

	/*
	 * Base method handling requests
	 */
	@Override
	public void doGet(HttpServletRequest request,HttpServletResponse response)	throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		String sessionID = handleCookie(request, response);
		handleCommand(request, out, sessionID);

		out.println("<html>\n<body>\n<br>&nbsp;<br>");

		out.println(getMessage(sessionID));
		out.println(getForm());
		out.println(getSessionLoc(sessionID));
		//		out.println(getVersionNumber(sessionID));
		//		out.println(getSessionID(sessionID));
		out.println(getSessionExp(sessionID));

		out.println("</body>\n</html>");
	}

	/*
	 * Examines cookies from the request to either extract or generate a new sessionID.  Additionally attaches a cookie to the response.
	 */
	private String handleCookie(HttpServletRequest request, HttpServletResponse response) {
		String sessionID = "-1";
		Cookie a2Cookie = null;

		//Check if there is a relevant cookie and extract sessionID
		if(request.getCookies() != null){
			System.out.println("old cookie");
			for(Cookie c : request.getCookies()){
				Hashtable<String,String> parsed= parseCookieValue(c.getValue());
				System.out.println(c.getValue());
				if(c.getName().equals(a2CookieName) && sessionTable.containsKey(c.getValue())){
					a2Cookie = c;
					sessionID = parsed.get("sessionID");
					System.out.println("SessionID: " + sessionID);				}
			}
		}
		//If no cookie was found, generate a new one 
		if(a2Cookie == null){
			System.out.println(" -- new cookie --");
			sessionID = getNextSessionID();

			// create new timestamp
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, cookieDuration);

			Hashtable<String, String> sessionValues = new Hashtable<String, String>();
			sessionValues.put("version", 1 +"");
			sessionValues.put("message", "");
			sessionValues.put("expiration-timestamp", df.format(cal.getTime()));
			try {
				String ip= InetAddress.getLocalHost().getHostAddress() + ":" + request.getLocalPort();
				sessionValues.put("location", InetAddress.getLocalHost().toString());
			} catch (UnknownHostException e) {
				sessionValues.put("location", "Unknown host");
			}
			sessionTable.put(sessionID + "", sessionValues);
			String cookieVal = "sessionID="+sessionID+";";
			// TODO check here
			Hashtable<String,String> parsed= parseCookieValue(cookieVal);
			a2Cookie = new Cookie(a2CookieName, parsed.get("sessionID"));
			response.addCookie(a2Cookie);
		}

		//Add cookie to response regardless, as it always contains new expiration and version information
		response.addCookie(a2Cookie);
		System.out.println("cookie val: " + a2Cookie.getValue());
		return sessionID;
	}

	/*
	 * Determines the next available sessionID for use
	 */
	private String getNextSessionID(){
		int sessionID = 1;

		while(true){
			if(!sessionTable.containsKey(sessionID + "")){
				break;
			}
			sessionID++;
		}

		return sessionID + "";
	}

	/*
	 * Examines the request for the 'cmd' value, and performs the pertinent action
	 */
	private void handleCommand(HttpServletRequest request, PrintWriter out, String sessionID){
		String cmd = request.getParameter("cmd");
		String message = request.getParameter("NewText");

		//Don't do anything if no command was provided
		if(cmd == null){
			return;
		} 
		//Destroy relevant session 
		else if(cmd.equals("LogOut")){
			System.out.println("LogOut command");
			sessionTable.remove(sessionID);
			out.write("<html>\n<body>\n<br>&nbsp;\n<br><big><big><b>Bye!<br>&nbsp;<br>\n</b></big></big>\n</body>\n</html>");
			out.close();
		} else if(cmd.equals("Replace") && message.length() > 512) {
			System.out.println("Message is greater than 512 bytes. Request ignored and no cookie made");
			return;
		}
		else{
			String cookieVal="sessionID="+sessionID+",";

			//update version number
			if ((sessionID != null) && sessionTable.contains(sessionID)) {
				int oldVersion = Integer.parseInt(sessionTable.get(sessionID).get("version"));
				String newVersion = ((Integer)(oldVersion + 1)).toString();
				sessionTable.get(sessionID).put("version", newVersion);
				cookieVal += "version=" +newVersion + ",";

				//update expiration timestamp
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MINUTE, cookieDuration);	
				sessionTable.get(sessionID).put("expiration-timestamp", df.format(cal.getTime()));

				//Update message for session
				if(cmd.equals("Replace")){
					System.out.println("Replace command");
					System.out.println("String length: " + message.length());
					sessionTable.get(sessionID).put("message", message);
					cookieVal+="message="+message+",";
				}

				//Update relevant session's expiration 
				else if(cmd.equals("Refresh")){
					System.out.println("Refresh command");
					cookieVal+="message="+""+",";
				} 
			}
		}
	}

	/*
	 * Generates the html input form for the page
	 */
	private String getForm() {
		String out = "<form method=GET action=''>";
		out += "<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n";
		out += "</form>\n";
		out += "<form method=GET action=''>\n";
		out += "<input type=submit name=cmd value=Refresh>\n";
		out += "</form>\n";
		out += "<form method=GET action=''>\n";
		out += "<input type=submit name=cmd value=LogOut>\n";
		out += "</form>\n";

		return out;
	}

	/*
	 * Creates the html with the session's relevant message or a default greeting if none is provided
	 */
	private String getMessage(String sessionID) {
		String out = "<big><big><b>";

		if((sessionID != null) && sessionTable.containsKey(sessionID)){
			//Check if there is anything but the default blank message
			if(sessionTable.get(sessionID).get("message") != ""){				
				out += sessionTable.get(sessionID).get("message");
			} else{
				out += "Hello, User!";
			}

		} else{
			out += "Issue with cookies";
		}

		out += "<br>&nbsp;<br></b></big></big>";

		return out;
	}

	/*
	 * Creates the html displaying the hashed location of the WQ server storing this session's data
	 */
	private String getSessionLoc(String sessionID) {
		String out = "<p>Session on ";

		if((sessionID != null) && sessionTable.containsKey(sessionID)){
			out += sessionTable.get(sessionID).get("location");
		} else{
			out += "Issue with cookies";
		}

		out += "</p>";

		return out;
	}

	/*
	 * Creates the html displaying the hashed expiration timestamp for the session
	 */
	private String getSessionExp(String sessionID) {
		String out = "<p>Expires ";

		if((sessionID != null) && sessionTable.containsKey(sessionID)){
			out += sessionTable.get(sessionID).get("expiration-timestamp");			
			String expirationDate = sessionTable.get(sessionID).get("expiration-timestamp");
			Date expDate = null;
			Date currentDate = new Date();
			try {
				expDate = df.parse(expirationDate);
			} catch (ParseException e) {
				System.out.println("Cookie Date Parse Error");
			}

			if (expDate.after(currentDate)) {
				long ms = expDate.getTime() - currentDate.getTime();
				Double minutes = (double)(ms/(1000.0 * 60.0));
				out += String.format(", %.2f minutes from now.", minutes);
			} 

		} else{
			out += "Issue with cookies";
		}

		out += "</p>";

		return out;
	}

	/*
	 * puts version number on html page
	 * for debugging purposes
	 */
	private String getVersionNumber(String sessionID) {
		String out = "<p>Version Number: ";

		if((sessionID != null) && sessionTable.containsKey(sessionID)){
			out += sessionTable.get(sessionID).get("version");
		} else{
			out += "Issue with cookies";
		}

		out += "</p>";

		return out;
	}

	/*
	 * print sessionID on HTML page
	 * for debugging purposes
	 */
	private String getSessionID(String sessionID) {
		String out = "<p>SessionID: ";

		if((sessionID != null) && sessionTable.containsKey(sessionID)){
			out += sessionID;
		} else{
			out += "Issue with cookies";
		}

		out += "</p>";

		return out;
	}	

	/*cookieVal is the string used as the value of a Cookie
	 *Each information of the cookie is in the following format: 'key=value,'
	 *parseCookieValue parses the string into a Hashtable
	 */
	private Hashtable<String,String> parseCookieValue(String cookieVal){
		Hashtable<String,String> parsed= new Hashtable<String,String>();
		String[] semicolonParsed = cookieVal.split(";");
		for (String s: semicolonParsed){
			String[] kv = s.split("=");
			if (kv.length == 2){
				parsed.put(kv[0], kv[1]);
			}
			else{
				parsed.put(kv[0], "");
			}
		}
		return parsed;
	}

	/*
	 * background thread that cleans up expired session from sessionTable
	 */
	private class GarbageCollector extends Thread {
		GarbageCollector(String name){
			super(name);
			start();
		}

		public void run(){
			while(true){
				for (String sessionID: sessionTable.keySet()){
					Hashtable<String,String> session = sessionTable.get(sessionID);
					String exprString= session.get("expiration-timestamp");

					Date expDate = null;
					try {
						expDate = df.parse(exprString);
					} catch (ParseException e) {
						System.out.println("Failure in parsing date");
					}
					if ((new Date()).after(expDate)){
						System.out.println("Session " + sessionID + " has expired");
						sessionTable.remove(sessionID);
						System.out.println("sessiontable size: "+ sessionTable.size());
					}
					else{
	//					System.out.println("Session #"+sessionID + " not expired");
					}

				}
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
