package org.lokfid.Utils;

import com.google.common.collect.ArrayListMultimap;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FilenameUtils;
import org.lokfid.type.Song;
import org.lokfid.type.Note;
import org.rusherhack.client.api.utils.ChatUtils;

import javax.sound.midi.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class NoteBotUtils {
    private static final int[] NOTE_POSES = {6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17}; //parse midi

    public static Song parse(Path path) {
        String string = path.toString();
        if (string.endsWith(".mid") || string.endsWith(".midi")) {
            return parseMidi(path);
        } else if (string.endsWith(".nbs")) {
            return parseNbs(path);
        } else {
            return ErrorParsing();
        }
    }

    private static Song parseMidi(Path path) {
        ArrayListMultimap<Integer, Note> notes = ArrayListMultimap.create();
        String name = FilenameUtils.getBaseName(path.toString());
        String author = "Unknown";

        try {
            Sequence seq = MidiSystem.getSequence(path.toFile());
            int res = seq.getResolution();
            for (Track track : seq.getTracks()) {
                long time = 0;
                long bpm = 120;

                boolean skipNote = false;

                int instrument = 0;

                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();

                    int ticksPerSecond = (int) (res * (bpm / 60.0));
                    time = (long) ((1000d / ticksPerSecond) * event.getTick());

                    if (message instanceof ShortMessage msg) {
                        if (msg.getCommand() == 0x90 || msg.getCommand() == 0x80) {
                            int key = msg.getData1();
                            int note = key % 12;
                            if (!skipNote) {
                                notes.put((int) Math.round(time / 50d), new Note(NOTE_POSES[note], instrument));
                                skipNote = true;
                            } else {
                                skipNote = false;
                            }
                        }
                    } else if (message instanceof MetaMessage msg) {
                        byte[] data = msg.getData();
                        if (msg.getType() == 0x51) {
                            int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                            bpm = 60_000_000 / tempo;
                        }
                    }
                }
            }


        } catch (Exception ignored) {
        }
        return new Song(path.getFileName().toString(), name, author, "MIDI", notes);
    }

    private static Song parseNbs(Path path) {
        ArrayListMultimap<Integer, Note> notes = ArrayListMultimap.create();
        String name = FilenameUtils.getBaseName(path.toString());
        String author = "Unknown";
        int version = 0;

        try (InputStream input = Files.newInputStream(path)) {
            // Signature
            version = readShort(input) != 0 ? 0 : input.read();

            // Skipping most of the headers because we don't need them
            input.skip(version >= 3 ? 5 : version >= 1 ? 3 : 2);
            String iname = readString(input);
            String iauthor = readString(input);
            String ioauthor = readString(input);
            if (!iname.isEmpty())
                name = iname;

            if (!ioauthor.isEmpty()) {
                author = ioauthor;
            } else if (!iauthor.isEmpty()) {
                author = iauthor;
            }

            readString(input);

            float tempo = readShort(input) / 100f;

            input.skip(23);
            readString(input);
            if (version >= 4)
                input.skip(4);

            // Notes
            double tick = -1;
            short jump;
            while ((jump = readShort(input)) != 0) {
                tick += jump * (20f / tempo);

                // Iterate through layers
                while (readShort(input) != 0) {
                    int instrument = input.read();
                    if (instrument == 0) {
                        instrument = 0;
                    } else if (instrument == 1) {
                        instrument = 4;
                    } else if (instrument == 2) {
                        instrument = 1;
                    } else if (instrument == 3) {
                        instrument = 2;
                    } else if (instrument == 4) {
                        instrument = 3;
                    } else if (instrument == 5) {
                        instrument = 7;
                    } else if (instrument == 6) {
                        instrument = 5;
                    } else if (instrument == 7) {
                        instrument = 6;
                    } else if (instrument > 15) {
                        instrument = 0;
                    }

                    int key = input.read() - 33;
                    if (key < 0) {
                        ChatUtils.print(Component.literal("Note @" + tick + " Key: " + key + " is below the 2-octave range!"));
                        key = Math.floorMod(key, 12);
                    } else if (key > 25) {
                        ChatUtils.print(Component.literal("Note @" + tick + " Key: " + key + " is above the 2-octave range!"));
                        key = Math.floorMod(key, 12) + 12;
                    }

                    notes.put((int) Math.round(tick), new Note(key, instrument));

                    if (version >= 4)
                        input.skip(4);
                }
            }


        } catch (IOException e) {
            ChatUtils.print(Component.literal("Error reading Nbs file!"));
        }
        return new Song(path.getFileName().toString(), name, author, "NBS v" + version, notes);
    }

    private static Song ErrorParsing(){
        ChatUtils.print(Component.literal("Wrong file format, please use .midi or .nbs"));
        return null;
    }
    // Reads a little endian short
    private static short readShort(InputStream input) throws IOException {
        return (short) (input.read() & 0xFF | input.read() << 8);
    }

    // Reads a little endian int
    private static int readInt(InputStream input) throws IOException {
        return input.read() | input.read() << 8 | input.read() << 16 | input.read() << 24;
    }

    private static String readString(InputStream input) throws IOException {
        return new String(input.readNBytes(readInt(input)));
    }
}
