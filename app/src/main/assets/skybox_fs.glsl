precision mediump float;

varying vec3 TexCoord;

uniform samplerCube skybox;

void main()
{
    gl_FragColor = textureCube(skybox, TexCoord);
}