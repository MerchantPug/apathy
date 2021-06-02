package agency.highlysuspect.apathy;

import agency.highlysuspect.apathy.list.PlayerSet;
import agency.highlysuspect.apathy.list.PlayerSetArgumentType;
import agency.highlysuspect.apathy.list.PlayerSetManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;

import java.util.*;
import java.util.function.BiFunction;

import static agency.highlysuspect.apathy.list.PlayerSetArgumentType.getPlayerSet;
import static agency.highlysuspect.apathy.list.PlayerSetArgumentType.playerSet;
import static net.minecraft.command.argument.EntityArgumentType.getPlayers;
import static net.minecraft.command.argument.EntityArgumentType.players;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
	public static void registerArgumentTypes() {
		ArgumentTypes.register(Init.id("player_set").toString(), PlayerSetArgumentType.class, new ConstantArgumentSerializer<>(PlayerSetArgumentType::playerSet));
	}
	
	public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
		dispatcher.register(literal(Init.MODID)
			.then(literal("set")
				.then(literal("join")
					.then(argument("set", playerSet()).suggests(PlayerSetArgumentType::suggestSelfSelectPlayerSets)
						.executes(cmd -> join(cmd, Collections.singletonList(cmd.getSource().getPlayer()), getPlayerSet(cmd, "set"), true))))
				.then(literal("part")
					.then(argument("set", playerSet()).suggests(PlayerSetArgumentType::suggestSelfSelectPlayerSets)
						.executes(cmd -> part(cmd, Collections.singletonList(cmd.getSource().getPlayer()), getPlayerSet(cmd, "set"), true))))
				.then(literal("show")
					.executes(cmd -> show(cmd, Collections.singletonList(cmd.getSource().getPlayer())))))
			.then(literal("set-admin")
				.requires(src -> src.hasPermissionLevel(2))
				.then(literal("join")
					.then(argument("who", players())
						.then(argument("set", playerSet()).suggests(PlayerSetArgumentType::suggestAllPlayerSets)
							.executes(cmd -> join(cmd, getPlayers(cmd, "who"), getPlayerSet(cmd, "set"), false)))))
				.then(literal("part")
					.then(argument("who", players())
						.then(argument("set", playerSet()).suggests(PlayerSetArgumentType::suggestAllPlayerSets)
							.executes(cmd -> part(cmd, getPlayers(cmd, "who"), getPlayerSet(cmd, "set"), false)))))
				.then(literal("show-all")
					.executes(Commands::showAll))
				.then(literal("delete")
					.then(argument("set", playerSet()).suggests(PlayerSetArgumentType::suggestAllPlayerSets)
						.executes(cmd -> delete(cmd, getPlayerSet(cmd, "set"))))))
			.then(literal("reload")
				.requires(src -> src.hasPermissionLevel(2))
				.executes(Commands::reloadNow))
		);
	}
	
	private static int join(CommandContext<ServerCommandSource> cmd, Collection<ServerPlayerEntity> players, PlayerSet set, boolean requireSelfSelect) {
		if(requireSelfSelect && !set.isSelfSelect()) {
			cmd.getSource().sendError(new TranslatableText("apathy.commands.set.notSelfSelect", set.getName()));
			return 0;
		}
		
		int success = 0;
		for(ServerPlayerEntity player : players) {
			success += frobnicate(cmd, player, set, "apathy.commands.set.joinSuccess", "apathy.commands.set.alreadyInSet", PlayerSet::join);
		}
		return success;
	}
	
	private static int part(CommandContext<ServerCommandSource> cmd, Collection<ServerPlayerEntity> players, PlayerSet set, boolean requireSelfSelect) {
		if(requireSelfSelect && !set.isSelfSelect()) {
			cmd.getSource().sendError(new TranslatableText("apathy.commands.set.notSelfSelect", set.getName()));
			return 0;
		}
		
		int success = 0;
		for(ServerPlayerEntity player : players) {
			success += frobnicate(cmd, player, set, "apathy.commands.set.partSuccess", "apathy.commands.set.notInSet", PlayerSet::part);
		}
		return success;
	}
	
	private static int frobnicate(CommandContext<ServerCommandSource> cmd, ServerPlayerEntity player, PlayerSet set, String success, String fail, BiFunction<PlayerSet, ServerPlayerEntity, Boolean> thing) {
		if(thing.apply(set, player)) {
			cmd.getSource().sendFeedback(new TranslatableText(success, player.getName(), set.getName()), true);
			return 1;
		} else {
			cmd.getSource().sendError(new TranslatableText(fail, player.getName(), set.getName()));
			return 0;
		}
	}
	
	private static int show(CommandContext<ServerCommandSource> cmd, Collection<ServerPlayerEntity> players) {
		PlayerSetManager setManager = PlayerSetManager.getFor(cmd.getSource().getMinecraftServer());
		
		if(setManager.isEmpty()) {
			cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.set.available.none"), false);
		} else {
			cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.set.available", Texts.join(setManager.allSets(), PlayerSet::toText)), false);
		}
		
		int success = 0;
		
		for(ServerPlayerEntity player : players) {
			Collection<PlayerSet> yea = setManager.allSetsContaining_KindaSlow_DontUseThisOnTheHotPath(player);
			
			if(yea.isEmpty()) {
				cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.set.show.none", player.getName()), false);
			} else {
				cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.set.show", player.getName(), Texts.join(yea, PlayerSet::toText)), false);
				success++;
			}
		}
		
		return success;
	}
	
	private static int showAll(CommandContext<ServerCommandSource> cmd) {
		PlayerSetManager setManager = PlayerSetManager.getFor(cmd.getSource().getMinecraftServer());
		
		if(setManager.isEmpty()) {
			cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.show-all.none"), false);
		} else {
			cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.show-all.list", Texts.join(setManager.allSets(), PlayerSet::toText)), false);
			PlayerManager mgr = cmd.getSource().getMinecraftServer().getPlayerManager();
			
			for(PlayerSet set : setManager.allSets()) {
				cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.show-all.set", set.getName(), set.members().size()), false);
				for(UUID uuid : set.members()) {
					ServerPlayerEntity player = mgr.getPlayer(uuid);
					TranslatableText asdf = player == null ?
						new TranslatableText("apathy.commands.show-all.set.member.offline-player", uuid) :
						new TranslatableText("apathy.commands.show-all.set.member.online-player", player.getName(), uuid);
					cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.show-all.set.member", asdf), false);
				}
			}
		}
		return 0;
	}
	
	private static int delete(CommandContext<ServerCommandSource> cmd, PlayerSet set) {
		PlayerSetManager setManager = PlayerSetManager.getFor(cmd.getSource().getMinecraftServer());
		
		Optional<String> yeayehhehh = Init.config.playerSetName;
		if(yeayehhehh.isPresent() && yeayehhehh.get().equals(set.getName())) {
			cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.delete.fail.config", set.getName()), false);
			return 0;
		}
		
		if(setManager.hasSet(set.getName())) {
			setManager.deleteSet(set.getName());
			cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.delete.success", set.getName()), false);
			return 1;
		} else {
			cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.delete.fail.noSet", set.getName()), false);
			return 0;
		}
	}
	
	private static int reloadNow(CommandContext<ServerCommandSource> cmd) {
		Init.reloadNow(cmd.getSource().getMinecraftServer());
		cmd.getSource().sendFeedback(new TranslatableText("apathy.commands.reload"), true);
		return 0;
	}
}
