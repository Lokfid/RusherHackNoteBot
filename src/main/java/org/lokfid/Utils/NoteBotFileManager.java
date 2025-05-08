package org.lokfid.Utils;


import java.nio.file.Path;
import java.nio.file.Paths;

import static org.rusherhack.client.api.Globals.mc;

public class NoteBotFileManager {

    //I can def just remove dir and make songdir mkdirs but meh
    public static final Path dir = Paths.get(mc.gameDirectory.getPath(), "rusherhack/notebot/");
    public static final Path songdir = Paths.get(mc.gameDirectory.getPath(), "rusherhack/notebot/songs/");

    public static void init() {
        if (!dir.toFile().exists()) {
            dir.toFile().mkdirs();
        }
        if (!songdir.toFile().exists()) {
            songdir.toFile().mkdir();
        }
    }
}
