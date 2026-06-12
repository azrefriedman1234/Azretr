package com.pasiflonet.mobile.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

object CyberUiHelper {
    fun flashUpdateButtons(activity: Activity, root: View) {
        val tags = listOf(
            "cyber_btn_current",
            "cyber_btn_verify",
            "cyber_btn_flights",
            "cyber_btn_military"
        )

        tags.forEach { tag ->
            val v = root.findViewWithTag<View>(tag)
            if (v is Button) flash(v)
        }
    }

    private fun flash(button: Button) {
        val animator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.rgb(255, 23, 68),
            Color.rgb(0, 191, 255),
            Color.rgb(255, 23, 68),
            Color.rgb(7, 26, 19)
        )
        animator.duration = 1300
        animator.addUpdateListener {
            button.setBackgroundColor(it.animatedValue as Int)
            button.setTextColor(Color.WHITE)
        }
        animator.start()
    }

    fun wireMapSearch(root: View) {
        val edit = root.findViewById<EditText>(
            root.resources.getIdentifier("etMapSearch", "id", root.context.packageName)
        )
        val map = root.findViewById<CyberMiddleEastMapView>(
            root.resources.getIdentifier("cyberMap", "id", root.context.packageName)
        )
        val btn = root.findViewById<Button>(
            root.resources.getIdentifier("btnMapSearch", "id", root.context.packageName)
        )
        btn?.setOnClickListener {
            map?.setSearchText(edit?.text?.toString().orEmpty())
        }
    }

    fun wireVerifyBox(root: View) {
        val input = root.findViewById<EditText>(
            root.resources.getIdentifier("etHomeVerify", "id", root.context.packageName)
        )
        val out = root.findViewById<TextView>(
            root.resources.getIdentifier("tvHomeVerifyResult", "id", root.context.packageName)
        )
        val btn = root.findViewById<Button>(
            root.resources.getIdentifier("btnHomeVerify", "id", root.context.packageName)
        )
        btn?.setOnClickListener {
            val text = input?.text?.toString()?.trim().orEmpty()
            out?.text = if (text.isBlank()) {
                "כתוב ידיעה לבדיקה"
            } else {
                "בדיקה מהירה:\n• ידיעה: $text\n• סטטוס: נדרש אימות מול מקורות נוספים\n• המלצה: לחץ על מסך אימות מידע לבדיקה מלאה"
            }
        }
    }
}
