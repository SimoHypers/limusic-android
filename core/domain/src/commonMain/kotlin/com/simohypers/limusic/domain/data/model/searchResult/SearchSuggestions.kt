package com.simohypers.limusic.domain.data.model.searchResult

import com.simohypers.limusic.domain.data.type.SearchResultType

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchResultType>,
)