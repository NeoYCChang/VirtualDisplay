package com.yuchen.virtualdisplay

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.yuchen.virtualdisplay.AUOGLSurfaceView.AUOVirtualDisplayService
import com.yuchen.virtualdisplay.ui.theme.VirtualDisplayTheme


class MainActivity : ComponentActivity() {

    private var lifecycleOwner = ComposeViewLifecycleOwner()
    private val REQUEST_CODE_SCREEN_CAPTURE = 1000
    private var mediaProjectionManager: MediaProjectionManager? = null
    val m_virtualdisplay_intents = ArrayList<Intent>()
    private var m_displayid : Int = 0
    private var m_virtualWidth : Int = 960
    private var m_virtualHeight : Int = 540
    private val m_tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//Kotlin Popup Window Example
//        val composeView: ComposeView = ComposeView(this).apply {
//            setContent {
//                Text(text = "I'm be added")
//            }
//        }
//
//        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val lp = WindowManager.LayoutParams()
//        //Note，before calling addView
//        lifecycleOwner.onCreate()
//        lifecycleOwner.attachToDecorView(composeView)
//
//        wm.addView(composeView, lp)


        // start requesting screen recording permission when user is ready
        startScreenCapturePermission()
//        WindowCompat.setDecorFitsSystemWindows(window, true)
//        window.decorView.systemUiVisibility =
//            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
//                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
//                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        enableEdgeToEdge()
        setContent {
            VirtualDisplayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavigationExample(inputmodifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                            ,onCreateVirtualDisplay = { param ->
                        createProjectionVirtualDisplay(param)
                    },
                        onCreateVirtualView = { param ->
                            createProjectionVirtualView(param)
                        })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleOwner.onResume()
    }

    override fun onPause() {
        super.onPause()
        lifecycleOwner.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        for (intent in m_virtualdisplay_intents) {
            stopService(intent)
        }
        //windowManager.removeViewImmediate(composeView)
        lifecycleOwner.onDestroy()
    }

    // start Intent to request screen recording permission
    private fun startScreenCapturePermission() {
        // Initialize MediaProjectionManager
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
        if (captureIntent != null) {
            startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
        }
    }

