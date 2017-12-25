#version 410 core

in vec3 vColor;

layout(location = 0) out vec4 vFragColor;	//fragment shader output

void main()
{
	//return constant colour as shader output
	vFragColor = vec4(vColor, 1);
}