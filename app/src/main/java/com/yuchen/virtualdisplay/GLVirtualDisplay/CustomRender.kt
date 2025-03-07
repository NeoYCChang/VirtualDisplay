package com.yuchen.virtualdisplay.GLVirtualDisplay

import android.content.Context
import android.content.res.Resources
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


interface CustomRenderCallback {
    fun onSurfaceCreatedCallback()
    fun onDrawFrameCallback()
}

// Define a class that implements GLSurfaceView.Renderer
class CustomRender : GLSurfaceView.Renderer {

    private var customRenderCallback: CustomRenderCallback? = null
    private var mProgram: Int = 0
    private var m_context: Context? = null
    private var m_positionHandle: Int = 0
    private var m_texCoordHandle: Int = 0
    private var mTextureID: Int  = -1
    // Set up shaders and program (vertex shader and fragment shader)


    constructor(context : Context, textureid : Int) : super() {
        m_context = context
        mTextureID = textureid
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        //GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        // Initialize OpenGL settings (e.g., set the background color)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f) // Set background color to black
        GLES20.glEnable(GLES20.GL_DEPTH_TEST) // Enable depth testing
        if(mTextureID  == -1){
            mTextureID = createOESTextureObject()
        }
        customRenderCallback?.onSurfaceCreatedCallback()
        createProgram(m_context!!.resources, "shader/vertex.glsl", "shader/fragment.glsl")
        activeHandle()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        // Adjust the viewport based on the new surface size
        GLES20.glViewport(0, 0, width, height)
    }

    // Implement the onDrawFrame() method
    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        // Clear the screen with the background color
        activeProgram()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f) // Set background color to black
        customRenderCallback?.onDrawFrameCallback()
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID)
        // Your OpenGL rendering code goes here
        Log.d("onDrawFrame", "onDrawFrame")
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        GLES20.glFinish();
    }

    fun setCustomRenderCallback(callback: CustomRenderCallback) {
        customRenderCallback = callback
    }

    private fun createProgram(res: Resources, vertexResPath: String, fragmentResPath: String) {
        createProgram(loadShaderSrcFromAssetFile(res, vertexResPath), loadShaderSrcFromAssetFile(res, fragmentResPath))
    }

    private fun loadShaderSrcFromAssetFile(resources: Resources, shaderNamePath: String?): String? {
        val result = java.lang.StringBuilder()
        try {
            val stream = resources.assets.open(shaderNamePath!!)
            var ch: Int
            val buffer = ByteArray(1024)
            while (-1 != (stream.read(buffer).also { ch = it })) {
                result.append(String(buffer, 0, ch))
            }
        } catch (e: java.lang.Exception) {
            return null
        }
        //        return result.toString().replaceAll("\\r\\n","\n");
        return result.toString().replace("\\r\\n".toRegex(), "\n")
    }

    private fun createProgram(vertexSrcCode: String?, fragSrcCode: String?) {
        //通常做法
//            String vertexSource = AssetsUtils.read(CameraGlSurfaceShowActivity.this, "vertex_texture.glsl");
//            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
//            String fragmentSource = AssetsUtils.read(CameraGlSurfaceShowActivity.this, "fragment_texture.glsl");
//            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexSrcCode!!)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragSrcCode!!)

        // 创建空的OpenGL ES程序
        mProgram = GLES20.glCreateProgram()


        // 添加顶点着色器到程序中
        GLES20.glAttachShader(mProgram, vertexShader)

        // 添加片段着色器到程序中
        GLES20.glAttachShader(mProgram, fragmentShader)

        // 创建OpenGL ES程序可执行文件
        GLES20.glLinkProgram(mProgram)
    }

//    private fun loadShader(type: Int, shaderCode: String): Int {
//        val shader = GLES20.glCreateShader(type)
//        // 添加上面编写的着色器代码并编译它
//        GLES20.glShaderSource(shader, shaderCode)
//        GLES20.glCompileShader(shader)
//        return shader
//    }

    private fun loadShader(type: Int, srcCode: String?): Int {
        // 创建shader
        var shader = GLES20.glCreateShader(type)
        // 加载源代码
        GLES20.glShaderSource(shader, srcCode)
        // 编译shader
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        // 查看编译状态
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(
                "test", ("Could not compile shader:" + shader
                        + " type = " + (if (type == GLES20.GL_VERTEX_SHADER) "GL_VERTEX_SHADER" else "GL_FRAGMENT_SHADER"))
            )
            Log.e("test", "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    //添加程序到ES环境中
    private fun activeProgram() {
        // 将程序添加到OpenGL ES环境
        GLES20.glUseProgram(mProgram)

        val vertices = floatArrayOf(
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
        )

        val textcoods = floatArrayOf(
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f
        )

        // Create direct ByteBuffers for vertices and texture coordinates
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)


        val textcoodBuffer = ByteBuffer.allocateDirect(textcoods.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        textcoodBuffer.put(textcoods)
        textcoodBuffer.position(0)


        GLES20.glEnableVertexAttribArray(m_positionHandle)
        GLES20.glEnableVertexAttribArray(m_texCoordHandle)
        GLES20.glVertexAttribPointer(m_positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glVertexAttribPointer(m_texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textcoodBuffer)
    }

    private fun activeHandle() {
        // Set up attributes and uniforms (code omitted for brevity)
        m_positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        m_texCoordHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate")
    }

    fun getTexture() : Int {
        return mTextureID
    }

    private fun createOESTextureObject() : Int {
        val tex = IntArray(1)

        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0)

        //将此纹理绑定到外部纹理上
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])

        //设置纹理过滤参数
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return tex[0]
    }
}
