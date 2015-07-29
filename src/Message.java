/*
 * Message object for GoatBot, used to store temporary message histories. Built by Matt Warman (mkwarman)
 * 
 * Date: 07/28/2015
 * Version: 1.0
 * 
 * Changes
 * 1.0
 * Made Message and extension of the new IRCUser object
 */

import java.util.Date;

public class Message extends IRCUser {
	private String message; // hold message
	private String channel; // hold message channel
	private Date timestamp; // hold time

	Message(String inputNick, String inputMessage, String inputChannel, Date inputDate){
		nick = inputNick;
		message = inputMessage;
		channel = inputChannel;
		timestamp = inputDate;
		setLastSeen();
	}
	
	String getMessage() 
	{
		return message;
	}
	
	String getChannel()
	{
		return channel;
	}
	
	Date getTimestamp()
	{
		return timestamp;
	}
}
