package kr.dorondo.displayHud.core;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.EntityType;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 버전마다 이름/시그니처가 바뀐 NMS 를 한 곳에서 흡수한다 (1.21.8 ~ 26.3 한 jar).
 * <ul>
 *   <li>EntityType.ITEM_DISPLAY 등 상수: 26.x 에서 EntityTypes 클래스로 이동</li>
 *   <li>Transformation: getTranslation()/getScale().. → translation()/scale().., 생성자 인자 Vector3f → Vector3fc</li>
 *   <li>CraftItemStack.asBukkitCopy(ItemStack) → asBukkitCopy(ItemInstance)</li>
 * </ul>
 * 바뀐 부분은 리플렉션으로 한 번만 찾아 캐시한다. 나머지 NMS 는 두 버전에서 바이트코드 시그니처가 같다.
 */
final class NmsCompat {
    private static final Map<String, EntityType<?>> ENTITY_TYPES = new ConcurrentHashMap<>();

    private static Constructor<?> transformationConstructor;
    private static Method translationGetter;
    private static Method leftRotationGetter;
    private static Method scaleGetter;
    private static Method rightRotationGetter;
    private static Method asBukkitCopy;

    private NmsCompat() {
    }

    // ── EntityType ──────────────────────────────────────────────

    static EntityType<?> entityType(String fieldName) {
        return ENTITY_TYPES.computeIfAbsent(fieldName, NmsCompat::findEntityType);
    }

    private static EntityType<?> findEntityType(String fieldName) {
        // 1.21.x: EntityType.ITEM_DISPLAY / 26.x: EntityTypes.ITEM_DISPLAY
        String[] owners = {"net.minecraft.world.entity.EntityType", "net.minecraft.world.entity.EntityTypes"};
        for (String owner : owners) {
            try {
                Field field = Class.forName(owner).getField(fieldName);
                if (Modifier.isStatic(field.getModifiers()) && field.get(null) instanceof EntityType<?> type) {
                    return type;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        throw new IllegalStateException("EntityType not found: " + fieldName);
    }

    // ── Transformation ──────────────────────────────────────────

    static Transformation transformation(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {
        try {
            return (Transformation) transformationConstructor().newInstance(translation, leftRotation, scale, rightRotation);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create Transformation", e);
        }
    }

    static Vector3f translation(Transformation tf) {
        return new Vector3f((Vector3fc) invoke(getter("translation"), tf));
    }

    static Quaternionf leftRotation(Transformation tf) {
        return new Quaternionf((Quaternionfc) invoke(getter("leftRotation"), tf));
    }

    static Vector3f scale(Transformation tf) {
        return new Vector3f((Vector3fc) invoke(getter("scale"), tf));
    }

    static Quaternionf rightRotation(Transformation tf) {
        return new Quaternionf((Quaternionfc) invoke(getter("rightRotation"), tf));
    }

    private static synchronized Constructor<?> transformationConstructor() {
        if (transformationConstructor == null) {
            for (Constructor<?> constructor : Transformation.class.getConstructors()) {
                if (constructor.getParameterCount() == 4) {
                    transformationConstructor = constructor;
                    break;
                }
            }
            if (transformationConstructor == null) {
                throw new IllegalStateException("Transformation(4 args) constructor not found");
            }
        }
        return transformationConstructor;
    }

    private static synchronized Method getter(String name) {
        Method cached = switch (name) {
            case "translation" -> translationGetter;
            case "leftRotation" -> leftRotationGetter;
            case "scale" -> scaleGetter;
            default -> rightRotationGetter;
        };
        if (cached != null) return cached;

        // 26.x: translation() / 1.21.x: getTranslation()
        String legacy = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method found = null;
        for (String candidate : new String[]{name, legacy}) {
            try {
                found = Transformation.class.getMethod(candidate);
                break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (found == null) {
            throw new IllegalStateException("Transformation getter not found: " + name);
        }
        switch (name) {
            case "translation" -> translationGetter = found;
            case "leftRotation" -> leftRotationGetter = found;
            case "scale" -> scaleGetter = found;
            default -> rightRotationGetter = found;
        }
        return found;
    }

    // ── ItemStack ───────────────────────────────────────────────

    static net.minecraft.world.item.ItemStack toNms(ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    static ItemStack toBukkit(net.minecraft.world.item.ItemStack itemStack) {
        return (ItemStack) invokeStatic(asBukkitCopy(), itemStack);
    }

    private static synchronized Method asBukkitCopy() {
        if (asBukkitCopy == null) {
            // 1.21.x: asBukkitCopy(ItemStack) / 26.x: asBukkitCopy(ItemInstance)
            for (Method method : CraftItemStack.class.getMethods()) {
                if (method.getName().equals("asBukkitCopy")
                        && Modifier.isStatic(method.getModifiers())
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(net.minecraft.world.item.ItemStack.class)) {
                    asBukkitCopy = method;
                    break;
                }
            }
            if (asBukkitCopy == null) {
                throw new IllegalStateException("CraftItemStack.asBukkitCopy not found");
            }
        }
        return asBukkitCopy;
    }

    // ── 공통 ────────────────────────────────────────────────────

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot invoke " + method.getName(), e);
        }
    }

    private static Object invokeStatic(Method method, Object arg) {
        try {
            return method.invoke(null, arg);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot invoke " + method.getName(), e);
        }
    }
}
