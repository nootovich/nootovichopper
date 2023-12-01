package net.nootovich.nootovichopper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

import static net.minecraftforge.registries.ForgeRegistries.ITEMS;

@Mod(ModNootovichopperInit.MOD_ID)
public class ModNootovichopperInit {

    public static final String MOD_ID = "nootovichopper";

    public ModNootovichopperInit() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static class ModItems {

        public static final DeferredRegister<Item> ITEMS_VANILLA = DeferredRegister.create(ITEMS, "minecraft");

        public static void register(IEventBus eventBus) {
            for (Item item: ITEMS) {
                if (!(item instanceof AxeItem axe)) continue;
                Tier tier = axe.getTier();
                ITEMS_VANILLA.register(
                    axe.toString(), () -> new ModNootovichopperItem(
                        tier, getAxeAttackDamage(axe), getAxeAttackSpeed(axe),
                        new Item.Properties().stacksTo(1).durability(tier.getUses())
                    ));
            }
            ITEMS_VANILLA.register(eventBus);
        }

        private static float getAxeAttackDamage(AxeItem axe) {
            return axe.getAttackDamage()-axe.getTier().getAttackDamageBonus();
        }

        private static float getAxeAttackSpeed(AxeItem axe) {
            return (float) axe.getAttributeModifiers(EquipmentSlot.MAINHAND, axe.getDefaultInstance())
                              .get(Attributes.ATTACK_SPEED).toArray(new AttributeModifier[0])[0].getAmount();
        }
    }

}
