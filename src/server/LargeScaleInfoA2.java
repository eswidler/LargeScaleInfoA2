package server;

import java.io.*; 
import java.util.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

@WebServlet("/")
public class LargeScaleInfoA2 extends HttpServlet {
	//Servlet metadata
	private static final long serialVersionUID = 1L;
	
	//Hashtable of sessionIDs to a table of information on their message, location, and expiration data.
	Hashtable<String,Hashtable<String,String>> sessionTable = new Hashtable<String,Hashtable<String,String>>();
	
	//Cookie name that is searched for in this project
	String a2CookieName = "CS5300PROJ1SESSION";

	/*
	 * Base method handling requests
	 * 
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
			for(Cookie c : request.getCookies()){
				if(c.getName().equals(a2CookieName) && sessionTable.containsKey(c.getValue() + "")){
					a2Cookie = c;
					sessionID = c.getValue();
				}
			}
		}
		//If no cookie was found, generate a new one 
		if(a2Cookie == null){
			sessionID = getNextSessionID();
			a2Cookie = new Cookie(a2CookieName, sessionID + "");
			
			Hashtable<String, String> sessionValues = new Hashtable<String, String>();
			sessionValues.put("version", 1 +"");
			sessionValues.put("message", "");
			sessionValues.put("expiration-timestamp", "test timestamp");
			try {
				sessionValues.put("location", InetAddress.getLocalHost().toString());
			} catch (UnknownHostException e) {
				sessionValues.put("location", "Unknown host");
			}
			sessionTable.put(sessionID + "", sessionValues);
		}
		
		//Add cookie to response regardless, as it always contains new expiration and version information
		response.addCookie(a2Cookie);
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
	private void handleCommand(HttpServletRequest request, PrintWriter out, String sessionID) {
		String cmd = request.getParameter("cmd");
		
		//Don't do anything if no command was provided
		if(cmd == null){
			return;
		} 
		//Update message for session
		else if(cmd.equals("Replace")){
			System.out.println("Replace command");
			String message = request.getParameter("NewText");
			sessionTable.get(sessionID).put("message", message);
		}
		//Update relevant session's expiration 
		else if(cmd.equals("Refresh")){
			System.out.println("Refresh command");
			
		} 
		//Destroy relevant session 
		else if(cmd.equals("LogOut")){
			System.out.println("LogOut command");
			sessionTable.remove(sessionID);
			out.write("<html>\n<body>\n<br>&nbsp;\n<br><big><big><b>Bye!<br>&nbsp;<br>\n</b></big></big>\n</body>\n</html>");
			out.close();
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
		
		if(sessionTable.containsKey(sessionID + "")){
			if(sessionTable.get(sessionID + "").get("message") != ""){
				out += sessionTable.get(sessionID + "").get("message");
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
		
		if(sessionTable.containsKey(sessionID + "")){
			out += sessionTable.get(sessionID + "").get("location");
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
		
		if(sessionTable.containsKey(sessionID + "")){
			out += sessionTable.get(sessionID + "").get("expiration-timestamp");
		} else{
			out += "Issue with cookies";
		}
		
		out += "</p>";
		
		return out;
	}
}
