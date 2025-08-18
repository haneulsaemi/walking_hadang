package com.example.walking_hadang.ui.community

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CommunityBoardsViewModel : ViewModel() {

    // 혈당 공유
    private val _sugar = MutableLiveData<List<CommunityPost>>(emptyList())
    val sugar: LiveData<List<CommunityPost>> get() = _sugar
    fun addSugar(post: CommunityPost) { _sugar.value = listOf(post) + (_sugar.value ?: emptyList()) }
    fun initSugarIfEmpty(supplier: () -> List<CommunityPost>) { if (_sugar.value.isNullOrEmpty()) _sugar.value = supplier() }

    // 운동 공유
    private val _exercise = MutableLiveData<List<CommunityPost>>(emptyList())
    val exercise: LiveData<List<CommunityPost>> get() = _exercise
    fun addExercise(post: CommunityPost) { _exercise.value = listOf(post) + (_exercise.value ?: emptyList()) }
    fun initExerciseIfEmpty(supplier: () -> List<CommunityPost>) { if (_exercise.value.isNullOrEmpty()) _exercise.value = supplier() }

    // 산책
    private val _walk = MutableLiveData<List<CommunityPost>>(emptyList())
    val walk: LiveData<List<CommunityPost>> get() = _walk
    fun addWalk(post: CommunityPost) { _walk.value = listOf(post) + (_walk.value ?: emptyList()) }
    fun initWalkIfEmpty(supplier: () -> List<CommunityPost>) { if (_walk.value.isNullOrEmpty()) _walk.value = supplier() }

    // 식품 공유
    private val _food = MutableLiveData<List<CommunityPost>>(emptyList())
    val food: LiveData<List<CommunityPost>> get() = _food
    fun addFood(post: CommunityPost) { _food.value = listOf(post) + (_food.value ?: emptyList()) }
    fun initFoodIfEmpty(supplier: () -> List<CommunityPost>) { if (_food.value.isNullOrEmpty()) _food.value = supplier() }

    // 자유게시판
    private val _free = MutableLiveData<List<CommunityPost>>(emptyList())
    val free: LiveData<List<CommunityPost>> get() = _free
    fun addFree(post: CommunityPost) { _free.value = listOf(post) + (_free.value ?: emptyList()) }
    fun initFreeIfEmpty(supplier: () -> List<CommunityPost>) { if (_free.value.isNullOrEmpty()) _free.value = supplier() }
}
