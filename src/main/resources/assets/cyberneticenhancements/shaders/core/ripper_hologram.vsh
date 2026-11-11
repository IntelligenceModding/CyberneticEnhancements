#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out float projectionHeight;
out float projectionTime;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPosition;
    vertexDistance = length(viewPosition.xyz);
    vertexColor = Color;
    texCoord0 = UV0;
    projectionHeight = float(UV1.x) / 4096.0;
    projectionTime = float(UV1.y) / 256.0;
}
