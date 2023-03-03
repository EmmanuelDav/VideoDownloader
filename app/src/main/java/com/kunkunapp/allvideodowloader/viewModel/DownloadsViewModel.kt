package com.kunkunapp.allvideodowloader.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.kunkunapp.allvideodowloader.database.AppDatabase
import com.kunkunapp.allvideodowloader.database.Download
import com.kunkunapp.allvideodowloader.database.DownloadsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadsRepository
    val allDownloads: LiveData<List<Download>>

    init {
        val downloadsDao = AppDatabase.getDatabase(application).downloadsDao()
        repository = DownloadsRepository(downloadsDao)
        allDownloads = repository.allDownloads
    }

    fun insert(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(word)
    }

    fun update(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(word)
    }

    fun delete(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(word)
    }
}
