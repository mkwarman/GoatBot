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
 * 
 * 0.4
 * Added ability to switch between testing and production mode
 * Improved array clearing after dealing with a penalized sender
 * Added unknown command reply
 * 
 * 0.5
 * Bug fixes
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.*;

public class GoatBot extends PircBot {
	
	public static final int MAX_MESSAGE_HISTORY = 10; // Sets max size of the message history array
	public static final int FLOOD_MESSAGE_FLOOR = 7; // Sets the number of messages from the user in the history to be considered a flood
	public static final int FLOOD_TIME_LIMIT = 6; // Set the amount of time between the FLOOD_MESSAGE_FLOOR number of messages to be declared a flood
	public static final int WARNINGS_BEFORE_KICK = 1; // Set how many times a user is warned before being kicked
	public static final int KICKS_BEFORE_BAN = 1; // Set how many times a user is kicked before being banned
	public static final int FIRST_BAN_TIME = 150; // Set how many seconds the first ban should last
	public static final int BAN_MULTIPLIER = 2; // Set the time multiplier for additional bans
	public static final int BAN_CHECK_INTERVAL = 1; // Set the interval in minutes for unban checks
	public static final int COOL_DOWN_TIME = 5; // The amount of time to wait for messages to clear before issuing another check or punishment
	public boolean testingMode = true;
	public boolean commandUnderstood = false;
	public Date time;
	public Date coolDownTimeStart = new Date(0);
	
	static List<Message> messageHistory = new ArrayList<Message>(); // Initialize an array list of messages
	static List<Action> actions = new ArrayList<Action>(); // Keep track of actions taken
	Message nullMessage;
	
