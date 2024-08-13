package org.lokfid.Utils;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.apache.commons.io.FilenameUtils;
import org.lokfid.type.Song;
import org.lokfid.type.Note;
import org.rusherhack.client.api.utils.ChatUtils;

import javax.sound.midi.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;


public class NoteBotUtils {
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"}; //midi parse
    public static final EnumMap<NoteBlockInstrument, ItemStack> INSTRUMENT_TO_ITEM = Util.make(new EnumMap<>(NoteBlockInstrument.class), it -> {
        it.put(NoteBlockInstrument.HARP, new ItemStack(Items.DIRT));
        it.put(NoteBlockInstrument.BASEDRUM, new ItemStack(Items.STONE));
        it.put(NoteBlockInstrument.SNARE, new ItemStack(Items.SAND));
        it.put(NoteBlockInstrument.HAT, new ItemStack(Items.GLASS));
        it.put(NoteBlockInstrument.BASS, new ItemStack(Items.OAK_WOOD));
        it.put(NoteBlockInstrument.FLUTE, new ItemStack(Items.CLAY));
        it.put(NoteBlockInstrument.BELL, new ItemStack(Items.GOLD_BLOCK));
        it.put(NoteBlockInstrument.GUITAR, new ItemStack(Items.WHITE_WOOL));
        it.put(NoteBlockInstrument.CHIME, new ItemStack(Items.PACKED_ICE));
        it.put(NoteBlockInstrument.XYLOPHONE, new ItemStack(Items.BONE_BLOCK));
        it.put(NoteBlockInstrument.IRON_XYLOPHONE, new ItemStack(Items.IRON_BLOCK));
        it.put(NoteBlockInstrument.COW_BELL, new ItemStack(Items.SOUL_SAND));
        it.put(NoteBlockInstrument.DIDGERIDOO, new ItemStack(Items.PUMPKIN));
        it.put(NoteBlockInstrument.BIT, new ItemStack(Items.EMERALD_BLOCK));
        it.put(NoteBlockInstrument.BANJO, new ItemStack(Items.HAY_BLOCK));
        it.put(NoteBlockInstrument.PLING, new ItemStack(Items.GLOWSTONE));
    });
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

    public static Song parseMidi(Path path) {
        Multimap<Integer, Note> notes = MultimapBuilder.linkedHashKeys().arrayListValues().build();
        String name = FilenameUtils.getBaseName(path.toString());
        String author = "Unknown";

        try {
            Sequence seq = MidiSystem.getSequence(path.toFile());
            int res = seq.getResolution();
            int trackCount = 0;
            for (Track track : seq.getTracks()) {
                // Track track = seq.getTracks()[0]

                long time = 0;
                long bpm = 120;
                boolean skipNote = false;
                int instrument = 0;
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();

                    int ticksPerSecond = (int) (res * (bpm / 60.0));
                    time = (long) ((1000d / ticksPerSecond) * event.getTick());

                    if (message instanceof ShortMessage) {
                        ShortMessage msg = (ShortMessage) message;

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
                    } else if (message instanceof MetaMessage) {
                        MetaMessage msg = (MetaMessage) message;

                        byte[] data = msg.getData();
                        if (msg.getType() == 0x03) {
                        } else if (msg.getType() == 0x51) {
                            int tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
                            bpm = 60_000_000 / tempo;
                        }
                    }
                }
                trackCount++;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Song(path.getFileName().toString(), name, author, "MIDI", notes);
    }

    public static Song parseNbs(Path path) {
        Multimap<Integer, Note> notes = MultimapBuilder.linkedHashKeys().arrayListValues().build();
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
            e.printStackTrace();
        }
        return new Song(path.getFileName().toString(), name, author, "NBS v" + version, notes);
    }

    public static Song ErrorParsing(){
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
