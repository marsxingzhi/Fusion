package com.mars.infra.lib

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mars.infra.fusion.annotations.Fusion

/**
 * Created by Mars on 2022/5/9
 *
 * 需要考虑在Fusion-Class类的某个方法中调用该类的其他方法情况
 */
@Fusion(target = AppCompatActivity::class)
class FusionLogActivity : AppCompatActivity() {

//    private var name: String? = null
//    private var age = 18
//    private val tag = "FusionLogActivity"

//    var versionName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("mars", "onCreate invoke")
        execute()
    }


    override fun onResume() {
        super.onResume()
        Log.e("mars", "onResume invoke")
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.e("mars", "onDestroy invoke")
    }


    /**
     * execute方法内调用checkLoginStatus方法，方法体指令如下：
     * ALOAD_0
     * LDC "execute"
     * MethodInsnNode(INVOKESPECIAL, "com/mars/infra/lib/FusionLogActivity", "checkLoginStatus", "(Ljava/lang/String;)V")
     *
     */
    private fun execute() {
        Thread.sleep(100)
        checkLoginStatus("execute")
    }

    protected fun action() {
        val t = Thread { println("打印一条语句：action") }
        t.start()
    }

    private fun checkLoginStatus(status: String) {
        Log.e("mars", "检查登录状态, status = $status")
    }
}