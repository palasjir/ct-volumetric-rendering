#version 330 core

layout (location = 0) in vec3 vVertex;

// combined modelview projection matrix
uniform mat4 mvp;

out vec3 entryPoint;

void main()
{
    gl_Position =  mvp * vec4(vVertex,1);
    entryPoint = vVertex;
}