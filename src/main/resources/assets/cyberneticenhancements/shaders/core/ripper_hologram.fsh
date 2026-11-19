#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in float projectionHeight;
in float projectionTime;

out vec4 fragColor;

void main() {
    // Player models use a 64x64 UV grid, even when a resource pack supplies a larger skin.
    vec2 skinPixel = (clamp(floor(texCoord0 * 64.0), vec2(0.0), vec2(63.0)) + 0.5) / 64.0;
    vec4 skin = texture(Sampler0, skinPixel);
    if (skin.a < 0.1) {
        discard;
    }

    // A model pixel is 1/16 block, including vanilla's 0.9375 player render scale.
    float pixelRow = floor(projectionHeight / (0.9375 / 16.0));
    float stripe = mod(pixelRow - floor(projectionTime / 8.0), 3.0) < 1.0 ? 0.45 : 1.0;
    float scanRow = floor(projectionTime / 80.0 * 32.0);
    float scan = max(0.0, 1.0 - abs(pixelRow - scanRow) * 0.5);
    float flicker = 0.94 + 0.04 * sin(projectionTime * 4.712389)
            + 0.02 * sin(projectionTime * 18.849556);
    vec3 cyan = vec3(0.20, 0.85, 1.0);
    vec3 color = mix(skin.rgb * vertexColor.rgb, cyan, 0.24 + scan * 0.48);
    color *= 0.85 + stripe * 0.25 + scan * 0.35;
    float alpha = skin.a * vertexColor.a * (0.30 + stripe * 0.70) * flicker;
    alpha = min(0.82, alpha + skin.a * scan * 0.20);
    fragColor = vec4(color, alpha) * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
    if (fragColor.a < 0.02) {
        discard;
    }
}
