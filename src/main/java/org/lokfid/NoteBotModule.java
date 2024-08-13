package org.lokfid;


import org.rusherhack.client.api.events.client.EventUpdate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.lokfid.Utils.NoteBotFileManager;
import org.lokfid.Utils.NoteBotUtils;
import org.lokfid.type.Note;
import org.lokfid.type.Song;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.rusherhack.core.event.subscribe.Subscribe;

import java.util.*;
import java.util.Map.Entry;


/**
 * NoteBot rusherhack module
 *
 * @author Lokfid
 */
@SuppressWarnings("unused")
public class NoteBotModule extends ToggleableModule {

	/* Status */

	public static boolean playing = false;

	/* Some settings */
	public static boolean loop = false;

	/* The loaded song */
	public static Song song;
	public static List<String> trail = new ArrayList<>();
	public static List<String> queue = new ArrayList<>();

	private static Map<BlockPos, Integer> blockPitches = new HashMap<>();
	private static int timer = -10;
	private static int tuneDelay = 0;

	public static int getNote(BlockPos pos) {
		if (!isNoteblock(pos)) return -1;

        assert mc.level != null;
        return mc.level.getBlockState(pos).getValue(NoteBlock.NOTE);
	}
	public static void playBlock(BlockPos pos) {
		if (!isNoteblock(pos)) return;
        assert mc.gameMode != null;
        mc.gameMode.startDestroyBlock(pos, Direction.UP);
        assert mc.player != null;
        mc.player.swing(InteractionHand.MAIN_HAND);
	}

	public static NoteBlockInstrument getInstrumentUnderneath(BlockPos pos) {
		if (!isNoteblock(pos)) return NoteBlockInstrument.HARP;

		// Retrieve the block underneath
		BlockPos posUnderneath = pos.below();
        assert mc.level != null;
        Block blockUnderneath = mc.level.getBlockState(posUnderneath).getBlock();

		// Return the instrument associated with the block underneath
		return blockToInstrument(blockUnderneath);
	}
	public static NoteBlockInstrument blockToInstrument(Block block) {

		// Specific block checks
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
		String blockIdString = blockId.toString();
		NoteBlockInstrument instrument = NoteBlockInstrument.HARP;  // Default to Harp for any other block


		if (blockIdString.equals("minecraft:dirt" ) || blockIdString.equals("minecraft:air")) {
			return NoteBlockInstrument.HARP;
		} else if (blockIdString.equals("minecraft:clay")) {
			return NoteBlockInstrument.FLUTE;
		} else if (blockIdString.equals("minecraft:gold_block")) {
			return NoteBlockInstrument.BELL;
		} else if (blockIdString.equals("minecraft:packed_ice")) {
			return NoteBlockInstrument.CHIME;
		} else if (blockIdString.equals("minecraft:bone_block")) {
			return NoteBlockInstrument.XYLOPHONE;
		} else if (blockIdString.equals("minecraft:iron_block")) {
			return NoteBlockInstrument.IRON_XYLOPHONE;
		} else if (blockIdString.equals("minecraft:soul_sand")) {
			return NoteBlockInstrument.COW_BELL;
		} else if (blockIdString.equals("minecraft:pumpkin")) {
			return NoteBlockInstrument.DIDGERIDOO;
		} else if (blockIdString.equals("minecraft:emerald_block")) {
			return NoteBlockInstrument.BIT;
		} else if (blockIdString.equals("minecraft:hay_block")) {
			return NoteBlockInstrument.BANJO;
		} else if (blockIdString.equals("minecraft:glowstone")) {
			return NoteBlockInstrument.PLING;
		} else if (blockIdString.equals("minecraft:sand") || blockIdString.equals("minecraft:gravel") || blockIdString.equals("minecraft:concrete_powder")) {
			return NoteBlockInstrument.SNARE;
		} else if (Arrays.asList("minecraft:stone", "minecraft:cobblestone", "minecraft:blackstone", "minecraft:netherrack", "minecraft:nylium", "minecraft:obsidian",
				"minecraft:quartz", "minecraft:sandstone", "minecraft:ores", "minecraft:bricks", "minecraft:corals",
				"minecraft:respawn_anchor", "minecraft:bedrock", "minecraft:concrete").contains(blockIdString)) {
			return NoteBlockInstrument.BASEDRUM;
		} else if (blockIdString.equals("minecraft:glass")) {
			return NoteBlockInstrument.HAT;
		}


		SoundType material = block.defaultBlockState().getSoundType();

		// Check for blocks with specific materials
		if (material.equals(SoundType.WOOD)) {
			return NoteBlockInstrument.BASS;
		}
		if (material.equals(SoundType.WOOL)) {
			return NoteBlockInstrument.GUITAR;
		}
		if (material.equals(SoundType.GLASS)) {
			return NoteBlockInstrument.HAT;
		}
		if (material.equals(SoundType.STONE)) {
			return NoteBlockInstrument.BASEDRUM;
		}

		return instrument;
	}
	public static boolean isNoteblock(BlockPos pos) {
		// Checks if this block is a noteblock and the noteblock can be played
        assert mc.level != null;
        return mc.level.getBlockState(pos).getBlock() instanceof NoteBlock && mc.level.getBlockState(pos.above()).isAir();
	}

