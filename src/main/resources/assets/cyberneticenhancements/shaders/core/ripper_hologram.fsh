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
    vec4 skin = texture(Sampler0, texCoord0);
    if (skin.a < 0.1) {
        discard;
    }

    float stripePhase = projectionHeight * 125.663706 - projectionTime * 0.628319;
    float stripe = smoothstep(-0.2, 0.55, sin(stripePhase));
    float scanHeight = projectionTime / 80.0 * 1.9;
    float scan = exp(-pow((projectionHeight - scanHeight) / 0.055, 2.0));
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
