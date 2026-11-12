#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightDirection;     // arah MENUJU lampu (cahaya directional)
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform float specularStrength;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform float shadowBias;

// Hasil: 0.0 = terkena cahaya, 1.0 = di dalam bayangan (rata-rata 3x3 supaya tepinya halus)
float calculateShadow(vec4 fragPosLightSpace, float nDotL) {
    vec3 proj = fragPosLightSpace.xyz / fragPosLightSpace.w;   // ke koordinat NDC
    proj = proj * 0.5 + 0.5;                                   // ke rentang 0..1 (UV shadow map)
    if (proj.z > 1.0) {
        return 0.0;                                            // di luar jangkauan cahaya
    }
    // Shadow Bias dinamis: makin miring permukaan terhadap cahaya, makin besar bias
    float bias = max(shadowBias * 10.0 * (1.0 - nDotL), shadowBias);

    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(shadowMap, proj.xy + vec2(x, y) * texelSize).r;
            shadow += (proj.z - bias > closestDepth) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightDirection);
    float nDotL = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = nDotL * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);    // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow Factor: 1.0 = terang, 0.2 = di dalam bayangan
    float shadowFactor = 1.0;
    if (useShadow) {
        shadowFactor = mix(1.0, 0.2, calculateShadow(FragPosLightSpace, nDotL));
    }

    // Gabungkan tekstur dan pencahayaan (bayangan hanya meredam diffuse dan specular)
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadowFactor * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
