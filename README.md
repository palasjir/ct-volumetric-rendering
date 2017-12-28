# Volume rendering using ray casting algorithm

Demo application applying volumetric rendering on CT scans of human skull.

This project implements the volume rendering using Raycasting algorithm with modern OpenGL API and the shading language GLSL using [JOGL](https://jogamp.org/jogl/www/) and [Kotlin](https://kotlinlang.org/).

## Screenshot
![screenshot](./readme/screenshot.png | width=500)

## Run instructions
To run application your graphic card needs to support OpenGL 3.3 and higher.

```bash
./gradlew run
```

## References
[Volume_Rendering_Using_GLSL](https://github.com/toolchainX/Volume_Rendering_Using_GLSL)

## Volume data source
[The Stanford volume data archive](http://www.graphics.stanford.edu/data/voldata/)