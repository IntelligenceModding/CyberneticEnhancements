#version 150

uniform sampler2D SceneDepth;
uniform vec2 ViewportSize;

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float sceneDepth = texture(SceneDepth, gl_FragCoord.xy / ViewportSize).r;
    if (gl_FragCoord.z > sceneDepth + 0.000001 || vertexColor.a <= 0.0) {
        discard;
    }
    fragColor = vertexColor;
}
