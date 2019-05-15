varying vec3 TexCoord;

uniform samplerCube skybox;

void main()
{
    gl_FragColor = texture2D(skybox, TexCoord);
}