    // callback after the user grants or denies screen recording permission
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                AUOVirtualDisplayService.resultData = data
                AUOVirtualDisplayService.resultCode = resultCode
                // start virtual display service
                //val virtualdisplay_intent = Intent(this, VirtualDisplayService::class.java)
                //val virtualdisplay_intent = Intent(this, CustomVirtualDisplayService::class.java)
                val virtualdisplay_intent = Intent(this, AUOVirtualDisplayService::class.java)
                val bundle = Bundle()
                bundle.putInt("displayID", m_displayid)
                bundle.putInt("viewWidth", m_virtualWidth)
                bundle.putInt("viewHeight", m_virtualHeight)
                virtualdisplay_intent.putExtras(bundle);
                startForegroundService(virtualdisplay_intent)
                m_virtualdisplay_intents.add(virtualdisplay_intent)
            } else {
            }
        }
    }

    fun createProjectionVirtualDisplay(param: VirtualViewParameter) {
        val intent = Intent("com.yuchen.virtualdisplay.UPDATE_DATA")
        intent.putExtra("primary", true)
        intent.putExtra("displayID", param.id)
        intent.putExtra("displayWidth", param.displayWidth)
        intent.putExtra("displayHeight", param.displayHeight)
        intent.putExtra("viewWidth", param.viewWidth)
        intent.putExtra("viewHeight", param.viewHeight)
        intent.putExtra("viewX", param.viewX)
        intent.putExtra("viewY", param.viewY)
        intent.putExtra("textureCropWidth", param.textureCropWidth)
        intent.putExtra("textureCropHeight", param.textureCropHeight)
        intent.putExtra("textureOffsetX", param.textureOffsetX)
        intent.putExtra("textureOffsetY", param.textureOffsetY)
        intent.putExtra("isDeWarp", param.isDeWarp)

        sendBroadcast(intent)
        Log.d("m_tag","createProjectionVirtualDisplay")
    }
    fun createProjectionVirtualView(param: VirtualViewParameter) {
        val intent = Intent("com.yuchen.virtualdisplay.UPDATE_DATA")
        intent.putExtra("primary", false)
        intent.putExtra("displayID", param.id)
        intent.putExtra("viewWidth", param.viewWidth)
        intent.putExtra("viewHeight", param.viewHeight)
        intent.putExtra("viewX", param.viewX)
        intent.putExtra("viewY", param.viewY)
        intent.putExtra("textureCropWidth", param.textureCropWidth)
        intent.putExtra("textureCropHeight", param.textureCropHeight)
        intent.putExtra("textureOffsetX", param.textureOffsetX)
        intent.putExtra("textureOffsetY", param.textureOffsetY)
        intent.putExtra("isDeWarp", param.isDeWarp)
        sendBroadcast(intent)
        Log.d("m_tag","createProjectionVirtualView")
    }
}
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationExample(inputmodifier: Modifier, onCreateVirtualDisplay: (VirtualViewParameter) -> Unit,
                      onCreateVirtualView: (VirtualViewParameter) -> Unit) {
    // Create a NavController to manage navigation
    val navController = rememberNavController()

    // Wrap the NavHost in a Box and center it
    Box(
        modifier = inputmodifier
    ) {
        // Define NavHost with transition animations
        AnimatedNavHost(
            navController = navController,
            startDestination = "screen1"
        ) {
            composable("screen1",
                enterTransition = {
                    scaleIn(
                        initialScale = 0.1f, // Start small (scaled down)
                        animationSpec = tween(durationMillis = 500)
                    )
                },
                exitTransition = {
                    scaleOut(
                        targetScale = 0.1f, // Scale up when exiting
                        animationSpec = tween(durationMillis = 500)
                    )
                },
                // Adding popEnter and popExit transitions for back navigation
                popEnterTransition = {
                    scaleIn(
                        initialScale = 0.1f, // Start large when navigating back
                        animationSpec = tween(durationMillis = 500)
                    )
                },
                popExitTransition = {
                    scaleOut(
                        targetScale = 0.1f, // Scale down when exiting backward
                        animationSpec = tween(durationMillis = 500)
                    )
                }) {
                VirtualControl(navController, onCreateVirtualDisplay, onCreateVirtualView)
            }
            composable("screen2",
                enterTransition = {
                    scaleIn(
                        initialScale = 0.1f, // Start small (scaled down)
                        animationSpec = tween(durationMillis = 500)
                    )
                },
                exitTransition = {
                    scaleOut(
                        targetScale = 0.1f, // Scale up when exiting
                        animationSpec = tween(durationMillis = 500)
                    )
                },
                // Adding popEnter and popExit transitions for back navigation
                popEnterTransition = {
                    scaleIn(
                        initialScale = 0.1f, // Start large when navigating back
                        animationSpec = tween(durationMillis = 500)
                    )
                },
                popExitTransition = {
                    scaleOut(
                        targetScale = 0.1f, // Scale down when exiting backward
                        animationSpec = tween(durationMillis = 500)
                    )
                }) {
                Screen2(navController)
            }
        }
    }
}

//@Composable
//fun Screen1(navController: NavController, onStartScreenCapture: () -> Unit) {
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(text = "This is Screen 1")
//        Button(onClick = {
//            // Navigate to Screen2 with a slide animation
//            //navController.navigate("screen2")
//            onStartScreenCapture()ㄘ
//        }) {
//            Text(text = "Go to Screen 2")
//        }
//    }
//}

data class VirtualViewParameter(var id: Int, var displayWidth: Int, var displayHeight: Int, var viewWidth: Int, var viewHeight: Int, var viewX: Int, var viewY: Int,
                                var textureOffsetX: Int, var textureOffsetY: Int, var textureCropWidth: Int, var textureCropHeight: Int, var isDeWarp: Boolean)

