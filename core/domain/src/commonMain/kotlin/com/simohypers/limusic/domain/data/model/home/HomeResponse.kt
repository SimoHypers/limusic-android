package com.simohypers.limusic.domain.data.model.home

import com.simohypers.limusic.domain.data.model.home.chart.Chart
import com.simohypers.limusic.domain.data.model.mood.Mood
import com.simohypers.limusic.domain.utils.Resource

data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>,
)