package org.cssnr.remotewallpaper.ui.history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cssnr.remotewallpaper.db.HistoryItem

class HistoryViewModel : ViewModel() {

    val selectedIds = mutableSetOf<Long>()

    //val stationData = MutableLiveData<List<HistoryItem>>()

    val historyData: MutableLiveData<List<HistoryItem>> by lazy {
        MutableLiveData()
    }

}
