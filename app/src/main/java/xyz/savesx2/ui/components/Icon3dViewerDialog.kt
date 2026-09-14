package xyz.savesx2.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.savesx2.core.Ps2Icon3dSession
import xyz.savesx2.core.Ps2IconDecoder
import xyz.savesx2.core.Ps2IconSys
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val DEFAULT_PITCH_DEG = 15.0f
private const val DEFAULT_YAW_DEG = -25.0f
private const val DEFAULT_ZOOM = 1.0f
private const val ROTATION_SPEED_DEG_PER_SEC = 35.0f
private const val IDLE_RESET_DELAY_MS = 3000L

/**
 * Native hardware-accelerated OpenGL ES 2.0 3D Save Icon Viewer.
 *
 * Behavior:
 * - Rotates to the left continuously by default using OpenGL ES 2.0.
 * - Pauses rotation immediately when touched/dragged.
 * - If no touch occurs for 3 seconds, smoothly eases pitch and zoom back to default
 *   and resumes rotating to the left.
 */
@Composable
fun Icon3dViewerDialog(
    session: Ps2Icon3dSession,
    onDismiss: () -> Unit
) {
    val glRenderer = remember(session) {
        Ps2IconGlRenderer(session.mesh, session.iconSys)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (session.subtitle.isNotBlank()) {
                                Text(
                                    text = session.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive OpenGL Canvas
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E2638),
                                    Color(0xFF0F131D)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            createGlSurfaceView(ctx, glRenderer)
                        },
                        modifier = Modifier.size(240.dp)
                    )

                    // Reset button overlay
                    IconButton(
                        onClick = {
                            glRenderer.resetView()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Angle",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Drag to rotate • Pinch to zoom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats and Controls Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "OpenGL ES 2.0",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${session.mesh.vertexCount / 3} Polys • ${session.mesh.vertexCount} Verts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { glRenderer.adjustZoom(-0.15f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { glRenderer.adjustZoom(0.15f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
private fun createGlSurfaceView(
    context: android.content.Context,
    renderer: Ps2IconGlRenderer
): GLSurfaceView {
    return GLSurfaceView(context).apply {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        var prevX = 0f
        var prevY = 0f

        val scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    renderer.zoomScale = (renderer.zoomScale * detector.scaleFactor).coerceIn(0.5f, 2.5f)
                    renderer.lastTouchTimeMs = SystemClock.uptimeMillis()
                    return true
                }
            }
        )

        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            renderer.lastTouchTimeMs = SystemClock.uptimeMillis()

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    renderer.isTouching = true
                    prevX = event.x
                    prevY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                        val dx = event.x - prevX
                        val dy = event.y - prevY
                        renderer.yawDeg = (renderer.yawDeg + dx * 0.6f) % 360f
                        renderer.pitchDeg = (renderer.pitchDeg - dy * 0.6f).coerceIn(-85f, 85f)
                    }
                    prevX = event.x
                    prevY = event.y
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    renderer.isTouching = false
                    renderer.lastTouchTimeMs = SystemClock.uptimeMillis()
                }
            }
            true
        }
    }
}

/**
 * High-performance OpenGL ES 2.0 Renderer directly rendering PS2 save icons.
 */
