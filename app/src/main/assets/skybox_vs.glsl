attribute vec3 aPos;

uniform mat4 projection;
uniform mat4 view;

varying vec3 TexCoord;

void main()
{
    TexCoord = aPos;
    gl_Position = projection * view * vec4(aPos, 1.0);
}