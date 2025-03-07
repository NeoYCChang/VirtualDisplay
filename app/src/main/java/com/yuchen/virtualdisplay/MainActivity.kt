package com.yuchen.virtualdisplay

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yuchen.virtualdisplay.GLVirtualDisplay.CustomVirtualDisplayService
import com.yuchen.virtualdisplay.ui.theme.VirtualDisplayTheme


class MainActivity : ComponentActivity() {

    private var lifecycleOwner = ComposeViewLifecycleOwner()
    private val REQUEST_CODE_SCREEN_CAPTURE = 1000
    private var mediaProjectionManager: MediaProjectionManager? = null
    val m_virtualdisplay_intents = ArrayList<Intent>()

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

        enableEdgeToEdge()
        setContent {
            VirtualDisplayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
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
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
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
                VirtualDisplayService.resultData = data
                VirtualDisplayService.resultCode = resultCode
                // start virtual display service
                //val virtualdisplay_intent = Intent(this, VirtualDisplayService::class.java)
                val virtualdisplay_intent = Intent(this, CustomVirtualDisplayService::class.java)
                val bundle = Bundle()
                bundle.putInt("displayID", 0)
                bundle.putInt("viewWidth", 960)
                bundle.putInt("viewHeight", 540)
                virtualdisplay_intent.putExtras(bundle);
                startForegroundService(virtualdisplay_intent)
                m_virtualdisplay_intents.add(virtualdisplay_intent)
                Toast.makeText(this, "Screen Capture Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Screen Capture Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VirtualDisplayTheme {
        Greeting("Android")
    }
}



