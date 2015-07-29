/*
 * The IRCUser object simply saves the basic information associated with a user. Built by Matt Warman (mkwarman)
 * 
 * Date: 07/29/2015
 * Version: 1.0
 * 
 * Changes
 */

import java.util.Date;

public class IRCUser {
	 protected String nick; // Store the nick of the user the action is associated with
	 protected String hostmask; // Store the hostmask of the user the action is associated with
	 protected Date lastSeen; // Store the time and date that the user was last active
	 protected boolean online; // keep track of whether the user is online or not

	 IRCUser()
	 {
		 nick = "null";
		 hostmask = "null";
		 lastSeen = new java.util.Date();
		 online = true;
	 }
	 
	 IRCUser(String inputNick, String inputHostmask)
	 {
		 nick = inputNick;
		 hostmask = inputHostmask;
		 lastSeen = new java.util.Date();
		 online = true;
	 }
	 
	 String getNick()
	 {
		 return nick;
	 }
	 
	 void setNick(String newNick)
	 {
		 nick = newNick;
	 }
	 
	 String getHostmask()
	 {
		 return hostmask;
	 }
	 
	 void setHostmask(String newHostmask)
	 {
		 hostmask = newHostmask;
	 }
	 
	 Date getLastSeen()
	 {
		 return lastSeen;
	 }
	 
	 void setLastSeen ()
	 {
		 lastSeen = new java.util.Date();
	 }
	 
	 boolean getOnline()
	 {
		 return online;
	 }
	 
	 void setOnline(boolean newStatus)
	 {
		 online = newStatus;
		 lastSeen = new java.util.Date();
	 }
}
