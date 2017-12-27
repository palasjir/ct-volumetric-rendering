#version 330 core

layout(location = 0) in vec3 vVertex;

//combined modelview projection matrix
uniform mat4 MVP;

out vec3 vColor;

void main()
{ 	 
	//get clipspace position
    vColor = vVertex;
	gl_Position = MVP*vec4(vVertex.xyz,1); 
}