package com.smartglassesmanager.androidsmartphone.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class ManualLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)

    init {
        // 初始化生命周期状态（通常为INITIALIZED或者CREATED，具体取决于你的应用需求）
        registry.currentState = Lifecycle.State.INITIALIZED
    }

    override fun getLifecycle(): Lifecycle {
        return registry
    }

    // 在适当的时候调用这些方法来更新Lifecycle状态
    fun onStart() {
        registry.currentState = Lifecycle.State.STARTED
    }

    fun onResume() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun onPause() {
        registry.currentState = Lifecycle.State.STARTED
    }

    fun onStop() {
        registry.currentState = Lifecycle.State.CREATED
    }

    fun onDestroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
