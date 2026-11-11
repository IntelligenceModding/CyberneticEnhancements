package de.artemis.cyberneticenhancements.client.render;

import java.lang.reflect.Method;

 final class ShaderPackCompatibility {
    private static final Method GET_INSTANCE;
    private static final Method PACK_ENABLED;
    private static final Method SHADOW_PASS;

    static {
        Method instance = null;
        Method enabled = null;
        Method shadow = null;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            instance = api.getMethod("getInstance");
            enabled = api.getMethod("isShaderPackInUse");
            shadow = api.getMethod("isRenderingShadowPass");
        } catch (ReflectiveOperationException ignored) {

        }
        GET_INSTANCE = instance;
        PACK_ENABLED = enabled;
        SHADOW_PASS = shadow;
    }

    private ShaderPackCompatibility() {}

    static boolean isRenderingShadowPass() {
        if (SHADOW_PASS == null) return false;
        try {
            return (boolean) SHADOW_PASS.invoke(GET_INSTANCE.invoke(null));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    static boolean isShaderPackEnabled() {
        if (PACK_ENABLED == null) return false;
        try {
            return (boolean) PACK_ENABLED.invoke(GET_INSTANCE.invoke(null));
        } catch (ReflectiveOperationException exception) {

            return true;
        }
    }
}
