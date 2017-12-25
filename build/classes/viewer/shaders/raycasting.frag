#version 330 core

in vec3 EntryPoint;
in vec2 UV;
 
uniform float     StepSize;
uniform vec2      ScreenSize;
uniform sampler2D exitPoints;
uniform sampler3D VolumeTex;
uniform sampler1D TransferFunc;
uniform sampler3D gradients;
uniform mat4 V;
uniform mat4 normalMatrix;

const float specularExp = 128.0;
const float ambientLight = 0.2;
const vec4 lightCol = vec4(0.2, 0.2, 0.2, 1.0);

vec3 phong(vec3 color, vec3 viewSpaceNormal, vec3 viewSpacePosition);
vec3 shading(vec3 N, vec3 V, vec3 L);

vec3 light_position_world = vec3 (10.0, -10.0, 10.0);
const vec3 Ls = vec3 (1.0, 1.0, 1.0); 
const vec3 Ld = vec3 (1.0, 1.0, 1.0); 
const vec3 La = vec3 (0.1, 0.1, 0.1);

// surface reflectance
const vec3 Ks = vec3 (1.0, 1.0, 1.0); // fully reflect specular light
const vec3 Kd = vec3 (0.1, 0.1, 0.1); // orange diffuse surface reflectance
const vec3 Ka = vec3 (1.0, 1.0, 1.0); // fully reflect ambient light
const float specular_exponent = 100.0; // specular 'power'

void main()
{
    vec3 exitPoint = texture(exitPoints, gl_FragCoord.st/ScreenSize).xyz;
    
    // empty space skipping
    if (EntryPoint == exitPoint)
        discard;

    vec3 dir = (exitPoint - EntryPoint);
    float len = length(dir);
    vec3 dirN = normalize(dir);
    
    vec3 deltaDir = dirN * StepSize;
    float deltaDirLen = length(deltaDir);
    
    // jittering
    float random = 1* fract(sin(gl_FragCoord.x * 12.9898 + gl_FragCoord.y * 78.233) * 43758.5453); 
    vec3 voxelCoord = EntryPoint + deltaDir *random;;

    vec4 colorAcum = vec4(0.0); // The dest color
    float alphaAcum = 0.0;

    float intensity;
    float lengthAcum = 0.0;
    vec4 sample;   // sample color 
    vec3 gradient;

    vec4 bgColor = vec4(0.2, 0.2, 0.2, 1.0);

    vec3 viewSpacePosition;
    vec3 viewSpaceNormal;


    for(int i = 0; i < 2000; i++)
    {
    	intensity =  texture(VolumeTex, voxelCoord).x;
        gradient = normalize(texture(gradients, voxelCoord).xyz);
        sample = texture(TransferFunc, intensity);
        
        viewSpacePosition = voxelCoord;
        viewSpaceNormal = normalize(( normalMatrix* vec4(gradient, 0.0)).xyz);

        if (sample.a > 0.0) {
            // correction
    	    sample.a = 1.0 - pow(1.0 - sample.a, StepSize*100.0f);
            sample.rbg = phong(sample.rgb, viewSpaceNormal, viewSpacePosition);
    	    
            colorAcum.rgb = colorAcum.rbg + (1.0 - colorAcum.a) * sample.rgb * sample.a ;
            colorAcum.a = colorAcum.a + (1.0 - colorAcum.a) * sample.a;
    	}

        voxelCoord += deltaDir;
    	lengthAcum += deltaDirLen;
        
        if (lengthAcum >= len ) {	
    	    colorAcum.rgb = colorAcum.rgb*colorAcum.a + (1 - colorAcum.a)*bgColor.rgb;		
    	    break;  	
    	}else if (colorAcum.a > 1.0) {
    	    colorAcum.a = 1.0;
    	    break;
    	}
    }

    gl_FragColor = colorAcum;
}

vec3 phong(vec3 color, vec3 viewSpaceNormal, vec3 viewSpacePosition)
{   
   
 // ambient intensity
  vec3 Ia = La * Ka;

  // diffuse intensity
  vec3 viewSpaceLightPosition = vec3 (V*vec4 (light_position_world, 1.0));
  vec3 normal = normalize(viewSpaceNormal);
  vec3 directionToLight = normalize(viewSpaceLightPosition - viewSpacePosition);
  vec3 Id = Ld * color * max(0, dot(normal, directionToLight));
  
  // specular intensity
  vec3 reflection_eye = reflect(-directionToLight, normal);
  vec3 surface_to_viewer_eye = normalize(-viewSpacePosition);
  float dot_prod_specular = dot (reflection_eye, surface_to_viewer_eye);
  dot_prod_specular = max(dot_prod_specular, 0.0);
  float specular_factor = pow (dot_prod_specular, specular_exponent);
  vec3 Is = Ls * Ks * specular_factor;
  
  // final colour
  return Is + Id + Ia;
}