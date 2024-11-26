package com.ileping.check_wubi

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.net.ConnectivityManager
import android.util.Base64
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var webView: WebView
    private lateinit var webAppInterface: WebAppInterface

    private var lastBackPressTime: Long = 0

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime > 2000) {
            Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show()
            lastBackPressTime = currentTime
        } else {
            super.onBackPressed()
            finish()
        }
    }

    inner class WebAppInterface(private val context: Context) {

        private fun showToast(message: String) {
            (context as Activity).runOnUiThread { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        }

        private fun checkAndRequestPermission(): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10 及以上版本使用 READ_MEDIA_IMAGES
                val permission = Manifest.permission.READ_MEDIA_IMAGES
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), PERMISSION_REQUEST_CODE)
                    return false
                }
            } else {
                // Android 9 及以下版本使用 WRITE_EXTERNAL_STORAGE
                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), PERMISSION_REQUEST_CODE)
                    return false
                }
            }
            return true
        }

        @JavascriptInterface
        fun saveImage(base64Image: String, fileName: String) {
            if (checkAndRequestPermission()) {
                // 在权限已授权的情况下保存图片
                saveImageToGallery(base64Image, fileName)
            }
        }

        private fun saveImageToGallery(base64Image: String, fileName: String) {
            try {
                val base64Data = base64Image.split(",")[1]
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                
                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    bitmap,
                    fileName,
                    "五笔字卡"
                )
                
                // 在主线程显示保存成功提示
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 显示错误提示
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 WebAppInterface
        webAppInterface = WebAppInterface(this)

        webView = WebView(this).apply {
            settings.apply {
                domStorageEnabled = true
                javaScriptEnabled = true
                blockNetworkImage = false
                allowFileAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }

            // 添加 JavaScript 接口
            addJavascriptInterface(webAppInterface, "Android")

            webViewClient = object : WebViewClient() {}
            webChromeClient = WebChromeClient()
            loadUrl("file:///android_asset/main.html")
        }

        findViewById<LinearLayout>(R.id.main_container).addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限已授予，请重新点击保存", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
     }
}
