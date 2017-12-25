#version 330 core

layout(location = 0) in vec3 vVertex;  //object space vertex position

uniform mat4 MVP;  //combined modelview projection matrix

out vec3 vColor;

void main()
{ 	 
	//get clipspace position
        vColor = vVertex;
	gl_Position = MVP*vec4(vVertex.xyz,1); 
}