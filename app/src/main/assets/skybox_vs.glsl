attribute vec3 aPos;

uniform mat4 projection;
uniform mat4 view;

varying vec3 TexCoord;

void main()
{
    TexCoord = aPos;
    vec4 pos = projection * view * vec4(aPos, 1.0);
    gl_Position = pos.xyww;
}