	// Instantiate the bot
	public GoatBot() {
		this.setName("GoatBot");
	
		// Start a timer for ban expiration checks
		Timer timer = new Timer();
		timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				banCheck(); // Run the check for expired bans
			}
		}, 0, 1000 * 60 * BAN_CHECK_INTERVAL); // Every interval amount

		// Prepare a null message to fill the database on startup
		nullMessage = new Message("null", "null", "null", new java.util.Date());
		
		// Prepare database
		for (int index = 0; index <= MAX_MESSAGE_HISTORY; index++)
		{
			// Load the database with null values to start with
			messageHistory.add(nullMessage);
		}
	}
	
	// When someone says something
	public void onMessage(String channel, String sender, String login, String hostname, String message)
	{
		// Record the time
		time = new java.util.Date();
		
		if (time.getTime() <= (coolDownTimeStart.getTime() + (COOL_DOWN_TIME * 1000)))
		{
			return;
		}
		
		// If someone asked for the time
		if (message.equalsIgnoreCase("!time"))
		{
			// Tell them the time
			sendMessage(channel, sender + ": The time is now " + time.toString());
			commandUnderstood = true;
		}
		
		// If someone askes about goatbots status
		if (message.equalsIgnoreCase("goatbot, status"))
		{
			// Say you're alive. If you don't, you're probably not alive
			sendMessage(channel, sender + ": GoatBot is alive");
			commandUnderstood = true;
		}
		
		// If an OP of chatbotcontrol wishes to enable testing mode
		if (message.equalsIgnoreCase("goatbot, enable testing mode") && isOp(sender, "#chatbotcontrol"))
		{
			// Enable testing mode
			testingMode = true;
			sendMessage(channel, "Ok " + sender + ", enabled testing mode");
			commandUnderstood = true;
		}
		
		// If a nonOP trys to enable testing mode
		else if (message.equalsIgnoreCase("goatbot, enable testing mode"))
		{
			sendMessage(channel, "Nice try, but you don't have the necessary authentication, " + sender);
			commandUnderstood = true;
		}
		
		// If an OP of chatbotcontrol wishes to disable testing mode
		if (message.equalsIgnoreCase("goatbot, disable testing mode") && isOp(sender, "#chatbotcontrol"))
		{
			// Disable testing mode
			testingMode = false;
			sendMessage(channel, "Ok " + sender + ", disabled testing mode");
			commandUnderstood = true;
		} 
		
		// If a nonOP trys to enable testing mode
		else if (message.equalsIgnoreCase("goatbot, disable testing mode"))
		{
			sendMessage(channel, "Nice try, but you don't have the necessary authentication, " + sender);
			commandUnderstood = true;
		}
		
		// Reply command unknown when appropriate
		if (!commandUnderstood && (message.toLowerCase().startsWith("goatbot, ") || message.toLowerCase().startsWith("goatbot: ")))
		{
			sendMessage(channel, "Sorry " + sender + ", I don't understand that command");
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
				System.out.println("Floodcheck returned true");
				return true;
			}
		}
		return false; // No flood detected
	}
	
	// In the case of a flood, check if the sender has a history, and react accordingly
	public void floodProtect(String channel, String sender, String hostmask)
	{
		boolean repeatOffender = false;	// Innocent till proven guilty	
		int userIndex = 0; // Keep track of the user's location in the arraylist

		// Scan through the actions arraylist, checking to see of the sender was previously warned, kicked, banned, etc		
		for (int i = 0; i < actions.size(); i++)
		{
			if (actions.get(i).getNick().equals(sender) && actions.get(i).getChannel().equals(channel))
			{
				repeatOffender = true; // Sender is a repeat offender
				break; // Stop scanning if the user is found
			}
		}
		
		// If repeatOffender was found
		if (repeatOffender)
		{
			// If the sender has used up all their kicks
			if (actions.get(userIndex).getKicks() >= KICKS_BEFORE_BAN)
			{
				// If this is the sender's first ban
				if (actions.get(userIndex).getBans() == 0)
				{
					// Kickban the user for the default first ban amount of time
					if (testingMode)
					{
						kickBanTest(channel, sender, hostmask, FIRST_BAN_TIME);
					}
					else
					{
						kickBan(channel, sender, hostmask, FIRST_BAN_TIME);
					}
					actions.get(userIndex).addBan(time, FIRST_BAN_TIME);
				}
				
				// If the sender has been banned before than once before
				else
				{
					// Ban the user again, with a ban time based on the BAN_MULTIPLIER
					if (testingMode)
					{
						kickBanTest(channel, sender, hostmask, actions.get(userIndex).getLastBanLength() * BAN_MULTIPLIER);
					}
					else
					{
						kickBan(channel, sender, hostmask, actions.get(userIndex).getLastBanLength() * BAN_MULTIPLIER);
					}
					actions.get(userIndex).addBan(time, actions.get(userIndex).getLastBanLength() * BAN_MULTIPLIER);
				}
			}
			
			// If the sender has used up all their warnings
			else if (actions.get(userIndex).getWarnings() >= WARNINGS_BEFORE_KICK)
			{
				// Kick them
				if (testingMode)
				{
					kickTest(channel, sender);
				}
				else
				{
					kick(channel, sender, "Kicked by the Goat!");
				}
				actions.get(userIndex).addKick(); // Add that the sender was kicked
				coolDownTimeStart = time; // Start cooldown timer
			}
			
			else // If the user has NOT used up their warnings
			{
				// Warn the user
				sendMessage(channel, "Warning, flood detected from user: " + sender);
				actions.get(userIndex).addWarning(); // Add that a warning was given to the actions arraylist
				coolDownTimeStart = time; // Start cooldown timer
			}
		}
		
		else // If the user is NOT a repeat offender
		{
			actions.add(new Action(channel, sender, hostmask)); // Add the user to the actions arraylist
			actions.get(userIndex).addWarning(); // Add that a warning was given to the actions arraylist
			coolDownTimeStart = time; // Start cooldown timer

			// Issue them a warning
			sendMessage(channel, sender + ", please be aware that spamming is not allowed in this channel");

		}

		// Prepare a null message to remove user messages from the messageHistory arraylist
		nullMessage = new Message("null", "null", "null", new java.util.Date());
		
		
		// Remove sender's messages from the history
		for (int i = 0; i < MAX_MESSAGE_HISTORY; i++)
		{
			// If the message sender and channel match...			
			if (messageHistory.get(i).getNick().equals(sender) && messageHistory.get(i).getChannel().equals(channel))
			{
				messageHistory.set(i, nullMessage); // Reset the entry
			}
			
		}
		// messageHistory.clear(); // clear list
	}
	
	// Determine if a user is an OP
	public boolean isOp(String nickname, String channel)
	{
		
	    String status = ""; // Initialize status
	    int index;
	    User userList[] = getUsers(channel); // Get users in the channel

	    for(index = 0; index < userList.length; index++) // Search through the user list
	    {
	    	System.out.println(userList[index].toString());
	    	// If the username is found in the user list...
	    	if( userList[index].toString().contains(nickname))
	        {
	    		// Get the user's prefix
	            status = userList[index].toString();
	            break; // Stop searching
	        }
	    }
	    
	    // If the prefix is an OP prefix
	    if (status.startsWith("@") || status.startsWith("~"))
	    {
	    	return true; // Return true
	    }
	    else // if not
	    {
	    	return false; // Return false
	    }
	}
	
	// Since GoatBot is in testing, we dont want to actually kick someone, just show when we WOULD have.
	public void kickTest(String channel, String sender)
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
				unBan(actions.get(index).getChannel(), actions.get(index).getHostmask());
				sendMessage(actions.get(index).getChannel(), Colors.BOLD + Colors.BLUE + "Unbanning " + actions.get(index).getNick() + " from channel " + actions.get(index).getChannel() + " with hostmask " + actions.get(index).getHostmask());
				actions.get(index).removeBan(); // Remove the banned status in the Action object
				
				// Provide console feedback
				System.out.print("Unbanned user " + actions.get(index).getNick() + " from channel " + actions.get(index).getChannel());
			}
		}
	}
	
}