/*
 * Action object for GoatBot, used to keep track of actions taken against users. Built by Matt Warman (mkwarman)
 * 
 * Date: 07/28/2015
 * Version: 0.2
 * 
 * Changes:
 * 0.2
 * Improved addBan - now tracks ban time and length
 * Added ban status, changed currently banned to ban expired
 */

import java.util.Date;

public class Action {
	 private String nick; // Store the nick of the user the action is associated with
	 private String channel; // Store the channel the action was made in
	 private int warnings = 0; // Number of warnings given to the user
	 private int kicks = 0; // Number of times the user was kicked
	 private int bans = 0; // Number of times the user has been banned
	 private boolean banStatus = false; // Keep track of whether a user is banned or not
	 private int lastBanLength = 0; // Length of the last ban
	 private Date lastBanStart; // Date and time that the last ban started
	 
	 Action(String inputChannel, String inputNick) {
		 channel = inputChannel;
		 nick = inputNick;
	 }
	 
	 String getNick()
	 {
		 return nick;
	 }
	 
	 String getChannel()
	 {
		 return channel;
	 }
	 
	 int getWarnings()
	 {
		 return warnings;
	 }
	 
	 void addWarning()
	 {
		 warnings++;
	 }
	 
	 int getKicks()
	 {
		 return kicks;
	 }
	 
	 void addKick()
	 {
		 kicks++;
	 }
	 
	 int getBans()
	 {
		 return bans;
	 }
	 
	 void addBan(Date banStartTime, int banLength)
	 {
		 lastBanStart = banStartTime;
		 lastBanLength = banLength;
		 bans++;
	 }
	 
	 boolean getBannedStatus()
	 {
		 return banStatus;
	 }
	 
	 boolean getBanExpired()
	 {
		 /*
		  * Check if the last ban time (in milliseconds since the epoch date) plus its length in milliseconds is 
		  * 	greater or less than the current date and time in milliseconds.
		  * 
		  * If it is greater than the current epoch time, then the ban has not been lifted yet.
		  * If it is less than the current epoch time, then the ban has been lifted.
		  */
		 return (new Date().getTime() > (lastBanStart.getTime() + (lastBanLength * 1000)));
	 }
	 
	 int getLastBanLength()
	 {
		 return lastBanLength;
	 }
	 
	 Date getLastBanStart()
	 {
		 return lastBanStart;
	 }
}
