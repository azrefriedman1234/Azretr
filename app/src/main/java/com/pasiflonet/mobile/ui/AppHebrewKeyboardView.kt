package com.pasiflonet.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.pasiflonet.mobile.R
import kotlin.math.max
import kotlin.math.min

class AppHebrewKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var targetEditText: EditText? = null

    fun setTargetEditText(editText: EditText?) {
        targetEditText = editText
    }

    private fun resolveTargetEditText(): EditText? {
        val direct = targetEditText
        if (direct != null) return direct
        val focused = rootView?.findFocus()
        return if (focused is EditText) focused else null
    }


    private var target: EditText? = null
    private val handler = Handler(Looper.getMainLooper())
    private val repeatDelete = object : Runnable {
        override fun run() {
            backspaceOnce()
            handler.postDelayed(this, 55)
        }
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_app_hebrew_keyboard, this, true)
        bindTaggedKeys(this)

        findViewById<View>(R.id.keyBackspace)?.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    feedback(v)
                    backspaceOnce()
                    handler.postDelayed(repeatDelete, 280)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(repeatDelete)
                    true
                }
                else -> false
            }
        }
    }

    fun bindTo(editText: EditText) {
        target = editText
        try { editText.showSoftInputOnFocus = false } catch (_: Exception) {}
        editText.setTextIsSelectable(true)
        editText.isLongClickable = true
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                target = editText
                visibility = View.VISIBLE
            }
        }
        editText.setOnClickListener {
            target = editText
            visibility = View.VISIBLE
        }
    }

    private fun bindTaggedKeys(root: View) {
        if (root is TextView) {
            val tagText = root.tag?.toString()
            if (!tagText.isNullOrBlank()) {
                root.setOnClickListener { onTagClicked(tagText, root) }
            }
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                bindTaggedKeys(root.getChildAt(i))
            }
        }
    }

    private fun onTagClicked(tagText: String, view: View) {
        feedback(view)
        when {
            tagText.startsWith("ins:") -> insertText(tagText.removePrefix("ins:"))
            tagText == "copy" -> copySelected()
            tagText == "paste" -> pasteClipboard()
            tagText == "left" -> moveCursor(-1)
            tagText == "right" -> moveCursor(1)
            tagText == "clear_selection" -> clearSelectionOnly()
            tagText == "enter" -> insertText("\n")
            tagText == "space" -> insertText(" ")
        }
    }

    private fun currentTarget(): EditText? = target

    private fun insertText(text: String) {
        val et = resolveTargetEditText() ?: return
        et.requestFocus()
        val editable = et.text ?: return

        val start: Int = et.selectionStart
        val end: Int = et.selectionEnd
        val selStart: Int = if (start <= end) start else end
        val selEnd: Int = if (start >= end) start else end

        editable.replace(selStart, selEnd, text)
        val newPos = selStart + text.length
        et.setSelection(if (newPos <= editable.length) newPos else editable.length)
    }

    private fun backspaceOnce() {
        val e = currentTarget() ?: return
        val editable = e.text ?: return
        val start = max(e.selectionStart, 0)
        val end = max(e.selectionEnd, 0)
        if (start != end) {
            val from = min(start, end)
            val to = max(start, end)
            editable.delete(from, to)
            e.setSelection(from.coerceAtMost(editable.length))
            return
        }
        if (start > 0 && start <= editable.length) {
            editable.delete(start - 1, start)
            e.setSelection((start - 1).coerceAtLeast(0))
        }
    }

    private fun clearSelectionOnly() {
        val e = currentTarget() ?: return
        val editable = e.text ?: return
        val start = max(e.selectionStart, 0)
        val end = max(e.selectionEnd, 0)
        if (start != end) {
            val from = min(start, end)
            val to = max(start, end)
            editable.delete(from, to)
            e.setSelection(from.coerceAtMost(editable.length))
        }
    }

    private fun moveCursor(delta: Int) {
        val e = currentTarget() ?: return
        val pos = max(e.selectionStart, 0)
        val newPos = (pos + delta).coerceIn(0, e.text?.length ?: 0)
        e.setSelection(newPos)
    }

    private fun copySelected() {
        val e = currentTarget() ?: return
        val text = e.text?.toString().orEmpty()
        val start = max(e.selectionStart, 0)
        val end = max(e.selectionEnd, 0)
        val content = if (start != end) text.substring(min(start, end), max(start, end)) else text
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("text", content))
    }

    private fun pasteClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = cm.primaryClip?.getItemAt(0) ?: return
        val pasteText = item.coerceToText(context)?.toString().orEmpty()
        if (pasteText.isNotEmpty()) insertText(pasteText)
    }

    private fun feedback(view: View) {
        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {}
    }
}
