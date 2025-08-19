package com.example.walking_hadang.ui

import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walking_hadang.R
import com.example.walking_hadang.adapter.AlarmAdapter
import com.example.walking_hadang.data.Alarm
import com.example.walking_hadang.util.DBHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.transition.platform.MaterialSharedAxis

class AlarmSettingsDialog : DialogFragment(R.layout.dialog_alarm_settings) {
    private lateinit var panelList: View
    private lateinit var panelEditor: View
    private lateinit var fab: FloatingActionButton

    private lateinit var adapter: AlarmAdapter

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
        fab = view.findViewById(R.id.fabAddAlarm)

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

        val etTitle = view.findViewById<TextInputEditText>(R.id.etLabel)
        // 시간 입력 클릭 → TimePicker
        val etTime = view.findViewById<TextInputEditText>(R.id.etTime)
        etTime.setOnClickListener { openTimePicker(etTime) }

        //알림 저장
        val btnOk = view.findViewById<MaterialButton>(R.id.btnOk)
        btnOk.setOnClickListener {
            val titleText = etTitle.text.toString()
            val timeText = etTime.text.toString()

            if (titleText.isBlank() || timeText.isBlank()) {
                Toast.makeText(requireContext(), "제목과 시간을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val parts = timeText.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            insertAlarm(hour, minute, titleText, "")
            refreshAlarms()
            showList()
        }
        showList()
    }
    fun insertAlarm(hour: Int, minute: Int, title: String, message: String): Long {
        val db = DBHelper(requireContext()).writableDatabase
        val values = ContentValues().apply {
            put("hour", hour)
            put("minute", minute)
            put("title", title)
            put("message", message)
        }
        return db.insert("ALARM_TB", null, values)
    }
    fun getAllAlarms(): List<Alarm> {
        val db = DBHelper(requireContext()).readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ALARM_TB ORDER BY hour, minute", null)
        val alarms = mutableListOf<Alarm>()

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
            val hour = cursor.getInt(cursor.getColumnIndexOrThrow("hour"))
            val minute = cursor.getInt(cursor.getColumnIndexOrThrow("minute"))
            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
            val message = cursor.getString(cursor.getColumnIndexOrThrow("message"))

            alarms.add(Alarm(id, hour, minute, title, message))
        }
        cursor.close()
        return alarms
    }

    fun refreshAlarms() {
        val rv = view?.findViewById<RecyclerView>(R.id.rvAlarms)
        val updatedList = getAllAlarms()
        adapter = AlarmAdapter(updatedList) { alarmId ->
            deleteAlarm(alarmId)
            refreshAlarms()
        }
        rv?.layoutManager = LinearLayoutManager(requireContext())

        rv?.adapter = adapter
    }
    fun deleteAlarm(id: Int): Int {
        val db = DBHelper(requireContext()).writableDatabase
        return db.delete("ALARM_TB", "_id = ?", arrayOf(id.toString()))
    }




    private fun showEditor() {
        animateSwap(showEditor = true)
    }
    private fun showList() {
        animateSwap(showEditor = false)
        val rv = view?.findViewById<RecyclerView>(R.id.rvAlarms)
        val alarmList = getAllAlarms()
        adapter = AlarmAdapter(alarmList) { alarmId ->
            deleteAlarm(alarmId)
            refreshAlarms() // 삭제 후 목록 새로고침
        }
        rv?.adapter = adapter
        rv?.layoutManager = LinearLayoutManager(requireContext())


    }

    private fun animateSwap(showEditor: Boolean) {
        // 간단히 visibility만 토글해도 OK
        // MaterialSharedAxis로 부드럽게
        val axis = MaterialSharedAxis(MaterialSharedAxis.Y, showEditor)
        TransitionManager.beginDelayedTransition(requireView() as ViewGroup, axis)
        panelList.visibility = if (showEditor) View.GONE else View.VISIBLE
        fab.visibility = if (showEditor) View.GONE else View.VISIBLE
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