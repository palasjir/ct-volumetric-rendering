#version 330 core

layout (location = 0) in vec3 vVertex;

out vec3 EntryPoint;
out vec4 ExitPointCoord;

uniform mat4 MVP;

void main()
{
    EntryPoint = vVertex;
    gl_Position =  MVP * vec4(vVertex,1);
}