import java.util.Scanner;

/*
 * Config file for GoatBot. Built by Matt Warman (mkwarman)
 * 
 * Date: 07/28/2015
 * Version: 0.2
 * 
 * Changes
 * 0.2
 * Implemented input password on startup
 */

public class GoatBotMain {
	
	public static void main(String args[]) throws Exception {
		// Start GoatBot
		GoatBot bot = new GoatBot();
		
		// Enable debugging
		bot.setVerbose(true);
		
		System.out.print("Please input the nickserv password: ");
		Scanner inputPassword = new Scanner(System.in); // initialize courseScore input
		String password = inputPassword.next();
		
		//Connect to server
		bot.connect("irc.veuwer.com");
		
		// Join chatbotcontrol
		bot.joinChannel("#chatbotcontrol");
		// Join chatbox
		bot.joinChannel("#chatbox");
		
		//Identify with NickServ
		bot.sendMessage("NickServ", "identify " + password);
		inputPassword.close(); // clean up after the password input
		
		// Allow the bot to talk a little faster
		bot.setMessageDelay(100);
		
		
	}
	
}
