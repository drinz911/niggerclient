package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.event.events.ItemUseListener;
import dev.lvstrng.argon.event.events.TickListener;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.*;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class SafeAnchor extends Module implements TickListener, ItemUseListener {
	private final BooleanSetting whileUse = new BooleanSetting(EncryptedString.of("While Use"), true)
			.setDescription(EncryptedString.of("If it should trigger while eating/using shield"));
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false)
			.setDescription(EncryptedString.of("Makes the CPS HUD think you're legit"));
	private final NumberSetting placeChance = new NumberSetting(EncryptedString.of("Place Chance"), 0, 100, 100, 1);
	private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Place Delay"), 0, 20, 0, 1);

	private int placeClock = 0;
	private final Set<BlockPos> ownedAnchors = new HashSet<>();

	public SafeAnchor() {
		super(EncryptedString.of("Safe Anchor"),
				EncryptedString.of("Places a glowstone safety block between you and the anchor"),
				-1,
				Category.COMBAT);
		addSettings(whileUse, clickSimulation, placeChance, placeDelay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(ItemUseListener.class, this);
		placeClock = 0;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.currentScreen != null)
			return;

		if (((mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)
				|| mc.player.getMainHandStack().getItem() instanceof ShieldItem
				|| mc.player.getOffHandStack().getItem() instanceof ShieldItem
				|| mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD))
				&& GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS)
				&& !whileUse.getValue())
			return;

		if (KeyUtils.isKeyPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
			if (mc.crosshairTarget instanceof BlockHitResult hit) {
				if (BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
					BlockPos anchorPos = hit.getBlockPos();

					// Compute position halfway between player eyes and anchor
					Vec3d playerEyePos = mc.player.getCameraPosVec(1.0f);
					Vec3d anchorCenter = Vec3d.ofCenter(anchorPos);
					Vec3d between = playerEyePos.lerp(anchorCenter, 0.5);
					BlockPos placePos = new BlockPos(between);

					// Only place if the position is air
					if (mc.world.isAir(placePos)) {
						if (placeClock != placeDelay.getValueInt()) {
							placeClock++;
							return;
						}

						int randomInt = MathUtils.randomInt(1, 100);
						if (randomInt <= placeChance.getValueInt()) {
							placeClock = 0;

							if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
								InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
							}

							if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
								if (clickSimulation.getValue())
									MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

								WorldUtils.placeBlock(
										new BlockHitResult(Vec3d.ofCenter(placePos), Direction.UP, placePos, false),
										true
								);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onItemUse(ItemUseEvent event) {
		if (mc.crosshairTarget instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
			if (mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR) {
				BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
				ownedAnchors.add(pos);
			}
		}
	}
}
