package org.cssnr.remotewallpaper.ui.remotes

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cssnr.remotewallpaper.db.Remote

class RemotesViewModel : ViewModel() {

    //val stationData = MutableLiveData<List<Remote>>()

    val remotesData: MutableLiveData<List<Remote>> by lazy {
        MutableLiveData<List<Remote>>()
    }

}
