/*
 * Built by Matt Warman (mkwarman) to protect IRC channels
 * 
 * Date: 07/28/2015
 * Version: 0.2
 * 
 * Changes
 * 0.2
 * Added banning functionality
 * 
 * 0.3
 * Added ban expiration
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.*;

public class GoatBot extends PircBot {
	
	public static final int MAX_MESSAGE_HISTORY = 15; // Sets max size of the message history array
	public static final int FLOOD_MESSAGE_FLOOR = 10; // Sets the number of messages in a row to be considered a flood
	public static final int FLOOD_TIME_LIMIT = 8; // Set the amount of time between the FLOOD_MESSAGE_FLOOR number of messages to be declared a flood
	public static final int WARNINGS_BEFORE_KICK = 1; // Set how many times a user is warned before being kicked
	public static final int KICKS_BEFORE_BAN = 1; // Set how many times a user is kicked before being banned
	public static final int FIRST_BAN_TIME = 150; // Set how many seconds the first ban should last
	public static final int BAN_MULTIPLIER = 2; // Set the time multiplier for additional bans
	public static final int BAN_CHECK_INTERVAL = 1; // Set the interval in minutes for unban checks
	public Date time;
	
	static List<Message> messageHistory = new ArrayList<Message>(); // Initialize an array list of messages
	static List<Action> actions = new ArrayList<Action>(); // Keep track of actions taken
	

	public GoatBot() {
		this.setName("GoatBot");
	
		Timer timer = new Timer();
		timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				banCheck();
			}
		}, 0, 1000 * 60 * BAN_CHECK_INTERVAL);
	}
	

	
	
	// When someone says something
	public void onMessage(String channel, String sender, String login, String hostname, String message)
	{
		// Record the time
		time = new java.util.Date();
		
		// If someone asked for the time
		if (message.equalsIgnoreCase("!time"))
		{
			// Tell them the time
			sendMessage(channel, sender + ": The time is now " + time.toString());
		}
		
		// If someone askes about goatbots status
		if (message.equalsIgnoreCase("goatbot, status"))
		{
			// Say you're alive. If you don't, you're probably not alive
			sendMessage(channel, sender + ": GoatBot is alive");
		}
		
		// Record the message's sender, content, the channel it was sent to, and the time it was sent
		messageHistory.add(new Message(sender, message, channel, time));
		
		// Limit the number of messages to save in the history, must be greater than flood floor
		if (messageHistory.size() > MAX_MESSAGE_HISTORY)
		{
			// If the history is greater than the max, remove the oldest element
			messageHistory.remove(0);
		}
		
		// Check for floods
		if (floodCheck(channel, sender))
		{
			// If a flood is detected, run floodProtect
			floodProtect(channel, sender, hostname);
		}
	}
	
	// Check for floods
	public boolean floodCheck(String channel, String sender)
	{
		// Keep track of repeated occurrences
		int repeatedOccurrence = 0;
		
		// Scan through the message history
		for (int i = 0; i < messageHistory.size(); i++)
		{
			// Count how many times the same user has posted a message in the history
			if (messageHistory.get(i).getNick().equals(sender))
			{
				repeatedOccurrence++; // Record the number in repeatedOccurrence
			}
		}
		
		// If the author has posted more than the floor allowance, check how quickly they posted
		if (repeatedOccurrence >= FLOOD_MESSAGE_FLOOR)
		{
			// Check the time between the current message and the oldest one
			if((messageHistory.get(MAX_MESSAGE_HISTORY-1).getTimestamp().getTime() - messageHistory.get(0).getTimestamp().getTime()) < (FLOOD_TIME_LIMIT*1000))
			{
				// If the messages were posted quickly enough to be considered a flood, return true
				return true;
			}
		}
		return false; // No flood detected
	}
	
	// In the case of a flood, check if the sender has a history, and react accordingly
	public void floodProtect(String channel, String sender, String hostmask)
	{
		boolean repeatOffender = false;	// Innocent till proven guilty	
		int index = 0; // Keep track of the user's location in the arraylist

		// Scan through the actions arraylist, checking to see of the sender was previously warned, kicked, banned, etc		
		for (index = 0; index < actions.size(); index++)
		{
			if (actions.get(index).getNick().equals(sender) && actions.get(index).getChannel().equals(channel))
			{
				repeatOffender = true; // Sender is a repeat offender
				break; // Stop scanning if the user is found
			}
		}
		
		// If repeatOffender was found
		if (repeatOffender)
		{
			// If the sender has used up all their kicks
			if (actions.get(index).getKicks() >= KICKS_BEFORE_BAN)
			{
				// If this is the sender's first ban
				if (actions.get(index).getBans() == 0)
				{
					// Kickban the user for the default first ban amount of time
					kickBanTest(channel, sender, hostmask, FIRST_BAN_TIME);
					actions.get(index).addBan(time, FIRST_BAN_TIME);
				}
				
				// If the sender has been banned before than once before
				else
				{
					// Ban the user again, with a ban time based on the BAN_MULTIPLIER
					kickBanTest(channel, sender, hostmask, actions.get(index).getLastBanLength() * BAN_MULTIPLIER);
					actions.get(index).addBan(time, actions.get(index).getLastBanLength() * BAN_MULTIPLIER);
				}
			}
			
			// If the sender has used up all their warnings
			else if (actions.get(index).getWarnings() >= WARNINGS_BEFORE_KICK)
			{
				// Kick them
				kickTest(channel, sender, "Kicked by the Goat!");
				actions.get(index).addKick(); // Add that the sender was kicked
			}
			
			else // If the user has NOT used up their warnings
			{
				// Warn the user
				sendMessage(channel, "Warning, flood detected from user: " + sender);
				actions.get(index).addWarning(); // Add that a warning was given to the actions arraylist
			}
		}
		
		else // If the user is NOT a repeat offender
		{
			actions.add(new Action(channel, sender, hostmask)); // Add the user to the actions arraylist
			actions.get(index).addWarning(); // Add that a warning was given to the actions arraylist

			// Issue them a warning
			sendMessage(channel, sender + ", please be aware that spamming is not allowed in this channel");

		}
		
		messageHistory.clear(); // Clear the message history since an action took place
	}
	
	// Since GoatBot is in testing, we dont want to actually kick someone, just show when we WOULD have.
	public void kickTest(String channel, String sender, String message)
	{
		// Report when a kick would have taken place
		sendMessage(channel, Colors.BOLD + Colors.RED + "This would hav resulted in a /kick of user " + sender + ", but didn't as GoatBot is still being tested");
		sendMessage(channel, Colors.BOLD + Colors.RED + "If this would have been unwarranted, please tell mkwarman.");
	}
	
	// Simple function for banning then kicking a user for a set amount of time
	public void kickBan(String channel, String sender, String hostname, int seconds)
	{
		sendMessage(channel, "Banning " + sender + " for " + seconds + " seconds.");
		ban(channel, hostname);
		kick(channel, sender);
	}
	
	// Test banning calls
	public void kickBanTest(String channel, String sender, String hostmask, int seconds)
	{
		sendMessage(channel, Colors.BOLD + Colors.RED + "Banning " + sender + " for " + seconds + " seconds.");
		sendMessage(channel, Colors.BOLD + Colors.RED + "But not really because I'm still being tested.");
	}
	
	// Test unbanning calls
	public void unBanTest(String channel, String sender, String hostmask)
	{
		sendMessage(channel, Colors.BOLD + Colors.BLUE + "Unbanning " + sender + " from channel " + channel + " with hostmask " + hostmask);
		sendMessage(channel, Colors.BOLD + Colors.BLUE + "But not really because I'm still being tested.");
	}
	
	// Check if there are any users to be unbanned
	public void banCheck()
	{
		System.out.print("Checking for expired bans");
		int index; // help cycle through the arraylist
		for (index = 0; index < actions.size(); index++) // cycle through the entire arraylist
		{
			// If an entry exists that is banned but the ban has expired...
			if (actions.get(index).getBannedStatus() && actions.get(index).getBanExpired())
			{
				// Unban the user
				//unBan(actions.get(index).getChannel(), actions.get(index).getHostmask());
				unBanTest(actions.get(index).getChannel(), actions.get(index).getNick(), actions.get(index).getHostmask());
				actions.get(index).removeBan(); // Remove the banned status in the Action object
				
				// Provide console feedback
				System.out.print("Unbanned user " + actions.get(index).getNick() + " from channel " + actions.get(index).getChannel());
			}
		}
	}
	
}
