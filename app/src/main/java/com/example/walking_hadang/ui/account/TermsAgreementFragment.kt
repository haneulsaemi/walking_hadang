package com.example.walking_hadang.ui.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.walking_hadang.R
import com.example.walking_hadang.databinding.FragmentHomeBinding
import com.example.walking_hadang.databinding.FragmentTermsAgreementBinding

class TermsAgreementFragment : Fragment() {
    private var _binding: FragmentTermsAgreementBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTermsAgreementBinding.inflate(inflater, container, false)
        return binding.root
    }

}