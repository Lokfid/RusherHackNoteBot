package org.lokfid.Utils;

import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NoteBotFileManager {
    private static Path dir;
    private static Path songdir;

    public static void init(){
        Minecraft mc = Minecraft.getInstance();
        dir = Paths.get(mc.gameDirectory.getPath(), "rusherhack/notebot/");
        if(!dir.toFile().exists()){
            dir.toFile().mkdirs();
        }
        songdir = Paths.get(mc.gameDirectory.getPath(), "rusherhack/notebot/songs/");
        if(!songdir.toFile().exists()){
            songdir.toFile().mkdir();
            }
    }
    public static Path getDir(){
        return dir;
    }
}
