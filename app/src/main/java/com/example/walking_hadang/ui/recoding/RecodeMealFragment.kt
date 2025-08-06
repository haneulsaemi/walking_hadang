package com.example.walking_hadang.ui.recoding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentRecodeMealBinding

class RecodeMealFragment : Fragment() {

    private var _binding: FragmentRecodeMealBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecodeMealBinding.inflate(inflater, container, false)
        return binding.root
    }

}