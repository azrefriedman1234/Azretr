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
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.pasiflonet.mobile.R

class AppHebrewKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var targetEditText: EditText? = null

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
        targetEditText = editText

        try {
            editText.showSoftInputOnFocus = false
        } catch (_: Exception) {
        }

        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.isClickable = true
        editText.isLongClickable = true

        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                targetEditText = editText
                visibility = View.VISIBLE
                hideSystemKeyboard(v)
            }
        }

        editText.setOnClickListener { v ->
            targetEditText = editText
            visibility = View.VISIBLE
            hideSystemKeyboard(v)
        }
    }

    private fun hideSystemKeyboard(v: View) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        } catch (_: Exception) {
        }
    }

    private fun resolveTargetEditText(): EditText? {
        val direct = targetEditText
        if (direct != null) return direct
        val focused = rootView?.findFocus()
        return focused as? EditText
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

    private fun insertText(text: String) {
        val et = resolveTargetEditText() ?: return
        et.requestFocus()

        val editable = et.text ?: return
        val start = et.selectionStart.coerceAtLeast(0)
        val end = et.selectionEnd.coerceAtLeast(0)

        val selStart = if (start <= end) start else end
        val selEnd = if (start >= end) start else end

        editable.replace(selStart, selEnd, text)
        val newPos = selStart + text.length
        et.setSelection(newPos.coerceAtMost(editable.length))
    }

    private fun backspaceOnce() {
        val e = resolveTargetEditText() ?: return
        e.requestFocus()

        val editable = e.text ?: return
        val start = e.selectionStart.coerceAtLeast(0)
        val end = e.selectionEnd.coerceAtLeast(0)

        if (start != end) {
            val from = if (start <= end) start else end
            val to = if (start >= end) start else end
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
        val e = resolveTargetEditText() ?: return
        val editable = e.text ?: return

        val start = e.selectionStart.coerceAtLeast(0)
        val end = e.selectionEnd.coerceAtLeast(0)

        if (start != end) {
            val from = if (start <= end) start else end
            val to = if (start >= end) start else end
            editable.delete(from, to)
            e.setSelection(from.coerceAtMost(editable.length))
        }
    }

    private fun moveCursor(delta: Int) {
        val e = resolveTargetEditText() ?: return
        e.requestFocus()

        val start = e.selectionStart.coerceAtLeast(0)
        val end = e.selectionEnd.coerceAtLeast(0)

        val base = if (delta < 0) {
            if (start <= end) start else end
        } else {
            if (start >= end) start else end
        }

        val textLen = e.text?.length ?: 0
        val newPos = (base + delta).coerceIn(0, textLen)
        e.setSelection(newPos)
    }

    private fun copySelected() {
        val e = resolveTargetEditText() ?: return
        val text = e.text?.toString().orEmpty()

        val start = e.selectionStart.coerceAtLeast(0)
        val end = e.selectionEnd.coerceAtLeast(0)

        val content = if (start != end) {
            val from = if (start <= end) start else end
            val to = if (start >= end) start else end
            text.substring(from, to)
        } else {
            text
        }

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("text", content))
    }

    private fun pasteClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = cm.primaryClip?.getItemAt(0) ?: return
        val pasteText = item.coerceToText(context)?.toString().orEmpty()
        if (pasteText.isNotEmpty()) {
            insertText(pasteText)
        }
    }

    private fun feedback(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {
        }
    }
}
