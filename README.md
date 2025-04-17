# Virtual Display
1.  SurfaceTexture will hold the content of the Virtual Display.
2.  SurfaceViews will render the content of the SurfaceTexture and display it on the physical screen.
3.  SurfaceViews can crop and dewarp the SurfaceTexture before displaying it.
4.  The H.265 stream is transmitted to clients(https://github.com/NeoYCChang/MirrorProjection) through a WebSocket connection.

The first click initializes a VirtualDisplay and a SurfaceView to display its content, as well as an H.265 encoder and a WebSocket server.

![Before_click.png](https://github.com/NeoYCChang/VirtualDisplay/blob/master/IMG/Before_click.png)

![After_click.png](https://github.com/NeoYCChang/VirtualDisplay/blob/master/IMG/After_click.png)

On subsequent clicks, only a SurfaceView is created to display the content of the VirtualDisplay.