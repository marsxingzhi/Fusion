package com.mars.infra.lib

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mars.infra.fusion.annotations.Fusion

/**
 * Created by Mars on 2022/5/9
 */
@Fusion(target = AppCompatActivity::class)
class FusionLogActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("gy", "onCreate invoke")
    }

    override fun onStart() {
        super.onStart()
        Log.e("gy", "onStart invoke")
    }

    override fun onResume() {
        super.onResume()
        Log.e("gy", "onResume invoke")
    }
}