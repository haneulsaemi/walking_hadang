package com.example.walking_hadang.ui.store

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walking_hadang.databinding.FragmentStoreBinding
import com.google.gson.Gson

class StoreFragment : Fragment() {
    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)

        // 기본은 저당식품 보여주기
        loadAndShowItems("store_dummy.json")

        // 버튼 클릭 시 카테고리 전환
        binding.btnFood.setOnClickListener {
            loadAndShowItems("store_dummy.json")
        }
        binding.btnClothes.setOnClickListener {
            loadAndShowItems("store_clothes.json")
        }
        binding.btnShoes.setOnClickListener {
            loadAndShowItems("store_shoes.json")
        }

        return binding.root
    }

    private fun loadAndShowItems(fileName: String) {
        try {
            val inputStream = requireContext().assets.open(fileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val wrapper = gson.fromJson(json, StoreWrapper::class.java)
            val items = wrapper.records

            binding.recyclerViewStore.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewStore.adapter = StoreAdapter(items)

        } catch (e: Exception) {
            Log.e("StoreFragment", "JSON Load Error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
