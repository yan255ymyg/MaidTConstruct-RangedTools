package com.maidtcr.maidtconstructrangedtools.util;

import com.maidtcr.maidtconstructrangedtools.MaidTConstructRangedTools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** 匠魂工具物品与“骑士史莱姆”图标缓存。 */
public final class TinkerToolLookup {

    public static final String TCONSTRUCT = "tconstruct";

    /** 工作模式图标使用的材质：骑士史莱姆。 */
    public static final String ICON_MATERIAL = "knightslime";

    private static final Map<String, Item> ITEM_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> ICON_CACHE = new HashMap<>();

    private TinkerToolLookup() {
    }

    /**
     * 取得匠魂的工具物品。找不到时返回空气，调用方需要判空
     * （正常环境下匠魂是本模组的强制依赖，不会走到这里）。
     */
    public static Item toolItem(String path) {
        Item cached = ITEM_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(TCONSTRUCT, path));
        if (item == null) {
            // 不缓存失败结果：注册表尚未就绪时之后还能再查到
            MaidTConstructRangedTools.LOGGER.warn("Tinkers' Construct item {}:{} is missing", TCONSTRUCT, path);
            return Items.AIR;
        }
        ITEM_CACHE.put(path, item);
        return item;
    }

    public static boolean isTool(ItemStack stack, String path) {
        return !stack.isEmpty() && stack.getItem() == toolItem(path);
    }

    /**
     * 构造“骑士史莱姆材质”的工具图标。
     *
     * <p>使用匠魂自己的 {@link ToolBuildHandler#createSingleMaterial}：它会把工具的每个部件
     * 尽量换成骑士史莱姆，不能用的部件（例如骑士史莱姆没有对应的部件属性）回退到该部件的首个可用材质。</p>
     */
    public static ItemStack icon(String path) {
        ItemStack cached = ICON_CACHE.get(path);
        if (cached != null && !cached.isEmpty()) {
            return cached.copy();
        }
        ItemStack stack = build(path);
        if (stack.isEmpty()) {
            // 兜底：至少给出一个非空图标，避免女仆 GUI 渲染出问题
            stack = new ItemStack(toolItem(path));
        }
        if (!stack.isEmpty()) {
            ICON_CACHE.put(path, stack);
        }
        return stack.copy();
    }

    private static ItemStack build(String path) {
        Item item = toolItem(path);
        if (!(item instanceof IModifiable modifiable)) {
            return ItemStack.EMPTY;
        }
        try {
            MaterialId materialId = MaterialId.tryParse(TCONSTRUCT + ":" + ICON_MATERIAL);
            if (materialId != null) {
                IMaterial material = MaterialRegistry.getMaterial(materialId);
                if (material != IMaterial.UNKNOWN) {
                    ItemStack stack = ToolBuildHandler.createSingleMaterial(modifiable, MaterialVariant.of(material));
                    if (!stack.isEmpty()) {
                        return stack;
                    }
                }
            }
            // 材质表还没加载好时的兜底
            return ToolBuildHandler.buildToolForRendering(item, modifiable.getToolDefinition());
        } catch (Exception e) {
            MaidTConstructRangedTools.LOGGER.warn("Failed to build knight slime icon for tconstruct:{}", path, e);
            return ItemStack.EMPTY;
        }
    }

    @Nullable
    public static IModifiable asModifiable(ItemStack stack) {
        return stack.getItem() instanceof IModifiable modifiable ? modifiable : null;
    }
}
