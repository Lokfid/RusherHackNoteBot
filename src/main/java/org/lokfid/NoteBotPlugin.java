package org.lokfid;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * Example rusherhack plugin
 *
 * @author Lokfid
 */
public class NoteBotPlugin extends Plugin {
	
	@Override
	public void onLoad() {
		
		//logger
		this.getLogger().info("NoteBot Loaded!");
		final NoteBotModule noteBotModule = new NoteBotModule();
		RusherHackAPI.getModuleManager().registerFeature(noteBotModule);

	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("NoteBot unloaded!");
	}
}