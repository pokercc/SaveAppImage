package com.pokercc.saveappimage.plugin


import android.app.Activity
import android.app.AndroidAppHelper
import android.content.Context
import android.os.Environment
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import com.pokercc.saveappimage.database.AppEntityModule
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


/**
 * 插件入口
 */
class Entrance : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        XposedBridge.log("handleLoadPackage执行啦!")

        getContext(lpparam)
    }

    private fun getContext(lpparam: XC_LoadPackage.LoadPackageParam) {

        try {
            val contextClass = findClass("android.content.ContextWrapper", lpparam.classLoader)
            findAndHookMethod(contextClass, "getApplicationContext", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    param?.apply {
                        XposedBridge.log("CSDN_LQR-->得到上下文")

                        val appEntities = AppEntityModule.provideOtherProcessAppEntityDao(result as Context).queryAll()
                        val appIdSet = appEntities
                            .map { it.appId }
                            .toSet()
                        XposedBridge.log("开启的app列表:$appIdSet")
                        if (appIdSet.contains(lpparam.packageName)) {
                            XposedBridge.log("开始hook:${lpparam.packageName}")
                            hookTestApp(lpparam)
                        }
                    }


                }
            });
        } catch (t: Throwable) {
            XposedBridge.log("CSDN_LQR-->获取上下文出错");
            XposedBridge.log(t)
        }

    }

    private fun hookTestApp(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("注册activity hook")
        XposedHelpers.findAndHookMethod(Activity::class.java,
            "onResume",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val activity = param!!.thisObject as Activity
                    hookActivity(activity)
                }
            })
    }

    private fun hookActivity(activity: Activity) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val button = Button(activity)
            .apply {
                text = "保存drawable"
                setOnClickListener {
                    saveDrawable(activity)
                }
            }
        rootView.post {
            PopupWindow(
                button, 300, 200
            ).showAtLocation(rootView, Gravity.RIGHT or Gravity.BOTTOM, 0, 0)
        }
    }

    private fun saveDrawable(activity: Activity) {
        XposedBridge.log("开始保存图片")
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val subViews = rootView.allViews()
        val parentFile =
            Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
        subViews
            .filter { it is ImageView }
            .filter { it.background != null }
            .map { it.background.toBitmap() }
            .forEach { it.save(File(parentFile, UUID.randomUUID().toString() + ".png")) }
    }


}