// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Per-pixel lighting (Phong Shading) dengan pilihan Phong / Blinn-Phong,
// Texture Mapping (Indikator 4) dan Shadow Mapping + PCF (Indikator 5).
// ==========================================================
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;   // unit 0
uniform sampler2D shadowMap;        // unit 1
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform float shadowBias;           // pengali bias: 1.0 = rumus modul, 0.0 = tanpa bias (Shadow Acne)

// Mengembalikan 0.0 (terang) .. 1.0 (bayangan penuh) dengan PCF 3x3
float calcShadow(vec4 fragPosLS, vec3 norm, vec3 lightDir)
{
    vec3 proj = fragPosLS.xyz / fragPosLS.w;   // perspective divide
    proj = proj * 0.5 + 0.5;                   // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0 || proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0)
        return 0.0;                            // di luar frustum cahaya -> terang

    // Bias dinamis berdasarkan kemiringan normal (rumus pada modul)
    float bias = shadowBias * max(0.05 * (1.0 - dot(norm, lightDir)), 0.005);

    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x)
    {
        for (int y = -1; y <= 1; ++y)
        {
            float dShadow = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (proj.z - bias > dShadow) ? 1.0 : 0.0;   // d_current > d_shadow + bias
        }
    }
    return shadow / 9.0;
}

void main()
{
    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float ndotl = dot(norm, lightDir);
    float diff = max(ndotl, 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
    }
    if (ndotl <= 0.0) spec = 0.0;   // tidak ada kilau di sisi yang membelakangi cahaya
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow: Shadow Factor = 0.2 (dalam bayangan) atau 1.0 (terkena cahaya)
    float shadowFactor = 1.0;
    if (useShadow) {
        float shadow = calcShadow(FragPosLightSpace, norm, lightDir);
        shadowFactor = mix(1.0, 0.2, shadow);
    }

    // Combine Texture & Lighting
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadowFactor * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
