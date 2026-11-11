#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float skinAlpha = texture(Sampler0, texCoord0).a;
    if (skinAlpha < 0.1) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * skinAlpha) * ColorModulator;
}
