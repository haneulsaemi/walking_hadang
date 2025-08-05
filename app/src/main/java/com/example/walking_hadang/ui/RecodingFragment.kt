package com.example.walking_hadang.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.walking_hadang.databinding.FragmentRecodingBinding


class RecodingFragment : Fragment() {
    private var _binding: FragmentRecodingBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecodingBinding.inflate(inflater, container, false)
        return binding.root
    }
}