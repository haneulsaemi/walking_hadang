package com.example.walking_hadang.ui.recoding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentRecodeMealBinding
import com.example.walking_hadang.util.RecodingSharedViewModel
import kotlin.getValue

class RecodeMealFragment : Fragment() {

    private var _binding: FragmentRecodeMealBinding? = null
    private val binding get() = _binding!!

    private val sharedVM: RecodingSharedViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecodeMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}