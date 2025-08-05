package com.example.vehicletracker.util

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

object DevicePromptUtil {
    fun promptDeviceName(context: Context, onNameEntered: (String) -> Unit) {
        val editText = EditText(context)
        AlertDialog.Builder(context)
            .setTitle("새 기기 등록")
            .setMessage("기기 이름을 입력하세요:")
            .setView(editText)
            .setPositiveButton("등록") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) onNameEntered(name)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
