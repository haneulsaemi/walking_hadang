package com.example.walking_hadang.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.walking_hadang.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.transition.platform.MaterialSharedAxis

class AlarmSettingsDialog : DialogFragment(R.layout.dialog_alarm_settings) {
    private lateinit var panelList: View
    private lateinit var panelEditor: View
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

        panelList = view.findViewById(R.id.panelList)
        panelEditor = view.findViewById(R.id.panelEditor)

        view.findViewById<FloatingActionButton>(R.id.fabAddAlarm).setOnClickListener {
            showEditor()
        }
        view.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            showList()
        }
        view.findViewById<MaterialButton>(R.id.btnOk).setOnClickListener {
            // TODO: 입력값 검증/저장 후
            showList()
        }

        // 시간 입력 클릭 → TimePicker
        val etTime = view.findViewById<TextInputEditText>(R.id.etTime)
        etTime.setOnClickListener { openTimePicker(etTime) }

    }
    private fun showEditor() {
        animateSwap(showEditor = true)
    }
    private fun showList() {
        animateSwap(showEditor = false)
    }

    private fun animateSwap(showEditor: Boolean) {
        // 간단히 visibility만 토글해도 OK
        // MaterialSharedAxis로 부드럽게
        val axis = MaterialSharedAxis(MaterialSharedAxis.Y, showEditor)
        TransitionManager.beginDelayedTransition(requireView() as ViewGroup, axis)
        panelList.visibility = if (showEditor) View.GONE else View.VISIBLE
        panelEditor.visibility = if (showEditor) View.VISIBLE else View.GONE
    }

    private fun openTimePicker(target: TextInputEditText) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H) // 24H면 CLOCK_24H
            .setHour(7).setMinute(0).build()
        picker.addOnPositiveButtonClickListener {
            val h = picker.hour
            val m = picker.minute
            target.setText(String.format("%02d:%02d", h, m))
        }
        picker.show(parentFragmentManager, "time")
    }

}