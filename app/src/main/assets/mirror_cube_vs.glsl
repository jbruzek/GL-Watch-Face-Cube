uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

attribute vec3 aPosition;
attribute vec3 aNormal;

varying vec3 Normal;
varying vec3 Position;

void main() {
    Normal = mat3(transpose(inverse(model))) * aNormal;
    Position = vec3(model * vec4(aPosition, 1.0));
    gl_Position = projection * view * model * vec4(aPosition, 1.0);
}