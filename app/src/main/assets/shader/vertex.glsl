attribute vec4 vPosition;
attribute vec2 vCoordinate;
varying vec2 vTexCoord;

void main (void) {
    gl_Position = vPosition;
    vTexCoord = vCoordinate;
}