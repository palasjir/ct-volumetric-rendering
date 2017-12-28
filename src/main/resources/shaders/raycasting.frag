#version 330 core

precision highp float;

in vec3 entryPoint;

uniform vec2 screenSize;
uniform sampler2D exitPoints;
uniform sampler3D volume;
uniform sampler1D transferFn;
uniform sampler3D gradients;
uniform mat4 viewMat;
uniform mat4 normalMat;

out vec4 fragColor;

const int MAX_ITERATIONS = 2000;
const float stepSize = 0.005;
const float specularExp = 128.0;
const float ambientLight = 0.2;
const vec4 bgColor = vec4(0.2, 0.2, 0.2, 1.0);
const vec3 lightPositionWorld = vec3(10.0, -10.0, 10.0);

// light constants
const vec3 Ls = vec3(1.0, 1.0, 1.0);
const vec3 Ld = vec3(1.0, 1.0, 1.0);
const vec3 La = vec3(0.1, 0.1, 0.1);

// surface reflectance
const vec3 Ks = vec3(1.0, 1.0, 1.0);    // fully reflect specular light
const vec3 Kd = vec3(0.1, 0.1, 0.1);    // orange diffuse surface reflectance
const vec3 Ka = vec3(1.0, 1.0, 1.0);    // fully reflect ambient light
const vec3 Ia = La * Ka;                // ambient intensity
const float specularExponent = 100.0;   // specular 'power'

// diffuse intensity
vec3 diffuse(vec3 color, vec3 normal, vec3 directionToLight) {
    return Ld * color * max(0, dot(normal, directionToLight));
}

// specular intensity
vec3 specular(vec3 viewSpacePosition, vec3 viewSpaceLightPosition, vec3 normal, vec3 directionToLight) {
    vec3 reflectionEye = reflect(-directionToLight, normal);
    vec3 surfaceToViewerEye = normalize(-viewSpacePosition);
    float dotProdSpecular = max(dot(reflectionEye, surfaceToViewerEye), 0.0);
    float specular_factor = pow (dotProdSpecular, specularExponent);
    return Ls * Ks * specular_factor;
}

// phong lightning
vec3 phong(vec3 color, vec3 normal, vec3 viewSpacePosition, vec3 viewSpaceLightPosition)
{
  vec3 directionToLight = normalize(viewSpaceLightPosition - viewSpacePosition);
  vec3 Id = diffuse(color, normal, directionToLight);
  vec3 Is = specular(viewSpacePosition, viewSpaceLightPosition, normal, directionToLight);
  return Is + Id + Ia;
}

float random(vec2 cords) {
    return 1 * fract(sin(cords.x * 12.9898 + cords.y * 78.233) * 43758.5453);
}

// depth is calculated from viewing direction
vec3 calcVoxel(vec3 point, vec2 cords, vec3 deltaDir) {
    return point + deltaDir * random(cords);
}

void main()
{
    vec3 exitPoint = texture(exitPoints, gl_FragCoord.st / screenSize).xyz;

    // empty space skipping
    if (entryPoint == exitPoint)
        discard;

    vec3 dir = (exitPoint - entryPoint);
    float len = length(dir);
    vec3 dirN = normalize(dir);
    vec3 deltaDir = dirN * stepSize;
    float deltaDirLen = length(deltaDir);

    vec3 voxelCoord = calcVoxel(entryPoint, gl_FragCoord.xy, deltaDir);

    float alphaAcum = 0.0;
    float lengthAcum = 0.0;
    vec4 colorAcum = vec4(0.0);

    float intensity;
    vec4 sampleColor;
    vec3 gradient;

    vec3 viewSpacePosition;
    vec3 viewSpaceNormal;
    vec3 viewSpaceLightPosition = vec3(viewMat * vec4(lightPositionWorld, 1.0));

    for(int i = 0; i < MAX_ITERATIONS; i++)
    {
    	intensity = texture(volume, voxelCoord).x;
        gradient = normalize(texture(gradients, voxelCoord).xyz);
        sampleColor = texture(transferFn, intensity);

        viewSpacePosition = voxelCoord;
        viewSpaceNormal = normalize((normalMat * vec4(gradient, 0.0)).xyz);

        if (sampleColor.a > 0.0) {
            // correction
    	    sampleColor.a = 1.0 - pow(1.0 - sampleColor.a, stepSize * 100.0f);
            sampleColor.rbg = phong(sampleColor.rgb, viewSpaceNormal, viewSpacePosition, viewSpaceLightPosition);

            colorAcum.rgb = colorAcum.rbg + (1.0 - colorAcum.a) * sampleColor.rgb * sampleColor.a ;
            colorAcum.a = colorAcum.a + (1.0 - colorAcum.a) * sampleColor.a;
    	}

        // increment voxel along viewing direction
        voxelCoord += deltaDir;
    	lengthAcum += deltaDirLen;

        if (lengthAcum >= len ) {
    	    colorAcum.rgb = colorAcum.rgb*colorAcum.a + (1 - colorAcum.a) * bgColor.rgb;
    	    break;
    	}else if (colorAcum.a > 1.0) {
    	    colorAcum.a = 1.0;
    	    break;
    	}
    }
    fragColor = colorAcum;
//     test
//     fragColor = vec4(EntryPoint, 1.0);
//     fragColor = vec4(exitPoint, 1.0);
}