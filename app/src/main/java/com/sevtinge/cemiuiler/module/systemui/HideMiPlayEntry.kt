package com.sevtinge.cemiuiler.module.systemui

import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge

object HideMiPlayEntry : BaseHook() {
    override fun init() {
        val miPlayPluginManagerClass =
            findClassIfExists("com.android.systemui.controlcenter.phone.controls.MiPlayPluginManager")
        XposedBridge.hookAllMethods(
            miPlayPluginManagerClass,
            "supportMiPlayAudio",
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam) {
                    param.result = false
                }
            }
        )
    }
}