class Ps2IconGlRenderer(
    private val mesh: Ps2IconDecoder.ParsedIconMesh,
    private val iconSys: Ps2IconSys?
) : GLSurfaceView.Renderer {

    @Volatile var pitchDeg: Float = DEFAULT_PITCH_DEG
    @Volatile var yawDeg: Float = DEFAULT_YAW_DEG
    @Volatile var zoomScale: Float = DEFAULT_ZOOM
    @Volatile var isTouching: Boolean = false
    @Volatile var lastTouchTimeMs: Long = 0L

    private var lastFrameTimeMs: Long = 0L
    private var program: Int = 0
    private var textureId: Int = 0

    private var uMVPMatrixHandle: Int = -1
    private var uModelMatrixHandle: Int = -1
    private var uAmbientHandle: Int = -1
    private var uTextureHandle: Int = -1

    private var aPositionHandle: Int = -1
    private var aNormalHandle: Int = -1
    private var aTexCoordHandle: Int = -1
    private var aColorHandle: Int = -1

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val viewProjMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var vertexBuffer: FloatBuffer? = null

    fun resetView() {
        pitchDeg = DEFAULT_PITCH_DEG
        yawDeg = DEFAULT_YAW_DEG
        zoomScale = DEFAULT_ZOOM
        lastTouchTimeMs = 0L
        isTouching = false
    }

    fun adjustZoom(delta: Float) {
        zoomScale = (zoomScale + delta).coerceIn(0.5f, 2.5f)
        lastTouchTimeMs = SystemClock.uptimeMillis()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) return

        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        uAmbientHandle = GLES20.glGetUniformLocation(program, "uAmbient")
        uTextureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        aTexCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        aColorHandle = GLES20.glGetAttribLocation(program, "aColor")

        // Texture upload
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        textureId = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        val texBitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        texBitmap.setPixels(mesh.texPixels, 0, 128, 0, 0, 128, 128)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texBitmap, 0)
        texBitmap.recycle()

        // Vertex buffer
        vertexBuffer = mesh.vertexBuffer ?: run {
            val count = mesh.vertexCount
            val buf = ByteBuffer.allocateDirect(count * 11 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            for (i in 0 until count) {
                buf.put(mesh.posX[i])
                buf.put(mesh.posY[i])
                buf.put(mesh.posZ[i])

                buf.put(mesh.normX[i])
                buf.put(mesh.normY[i])
                buf.put(mesh.normZ[i])

                buf.put(mesh.uvU[i])
                buf.put(mesh.uvV[i])

                buf.put(mesh.colR[i])
                buf.put(mesh.colG[i])
                buf.put(mesh.colB[i])
            }
            buf.position(0)
            buf
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
        val near = 1.0f
        val far = 20.0f
        val top = (64.0f / 140.0f) * near
        val bottom = -top
        val left = -top * aspect
        val right = top * aspect
        Matrix.frustumM(projMatrix, 0, left, right, bottom, top, near, far)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrameTimeMs == 0L) 0.016f else minOf(0.05f, (now - lastFrameTimeMs) / 1000f)
        lastFrameTimeMs = now

        // Animation update
        if (!isTouching) {
            val idleMs = now - lastTouchTimeMs
            if (idleMs >= IDLE_RESET_DELAY_MS) {
                // Smooth critically-damped spring transition for pitch back to default (15°)
                pitchDeg += (DEFAULT_PITCH_DEG - pitchDeg) * minOf(1.0f, 4.0f * dt)
                // Smooth transition for zoom back to default (1.0x)
                zoomScale += (DEFAULT_ZOOM - zoomScale) * minOf(1.0f, 4.0f * dt)
                // Continuously rotate to the left
                yawDeg = (yawDeg - ROTATION_SPEED_DEG_PER_SEC * dt) % 360f
            }
        }

        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (program == 0 || vertexBuffer == null) return

        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureHandle, 0)

        // Model Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        val totalScale = mesh.scale * zoomScale
        Matrix.scaleM(modelMatrix, 0, totalScale, totalScale, totalScale)
        Matrix.translateM(modelMatrix, 0, -mesh.centerX, -mesh.centerY, -mesh.centerZ)

        // View Matrix (camera at distance 5.0 looking at origin)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5.0f, 0f, 0f, 0f, 0f, 1f, 0f)

        // MVP Matrix
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0)

        val ambR = iconSys?.ambientR?.coerceIn(0.4f, 0.8f) ?: 0.55f
        val ambG = iconSys?.ambientG?.coerceIn(0.4f, 0.8f) ?: 0.55f
        val ambB = iconSys?.ambientB?.coerceIn(0.4f, 0.8f) ?: 0.55f
        GLES20.glUniform3f(uAmbientHandle, ambR, ambG, ambB)

        val vBuf = vertexBuffer!!
        val stride = 11 * 4

        vBuf.position(0)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, stride, vBuf)
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        vBuf.position(3)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, stride, vBuf)
        GLES20.glEnableVertexAttribArray(aNormalHandle)

        vBuf.position(6)
        GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, stride, vBuf)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)

        vBuf.position(8)
        GLES20.glVertexAttribPointer(aColorHandle, 3, GLES20.GL_FLOAT, false, stride, vBuf)
        GLES20.glEnableVertexAttribArray(aColorHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)
        GLES20.glDisableVertexAttribArray(aColorHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, shaderCode.trimIndent())
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        if (vertexShader == 0) return 0
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return 0
        }

        val prog = GLES20.glCreateProgram()
        if (prog == 0) {
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return 0
        }

        GLES20.glAttachShader(prog, vertexShader)
        GLES20.glAttachShader(prog, fragmentShader)
        GLES20.glLinkProgram(prog)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uAmbient;

            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aTexCoord;
            attribute vec3 aColor;

            varying vec2 vTexCoord;
            varying vec3 vLitColor;

            const vec3 lightDir0 = vec3(0.577, 0.577, 0.577);
            const vec3 lightCol0 = vec3(0.9, 0.9, 0.85);
            const vec3 lightDir1 = vec3(-0.577, 0.577, 0.577);
            const vec3 lightCol1 = vec3(0.5, 0.55, 0.6);
            const vec3 lightDir2 = vec3(0.0, -0.707, 0.707);
            const vec3 lightCol2 = vec3(0.3, 0.3, 0.3);

            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
                vTexCoord = aTexCoord;

                vec3 normal = (uModelMatrix * vec4(aNormal, 0.0)).xyz;
                float nLen = length(normal);
                vec3 n = nLen > 0.0001 ? (normal / nLen) : vec3(0.0, 1.0, 0.0);

                vec3 light = uAmbient;
                light += max(0.0, dot(n, lightDir0)) * lightCol0;
                light += max(0.0, dot(n, lightDir1)) * lightCol1;
                light += max(0.0, dot(n, lightDir2)) * lightCol2;
                light = clamp(light, 0.0, 1.4);

                vLitColor = clamp(aColor * light, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;

            varying vec2 vTexCoord;
            varying vec3 vLitColor;

            uniform sampler2D uTexture;

            void main() {
                vec4 tex = texture2D(uTexture, vTexCoord);
                gl_FragColor = vec4(clamp(tex.rgb * vLitColor, 0.0, 1.0), tex.a);
            }
        """
    }
}