	public static void stop() {
		playing = false;
		song = null;
		blockPitches.clear();
		timer = -10;
		tuneDelay = 0;
	}

	public static void loadSong() {
		blockPitches.clear();

		try {
            assert mc.gameMode != null;
            if (!mc.gameMode.getPlayerMode().isSurvival()) {
				ChatUtils.print("§cNot in Survival mode!");
				return;
			} else if (song == null) {
				ChatUtils.print("§6No song in queue!, Use §c*notebot queueadd §6to add a song.");
				return;
			}
		} catch (NullPointerException e) {
			return;
		}

		timer = -10;

        assert mc.player != null;
        BlockPos playerEyePos = new BlockPos((int) mc.player.getEyePosition().x, (int) mc.player.getEyePosition().y, (int) mc.player.getEyePosition().z);

		List<BlockPos> noteblocks = BlockPos.withinManhattanStream(playerEyePos, 5, 5, 5).filter(NoteBotModule::isNoteblock).map(BlockPos::immutable).toList();

		HashMap<NoteBlockInstrument, Integer> requiredInstruments = new HashMap<>();
		HashMap<NoteBlockInstrument, Integer> foundInstruments = new HashMap<>();

		for (Note note : song.requirements) {
			NoteBlockInstrument instrument = NoteBlockInstrument.values()[note.instrument];
			requiredInstruments.put(instrument, requiredInstruments.getOrDefault(instrument, 0) + 1);
			for (BlockPos pos : noteblocks) {
				if (blockPitches.containsKey(pos)) continue;

				NoteBlockInstrument blockInstrument = getInstrumentUnderneath(pos);
				if (note.instrument == blockInstrument.ordinal() && blockPitches.entrySet().stream().filter(e -> e.getValue() == note.pitch).noneMatch(e -> getInstrumentUnderneath(e.getKey()).ordinal() == blockInstrument.ordinal())) {
					blockPitches.put(pos, note.pitch);
					foundInstruments.put(blockInstrument, foundInstruments.getOrDefault(blockInstrument, 0) + 1);
					break;
				}
			}
		}

		for (NoteBlockInstrument instrument : requiredInstruments.keySet()) {
			int requiredCount = requiredInstruments.get(instrument);
			int foundCount = foundInstruments.getOrDefault(instrument, 0);
			int missingCount = requiredCount - foundCount;

			if (missingCount > 0) {
				ChatUtils.print("§6Warning: Missing §c" + missingCount + " §6" + instrument + " Noteblocks");
			}
		}

	}

	public NoteBotModule() {
		super("NoteBotModule", "NoteBot Module (just turn it on)", ModuleCategory.CLIENT);
	}

	@Subscribe
	public void onUpdate(EventUpdate event){
		if (!playing) return;

		if (song == null) {
			if (queue.isEmpty()) {
				ChatUtils.print("§cYou have no songs in your queue!");
				stop();
				return;
			}
			NoteBotModule.song = NoteBotUtils.parse(
					NoteBotFileManager.getDir().resolve(
							"songs/" + NoteBotModule.queue.remove(0)
					)
			);

			loadSong();
		}

		// Tune Noteblocks
		for (Entry<BlockPos, Integer> e : blockPitches.entrySet()) {
			int note = getNote(e.getKey());
			if (note == -1)
				continue;

			if (note != e.getValue()) {
				if (tuneDelay < 5) {
					tuneDelay++;
					return;
				}

				int neededNote = e.getValue() < note ? e.getValue() + 25 : e.getValue();
				int reqTunes = Math.min(25, neededNote - note);
				for (int i = 0; i < reqTunes; i++) {
                    assert mc.gameMode != null;
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.upFromBottomCenterOf(e.getKey(), 1), Direction.UP, e.getKey(), true));
                }

				tuneDelay = 0;

				return;
			}
		}

		// Loop
		if (timer - 10 > song.length) {
			if (loop) {
				timer = -10;
			} else if (!queue.isEmpty()) {
				song = null;
				return;
			} else {
				ChatUtils.print("§6The queue is empty, stopping...");
				stop();
				return;
			}
		}

		if (timer == -10) {
			ChatUtils.print("§6Now Playing: §a" + song.filename);
		}

		timer++;

		Collection<Note> curNotes = song.notes.get(timer);

		if (curNotes.isEmpty()) return;

		for (Entry<BlockPos, Integer> e : blockPitches.entrySet()) {
			for (Note i : curNotes) {
				if (isNoteblock(e.getKey()) && (i.pitch == getNote(e.getKey())) && (i.instrument == getInstrumentUnderneath(e.getKey()).ordinal()))
					playBlock(e.getKey());
			}
		}

	}

	@Override
	public void onEnable() {
		NoteBotFileManager.init();
		}

	
	@Override
	public void onDisable() {

	}
}
