package de.artemis.cyberneticenhancements.client.render;

final class NpcSkinAlpha {
    private NpcSkinAlpha() {
    }

    static Layers split(int[] source) {
        int[] opaque = new int[source.length];
        int[] translucent = new int[source.length];
        boolean hasTranslucency = false;
        for (int i = 0; i < source.length; i++) {
            int alpha = source[i] >>> 24;
            if (alpha == 255) opaque[i] = source[i];
            else if (alpha > 0) {
                translucent[i] = source[i];
                hasTranslucency = true;
            }
        }
        return new Layers(opaque, translucent, hasTranslucency);
    }

    record Layers(int[] opaque, int[] translucent, boolean hasTranslucency) {
    }
}