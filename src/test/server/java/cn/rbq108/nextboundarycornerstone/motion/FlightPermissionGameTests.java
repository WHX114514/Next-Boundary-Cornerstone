package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.ServeMiao.AllowFly;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(main.MODID)
@PrefixGameTestTemplate(false)
public final class FlightPermissionGameTests {
    @GameTest(template = "surface_empty")
    public static void zeroGravityPreservesIndependentFlightGrants(GameTestHelper helper) {
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(main.BASIC_BACKPACK.get()));
        AllowFly.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.isNoGravity() && !player.mayFly(), "survival backpack needs no creative flight grant");
        var flight=player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        var external=ResourceLocation.fromNamespaceAndPath("qa", "independent_flight");
        try {
            // Simulate a previous Cornerstone grant plus an independent mod's grant.
            AllowFly.updateFlightPermission(player,true);
            flight.addTransientModifier(new AttributeModifier(external,1,AttributeModifier.Operation.ADD_VALUE));
            player.getAbilities().flying=true;
            AllowFly.updateFlightPermission(player,false);
            helper.assertTrue(player.mayFly() && player.getAbilities().flying && flight.hasModifier(external),
                    "removing Cornerstone grant preserves independent attribute and active flight");
            flight.removeModifier(external);
            AllowFly.updateFlightPermission(player,false);
            helper.assertTrue(!player.mayFly() && !player.getAbilities().flying, "exhausted grants restore survival abilities");
        } finally {
            flight.removeModifier(external);
            AllowFly.updateFlightPermission(player,false);
        }
        helper.succeed();
    }
}
