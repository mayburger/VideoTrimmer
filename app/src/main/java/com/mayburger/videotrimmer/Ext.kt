package com.mayburger.videotrimmer

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt


fun Int.toDp(context: Context):Int{
    val r: Resources = context.resources
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), r.displayMetrics
    ).roundToInt()
}