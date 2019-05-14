uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

attribute vec4 vPosition;
attribute vec2 aTexCoord;

varying vec2 TexCoord;

void main() {
    gl_Position = projection * view * model * vPosition;
    TexCoord = aTexCoord;
}