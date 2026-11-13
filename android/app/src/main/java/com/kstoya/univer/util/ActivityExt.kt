package com.kstoya.univer.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx != null) {
        if (ctx is Activity) return ctx
        ctx = (ctx as? ContextWrapper)?.baseContext
    }
    return null
}
