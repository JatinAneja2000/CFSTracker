package com.teessideUni.cfs_tracker.presentation.screens.heart_rate

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teessideUni.cfs_tracker.data.repository.HeartRateRepositoryImpl
import com.teessideUni.cfs_tracker.domain.model.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Date

class HeartRateDataStoreViewModel (
    private val repository: HeartRateRepositoryImpl
) : ViewModel()
{
    private val _storeState = Channel<DataStoreState>()
    val storeState = _storeState.receiveAsFlow()

    fun storeHeartRate(bpm:Double, timestamp: Date) = viewModelScope.launch {


        Log.d("reached", bpm.toString())
        repository.storeHeartRateData(bpm, timestamp).collect{
                result ->
            when(result){
                is Resource.Success -> {
                    Log.d("saved", bpm.toString())

                    _storeState.send(DataStoreState(isSuccess = "Data get successfully stored."))
                }
                is Resource.Error -> {
                    Log.d("error", result.message.toString())
                    _storeState.send(DataStoreState(isError = result.message.toString()))
                }
                is Resource.Loading -> {
                    _storeState.send(DataStoreState(isLoading = true))
                }
            }
        }
    }
}