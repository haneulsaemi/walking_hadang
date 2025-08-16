package com.example.walking_hadang.ui.recoding

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walking_hadang.BuildConfig
import com.example.walking_hadang.MyApplication
import com.example.walking_hadang.R
import com.example.walking_hadang.adapter.WalkAdapter
import com.example.walking_hadang.data.StaticMapConfig
import com.example.walking_hadang.data.buildStaticMapUrl
import com.example.walking_hadang.databinding.FragmentRecodeWalkingBinding
import com.example.walking_hadang.util.WalkRepository

class RecodeWalkingFragment : Fragment() {

    private var _binding: FragmentRecodeWalkingBinding? = null
    private  val binding get() = _binding!!

    private lateinit var adapter: WalkAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecodeWalkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = binding.rvWalks
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = WalkAdapter(
            buildStaticMapUrl = { poly, start, end ->
                buildStaticMapUrl(
                    routePolyline = poly,
                    apiKey = BuildConfig.MAPS_API_KEY,
                    start = start,
                    end = end,
                    cfg = StaticMapConfig(width = 600, height = 360, strokeColor = "0x000000ff")
                )
            },
            onClick = { walk ->
                // TODO: 상세 화면 이동
            }
        )
        rv.adapter = adapter
        WalkRepository.loadLatestWalks(
            pageSize = 20,
            onSuccess = { list ->
                Log.d("Walk", "loaded count = ${list.size}")
                adapter.submitList(list)
                        },
            onError = { e ->
                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()
                Log.d("Walk", e.localizedMessage)
            }
        )
    }
}