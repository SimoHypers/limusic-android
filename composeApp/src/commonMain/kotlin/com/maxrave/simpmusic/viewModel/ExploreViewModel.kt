package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.music_video
import simpmusic.composeapp.generated.resources.new_release

/**
 * The Explore tab: new releases, moods & genres and the charts. Loaded once when the tab is first
 * opened (and again on pull to refresh), so starting the app costs only the Home feed.
 */
class ExploreViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _newRelease = MutableStateFlow<List<HomeItem>>(emptyList())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease.asStateFlow()

    private val _mood = MutableStateFlow<Mood?>(null)
    val mood: StateFlow<Mood?> = _mood.asStateFlow()

    private val _chart = MutableStateFlow<Chart?>(null)
    val chart: StateFlow<Chart?> = _chart.asStateFlow()

    private val _chartRegion = MutableStateFlow<String?>(null)
    val chartRegion: StateFlow<String?> = _chartRegion.asStateFlow()

    private val _loadingChart = MutableStateFlow(true)
    val loadingChart: StateFlow<Boolean> = _loadingChart.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _loading.value = true
        viewModelScope.launch {
            val newReleaseJob =
                launch {
                    homeRepository
                        .getNewRelease(getString(Res.string.new_release), getString(Res.string.music_video))
                        .collect { _newRelease.value = (it as? Resource.Success)?.data.orEmpty() }
                }
            val moodJob =
                launch {
                    homeRepository.getMoodAndMomentsData().collect { _mood.value = (it as? Resource.Success)?.data }
                }
            loadChart(dataStoreManager.chartKey.first())
            newReleaseJob.join()
            moodJob.join()
            _loading.value = false
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            dataStoreManager.setChartKey(region)
            loadChart(region)
        }
    }

    private suspend fun loadChart(region: String) {
        _loadingChart.value = true
        _chartRegion.value = region
        homeRepository.getChartData(region).collect { _chart.value = (it as? Resource.Success)?.data }
        _loadingChart.value = false
    }
}