@Composable
fun VirtualControl(navController: NavController, onCreateVirtualDisplay: (VirtualViewParameter) -> Unit,
                   onCreateVirtualView: (VirtualViewParameter) -> Unit) {
    val u_ViewParameters = remember { mutableStateListOf<MutableState<VirtualViewParameter>>() }
    val u_count_views = remember { mutableStateOf<Int>(0) }
    val u_primaryCreated = remember { mutableStateOf<Boolean>(false) }
    // Create a vertical scroll modifier
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState), // Make the Column scrollable,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.hsv(106f, 0.43f, 0.89f)),
            horizontalAlignment = Alignment.Start)
        {
            Text(text = "Virtual Display")
            VirtualDisplay(navController, u_ViewParameters)
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(3.dp),
            color = Color.Black,
        )
        for (i in 1..u_count_views.value) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.hsv(23f, 0.43f, 0.89f)),
                horizontalAlignment = Alignment.Start)
            {
                Text(text = "View: " + i.toString())
                VirtualView(navController, u_ViewParameters)
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(3.dp),
                color = Color.Black,
            )
        }




        IconButton(
            onClick = {
                if (u_ViewParameters.size > u_count_views.value) {
                    if (!u_primaryCreated.value) {
                        onCreateVirtualDisplay(u_ViewParameters[u_ViewParameters.size - 1].value)
                        u_primaryCreated.value = true
                    } else {
                        onCreateVirtualView(u_ViewParameters[u_ViewParameters.size - 1].value)
                    }
                    u_count_views.value = u_count_views.value + 1
                }
            },
            modifier = Modifier
                .padding(10.dp)
                .size(40.dp),
        ) {
            Icon(bitmap = ImageBitmap.imageResource(id = R.drawable.add),
                contentDescription = "add.icon",
                modifier = Modifier.size(40.dp),
                tint = Color.Blue)
        }

    }
}

