#version 330 core
out vec4 FragColor;

in vec2 uv;
uniform sampler2D depthMap;

void main()
{
    float d = texture(depthMap, uv).r;
    float v = clamp(d / 0.7, 0.0, 1.0);   // regangkan kontras rentang depth yang dipakai scene
    FragColor = vec4(vec3(v), 1.0);
}
