#version 300 es
precision highp float;
in vec4 Position;
in vec3 Normal;
in vec3 IncidentVector;

uniform vec3 emission;
uniform vec4 color;
uniform samplerCube envMap;
uniform samplerCube envMapRough;

out vec4 gl_FragColor;
#define texture2D(x, y) texture(x, y)
#define textureCube(x, y) texture(x, y)

float my_fresnel(vec3 I, vec3 N, float power,  float scale,  float bias)
{
    return bias + (pow(clamp(1.0 - dot(I, N), 0.0, 1.0), power) * scale);
}

void main()
{
    vec3 I = normalize(IncidentVector);
    vec3 N = normalize(Normal);
    vec3 R = reflect(I, N);
    float fresnel = my_fresnel(-I, N, 5.0, 1.0, 0.1);
    vec3 Creflect = texture(envMap, R).rgb;
	vec3 CreflectRough = texture(envMapRough, R).rgb;
    CreflectRough *= color.rgb;
	Creflect *= color.rgb;
	gl_FragColor = vec4(mix(mix(CreflectRough,Creflect,fresnel),mix(Creflect,CreflectRough,fresnel),color.a)+emission, 1.0);
}