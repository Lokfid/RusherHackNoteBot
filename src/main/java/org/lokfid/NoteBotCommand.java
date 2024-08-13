package org.lokfid;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.core.command.annotations.CommandExecutor;

@SuppressWarnings("unused")
public class NoteBotCommand extends Command {

	public NoteBotCommand() {
		super("NoteBot", "description");
	}

	private static String listQueue() {
		StringBuilder result = new StringBuilder();

		result.append("§6Queue:");

		for (int i = 0; i < NoteBotModule.queue.size(); i++) {
			result.append("\n§6- §e" + i + ": §a" + NoteBotModule.queue.get(i));
		}

		return result.toString();
	}


	@CommandExecutor(subCommand = "queue list")
	private static int queue() {
		ChatUtils.print(listQueue());
		return 1;
	}

	@CommandExecutor(subCommand = "queue add")
	@CommandExecutor.Argument({"string"})
	private static int queueadd(String string) {
		NoteBotModule.queue.add(string);

		ChatUtils.print(("§6Added §a" + string) + "§6 to the queue.");

		return 1;
	}

	@CommandExecutor(subCommand = "queue del")
	@CommandExecutor.Argument({"index"})
	private static int queuedel(int index) {

		String name;

		try {
			name = NoteBotModule.queue.remove(index);
		} catch (IndexOutOfBoundsException e) {
			ChatUtils.print("§cIndex out of bounds.");
			return 0;
		}

		ChatUtils.print("§6Removed §a" + name + "§6 at §e" + index + " §6from the queue.");

		return 1;
	}

	@CommandExecutor(subCommand = "queue clear")
	private static int queueclear() {
		int amount = NoteBotModule.queue.size();
		NoteBotModule.queue.clear();
		ChatUtils.print("§6Cleared §a" + amount + "§6 songs from the queue.");
		return 1;
	}


	@CommandExecutor(subCommand = "start")
	private static int start() {
		NoteBotModule.playing = true;
		return 1;
	}
	
	@CommandExecutor(subCommand = {"stop"})
	private static int stop( ) {
		NoteBotModule.playing = false;
		NoteBotModule.song = null;
		return 1;
	}
	
}
