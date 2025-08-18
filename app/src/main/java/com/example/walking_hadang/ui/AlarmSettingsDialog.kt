package com.example.walking_hadang.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.walking_hadang.R

class AlarmSettingsDialog : DialogFragment(R.layout.dialog_alarm_settings) {

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //닫기 버튼
        val btnClose = view.findViewById<View>(R.id.btnClose)
        btnClose?.setOnClickListener {
            dismiss()  // 다이얼로그 닫기
        }
    }
}