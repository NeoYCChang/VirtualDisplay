#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES videoTex;
varying vec2 vTexCoord;
void main() {
    vec4 tc = texture2D(videoTex, vTexCoord);
    //gl_FragColor = vec4(tc.r, 0.0, 1.0, 1.0);

    gl_FragColor = tc;
}