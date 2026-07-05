package vortx.red.skingrabber;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Predicate;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class SkinGrabber implements ClientModInitializer {

	public static final String MOD_NAME = "Skin Grabber";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
	private static final TextColor SUCCESS = TextColor.fromRgb(0x7FFF7F);
	private static final TextColor SUCCESS_HIGHLIGHT = TextColor.fromRgb(0xAAFFAA);
	private static final TextColor SUCCESS_TEXT = TextColor.fromRgb(0xFFFFFF);
	private static final TextColor DARK_PREFIX = TextColor.fromRgb(0xAAAAAA);
	private static final TextColor NOTE = TextColor.fromRgb(0xFFD47F);
	private static final TextColor NOTE_TEXT = TextColor.fromRgb(0xFFFFAA);
	private static final TextColor ERROR = TextColor.fromRgb(0xFF5555);
	public static Minecraft MC = Minecraft.getInstance();

	private static void grabCommand(String givenPlayerName, CommandContext<FabricClientCommandSource> context) {
		if (context == null) return;
		if (MC.level == null) return;
		if (MC.player == null) return;

		ClientLevel world = MC.level;
		LocalPlayer player = MC.player;

		Avatar grabbedAvatar = null;

		// Attempt to get the Player from the argument, if we fail, resort to the nearest player
		if (givenPlayerName != null) {
			grabbedAvatar = world.players().stream()
					.filter(p -> p.getName().getString().equals(givenPlayerName))
					.findFirst()
					.orElse(null);
			if (grabbedAvatar == null || isInvalid(grabbedAvatar)) {
				player.sendSystemMessage(
						Component.literal("")
								.append(Component.literal("»")
										.setStyle(Style.EMPTY.withColor(ERROR).withBold(true))
								)
								.append(Component.literal(" Could not find the specified player."))
						);
				return;
			}
		}

		if (grabbedAvatar == null) {
			// Get the closest entity
			grabbedAvatar = world.getNearestPlayer(player.getX(), player.getY(), player.getZ(), 5D, (p) -> p != player);

			// Try mannequins
			if (grabbedAvatar == null) {
				Mannequin mannequin = getNearestMannequin(world, player, 5);
				if (mannequin != null) {
					grabbedAvatar = mannequin;
				}
			}
			if (grabbedAvatar == null || isInvalid(grabbedAvatar) || grabbedAvatar.getUUID().equals(player.getUUID())) {
				player.sendSystemMessage(
						Component.literal("")
								.append(Component.literal("»")
										.setStyle(Style.EMPTY.withColor(ERROR).withBold(true))
								)
								.append(Component.literal(" Could not find a valid player.")
								)
								.append(Component.literal("\nℹ")
										.setStyle(Style.EMPTY.withColor(NOTE))
								)
								.append(Component.literal(" Make sure you are close to a player (6 blocks), alternatively run /grabskin <player>.")
										.setStyle(Style.EMPTY.withColor(NOTE_TEXT))
								)
						);
				return;
			}
		}


		// All the methods we are accessing should already be confirmed to not be null due to isInvalid()
		Collection<Property> textures = grabbedAvatar.getProfile().partialProfile().properties().get("textures");
		if (textures.isEmpty()) {
			// isInvalid already checks if the list is empty, but this is to get rid of the warning
			return;
		}
		String skin = textures.iterator().next().value();

		byte[] byteArray;
		try {
			byteArray = Base64.getDecoder().decode(skin);
		} catch (IllegalArgumentException ignored) {
			return;
		}
		String decodedSkin = new String(byteArray, StandardCharsets.UTF_8);

		JsonElement jsonElement = JsonParser.parseString(decodedSkin);
		String profileName = jsonElement.getAsJsonObject().get("profileName").getAsString();
		String url = jsonElement.getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

		LOGGER.info("Grabbing skin!\n-----------------------");
		LOGGER.info("Profile Name: {}", profileName);
		LOGGER.info("Skin: {}", skin);
		LOGGER.info("Decoded Skin: {}", decodedSkin);
		LOGGER.info("URL: {}\n-----------------------", url);

		player.sendSystemMessage(Component.literal("")
						.append(Component.literal("»")
								.setStyle(Style.EMPTY.withColor(SUCCESS).withBold(true))
						)
						.append(Component.literal(" Successfully grabbed the skin of "))
						.append(grabbedAvatar.getName())
						.append(Component.literal("!"))
						.append(Component.literal("\n›")
								.setStyle(Style.EMPTY.withColor(DARK_PREFIX))
						)
						.append(Component.literal(" Profile Name: ")
								.setStyle(Style.EMPTY.withColor(SUCCESS_HIGHLIGHT))
						)
						.append(Component.literal(profileName)
								.setStyle(Style.EMPTY.withColor(SUCCESS_TEXT))
						)
						.append(Component.literal("\n›")
								.setStyle(Style.EMPTY.withColor(DARK_PREFIX))
						)
						.append(Component.literal(" URL: ")
								.setStyle(Style.EMPTY.withColor(SUCCESS_HIGHLIGHT))
						)
						.append(Component.literal(url)
								.setStyle(Style.EMPTY
										.withColor(SUCCESS_TEXT)
										.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
										.withHoverEvent(new HoverEvent.ShowText(
												Component.literal("Click to open.")
														.withStyle(s2 -> s2.withColor(ChatFormatting.GRAY))
										))
								)
						)
						.append(Component.literal("\nℹ")
								.setStyle(Style.EMPTY.withColor(NOTE))
						)
						.append(Component.literal(" Check the Console for more information.")
								.setStyle(Style.EMPTY.withColor(NOTE_TEXT))
						)
				);

		player.playSound(SoundEvents.COPPER_BULB_TURN_ON, 1.0F, 1.0F);

	}


	public static boolean isInvalid(Avatar player) {
		GameProfile gameProfile = player.getProfile().partialProfile();
		return gameProfile == null || gameProfile.properties() == null || gameProfile.properties().get("textures").isEmpty();
	}

	private static @Nullable Mannequin getNearestMannequin(ClientLevel level, Player player, int range) {
		double best = (double)-1.0F;
		Mannequin result = null;


		for(Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof Mannequin mannequin)) continue;

			double dist = entity.distanceToSqr(player);
			if ((range < (double)0.0F || dist < range * range) && (best == (double)-1.0F || dist < best)) {
				best = dist;
				result = mannequin;
			}
		}

		return result;
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing");
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(
						literal("grabskin")
								.executes(context -> {
									grabCommand(null, context);
									return 0;
								})
								.then(
										argument("playerName", StringArgumentType.string())
												.suggests((context, builder) -> {
													FabricClientCommandSource source = context.getSource();
													source.getOnlinePlayerNames().forEach(builder::suggest);
													return builder.buildFuture();
												})
												.executes(context -> {
													grabCommand(StringArgumentType.getString(context, "playerName"), context);
													return 0;
												})
								)
				)
		);
	}

}