@Composable
fun VirtualDisplay(navController: NavController, viewParams: MutableList<MutableState<VirtualViewParameter>>) {
    val availableDisplays = remember { mutableStateListOf<String>() }
    var expanded = remember { mutableStateOf(false) }
    val u_ViewParameter = remember { mutableStateOf<VirtualViewParameter>(
        VirtualViewParameter(0,960,540,960,540, 0, 0,
                            0,0,960,540, false)
    ) }
    viewParams.addAll(listOf(u_ViewParameter))

    val context = LocalContext.current
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager



    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Select a Display ID")

        // Dropdown menu for display ID selection
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    expanded.value = !expanded.value
                    // Get available displays
                        val displays = displayManager.getDisplays()
                        availableDisplays.clear()
                        availableDisplays.addAll(displays.map { it.displayId.toString() })
                    },
                modifier = Modifier
                    .fillMaxWidth(1.0f)
                    .align(Alignment.TopCenter)

            ) {
                Text(text = "Select Display ID: " + u_ViewParameter.value.id.toString())
                // Dropdown menu
                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    availableDisplays.forEach { displayID ->
                        DropdownMenuItem(
                            onClick = {
                                u_ViewParameter.value = u_ViewParameter.value.copy(id = displayID.toInt())
                                expanded.value = false
                            },
                            text = {Text(text = displayID, modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center)},
                        )
                    }
                }
            }
        }

        TwoOfTextField(
            leftvalue = u_ViewParameter.value.displayWidth.toString(),
            leftlabel = "display width",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(displayWidth = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(displayWidth = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.displayHeight.toString(),
            rightlabel = "display height",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(displayHeight = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(displayHeight = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )

        TwoOfTextField(
            leftvalue = u_ViewParameter.value.viewX.toString(),
            leftlabel = "view x",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewX = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewX = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.viewY.toString(),
            rightlabel = "view y",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewY = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewY = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )

        TwoOfTextField(
            leftvalue = u_ViewParameter.value.viewWidth.toString(),
            leftlabel = "view width",
            leftonValueChange =  {newText ->
            if(newText.isNotEmpty()) {
                if (newText.all { it.isDigit() }) {
                    if (newText.toInt() < 10000) {
                        u_ViewParameter.value = u_ViewParameter.value.copy(viewWidth = newText.toInt())
                    } else {
                        u_ViewParameter.value = u_ViewParameter.value.copy(viewWidth = 10000)
                    }
                }
            }
        },
            rightvalue = u_ViewParameter.value.viewHeight.toString(),
            rightlabel = "view height",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewHeight = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewHeight = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )

        TwoOfTextField(
            leftvalue = u_ViewParameter.value.textureOffsetX.toString(),
            leftlabel = "texture x offset",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetX = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetX = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.textureOffsetY.toString(),
            rightlabel = "texture y offset",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetY = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetY = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )
        TwoOfTextField(
            leftvalue = u_ViewParameter.value.textureCropWidth.toString(),
            leftlabel = "texture width crop",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropWidth = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropWidth = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.textureCropHeight.toString(),
            rightlabel = "texture height crop",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropHeight = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropHeight = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("dewarp?")
            Checkbox(
                checked = u_ViewParameter.value.isDeWarp,
                onCheckedChange = { checked ->
                    u_ViewParameter.value = u_ViewParameter.value.copy(isDeWarp = checked)
                }
            )
        }

    }
}

@Composable
fun VirtualView(navController: NavController, viewParams: MutableList<MutableState<VirtualViewParameter>>) {
    val availableDisplays = remember { mutableStateListOf<String>() }
    var expanded = remember { mutableStateOf(false) }
    val u_ViewParameter = remember { mutableStateOf<VirtualViewParameter>(
        VirtualViewParameter(0,960,540,960,540, 0, 0,
            0,0,960,540, false)
    ) }
    viewParams.addAll(listOf(u_ViewParameter))

    val context = LocalContext.current
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager



    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Select a Display ID")

        // Dropdown menu for display ID selection
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    expanded.value = !expanded.value
                    // Get available displays
                    val displays = displayManager.getDisplays()
                    availableDisplays.clear()
                    availableDisplays.addAll(displays.map { it.displayId.toString() })
                },
                modifier = Modifier
                    .fillMaxWidth(1.0f)
                    .align(Alignment.TopCenter)

            ) {
                Text(text = "Select Display ID: " + u_ViewParameter.value.id.toString())
                // Dropdown menu
                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    availableDisplays.forEach { displayID ->
                        DropdownMenuItem(
                            onClick = {
                                u_ViewParameter.value = u_ViewParameter.value.copy(id = displayID.toInt())
                                expanded.value = false
                            },
                            text = {Text(text = displayID, modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center)},
                        )
                    }
                }
            }
        }


        TwoOfTextField(
            leftvalue = u_ViewParameter.value.viewX.toString(),
            leftlabel = "view x",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewX = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewX = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.viewY.toString(),
            rightlabel = "view y",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewY = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewY = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )

        TwoOfTextField(
            leftvalue = u_ViewParameter.value.viewWidth.toString(),
            leftlabel = "view width",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewWidth = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewWidth = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.viewHeight.toString(),
            rightlabel = "view height",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewHeight = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(viewHeight = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )

        TwoOfTextField(
            leftvalue = u_ViewParameter.value.textureOffsetX.toString(),
            leftlabel = "texture x offset",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetX = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetX = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.textureOffsetY.toString(),
            rightlabel = "texture y offset",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetY = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureOffsetY = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )
        TwoOfTextField(
            leftvalue = u_ViewParameter.value.textureCropWidth.toString(),
            leftlabel = "texture width crop",
            leftonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropWidth = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropWidth = 10000)
                        }
                    }
                }
            },
            rightvalue = u_ViewParameter.value.textureCropHeight.toString(),
            rightlabel = "texture height crop",
            rightonValueChange =  {newText ->
                if(newText.isNotEmpty()) {
                    if (newText.all { it.isDigit() }) {
                        if (newText.toInt() < 10000) {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropHeight = newText.toInt())
                        } else {
                            u_ViewParameter.value = u_ViewParameter.value.copy(textureCropHeight = 10000)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .padding(vertical = 5.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("dewarp?")
            Checkbox(
                checked = u_ViewParameter.value.isDeWarp,
                onCheckedChange = { checked ->
                    u_ViewParameter.value = u_ViewParameter.value.copy(isDeWarp = checked)
                }
            )
        }


    }
}

@Composable
fun TwoOfTextField(
    leftvalue: String,
    leftonValueChange: (String) -> Unit,
    leftlabel: String,
    rightvalue: String,
    rightonValueChange: (String) -> Unit,
    rightlabel: String,
    modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
    ) {
        TextField(
            value = leftvalue,
            onValueChange = leftonValueChange,
            label = { Text(text = leftlabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.45f)
        )
        Divider(
            modifier = Modifier
                .height(55.dp)
                .padding(horizontal = 8.dp)
                .width(3.dp),
            color = Color.Black
        )

        TextField(
            value = rightvalue,
            onValueChange = rightonValueChange,
            label = { Text(text = rightlabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.45f)
        )
    }
}

@Composable
fun Screen2(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "This is Screen 2")
        Button(onClick = {
            // Navigate back to Screen1 with a slide animation
            navController.navigate("screen1")
        }) {
            Text(text = "Back to Screen 1")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,

    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VirtualDisplayTheme {
        Greeting("Android")
    }
}



