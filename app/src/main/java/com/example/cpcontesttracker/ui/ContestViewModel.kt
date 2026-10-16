package com.example.cpcontesttracker.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpcontesttracker.data.api.RetrofitInstance
import com.example.cpcontesttracker.data.model.Contest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class ContestUiState {
    object Loading : ContestUiState()
    data class Success(val contests: List<Contest>) : ContestUiState()
    data class Error(val message: String) : ContestUiState()
}

class ContestViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ContestUiState>(ContestUiState.Loading)
    val uiState: StateFlow<ContestUiState> = _uiState

    fun fetchContests(selectedPlatforms: Set<String>) {
        viewModelScope.launch {
            _uiState.value = ContestUiState.Loading
            try {
                // Fetch data from our Vercel Proxy (no API key needed here anymore!)
                val response = RetrofitInstance.api.getContests()
                
                // Filter 1: Only include platforms the user selected
                // Filter 2: Exclude contests with Cyrillic (Russian) characters to ensure English
                val cyrillicRegex = Regex(".*[А-Яа-я].*")
                
                val filteredContests = response.contests.filter { contest ->
                    selectedPlatforms.contains(contest.resource) && !contest.event.matches(cyrillicRegex)
                }
                
                _uiState.value = ContestUiState.Success(filteredContests)
            } catch (e: Exception) {
                Log.e("ContestViewModel", "Error fetching contests", e)
                _uiState.value = ContestUiState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }
}
