package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.SELECTED_LANGUAGE
import com.simohypers.limusic.common.SUPPORTED_LANGUAGE
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.home.HomeItem
import com.simohypers.limusic.domain.data.model.library.LibraryOverview
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.repository.AccountRepository
import com.simohypers.limusic.domain.repository.CommonRepository
import com.simohypers.limusic.domain.repository.HomeRepository
import com.simohypers.limusic.domain.utils.Resource
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.viewModel.base.BaseViewModel
import org.koin.core.component.inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.quick_picks
import limusic.composeapp.generated.resources.song
import limusic.composeapp.generated.resources.view_count

@OptIn(kotlinx.coroutines.FlowPreview::class)
class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
    private val accountRepository: AccountRepository,
    commonRepository: CommonRepository,
) : BaseViewModel() {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private var _homeListState = MutableStateFlow<ListState>(ListState.IDLE)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private val accounts: YouTubeAccounts by inject()

    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()


    val loading = MutableStateFlow<Boolean>(true)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(null)
    val params: StateFlow<String?> = _params

    // For showing alert that should log in to YouTube

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val shortcuts: StateFlow<List<Shortcut>> =
        Shortcut
            .flow(dataStoreManager)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Live counts for the Library collections pinned to Shortcuts. */
    val libraryOverview: StateFlow<LibraryOverview?> =
        commonRepository
            .getLibraryOverview()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val homeLayout: StateFlow<HomeLayout> =
        dataStoreManager
            .getString(HOME_LAYOUT_KEY)
            .map(HomeLayout::parse)
            .stateIn(viewModelScope, SharingStarted.Eagerly, HomeLayout())

    // Each edit reads, changes and writes back the whole blob. Shortcuts keep their own lock.
    private val storeLock = Mutex()

    init {
        homeJob = Job()
        // One trigger for everything that changes what Home shows: region, language, the YouTube
        // session and the mood chip. Four collectors used to start a full reload each at launch.
        // The short debounce also lets a new sign-in reach the scraper before the reload asks.
        viewModelScope.launch {
            combine(
                dataStoreManager.location,
                dataStoreManager.language,
                dataStoreManager.cookie,
                params,
            ) { location, lang, cookie, chip -> listOf(location, lang, cookie, chip) }
                .distinctUntilChanged()
                .debounce(300)
                .collectLatest { (_, _, _, chip) -> getHomeItemList(chip) }
        }
        // The active saved account alone, as Settings shows it: name and photo are written once, at
        // sign-in, so a half-finished write or the feed's strapline can't blank the avatar.
        viewModelScope.launch {
            combine(dataStoreManager.cookie, accounts.active) { cookie, active ->
                if (cookie.isEmpty()) null else Pair(active?.name, active?.thumbnailUrl)
            }.distinctUntilChanged()
                .collect { _accountInfo.value = it }
        }
    }

    fun getHomeItemList(params: String? = null) {
        loading.value = true
        _homeListState.value = ListState.LOADING
        language =
            runBlocking {
                dataStoreManager.getString(SELECTED_LANGUAGE).first()
                    ?: SUPPORTED_LANGUAGE.codes.first()
            }
        regionCode = runBlocking { dataStoreManager.location.first() }
        homeJob?.cancel()
        homeJob =
            viewModelScope.launch {
                homeRepository
                    .getHomeData(
                        params,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                _homeItemList.value = home.data?.second ?: listOf()
                            }

                            else -> {
                                _continuation.value = null
                                _homeItemList.value = listOf()
                            }
                        }
                        if (home is Resource.Error) {
                            showSnackBarErrorState.emit(home.message ?: "Unknown error")
                            Logger.w("Error", "getHomeItemList: ${home.message}")
                            reloadWhenOnline({ _homeItemList.value.isEmpty() }) { getHomeItemList(params) }
                        }
                    }
                // Quick picks leads Home, but YouTube can send it on a later page. Fetch up to it
                // before showing Home, or it lands above the list once the user scrolls there.
                // ponytail: two extra pages at most, the same requests scrolling down would make.
                val quickPicks = getString(Res.string.quick_picks)
                repeat(2) {
                    val next = continuation.value
                    if (next.isNullOrEmpty() || _homeItemList.value.any { it.title == quickPicks }) return@repeat
                    homeRepository
                        .getHomeDataContinue(next, getString(Res.string.view_count), getString(Res.string.song))
                        .collect { home ->
                            _continuation.value = (home as? Resource.Success)?.data?.first
                            home.data?.second?.let { items -> _homeItemList.update { it + items } }
                        }
                }
                _homeListState.value =
                    if (continuation.value.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
                loading.value = false
            }
    }

    /** A shelf's "Play all": YouTube's own finite queue of that shelf's songs. */
    fun playAll(item: HomeItem) {
        item.playAllEndpoint?.let { playFromEndpoint(it, item.title) }
    }

    fun getContinueHomeItem(continuation: String?) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _homeListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                log("Get more home item with continuation: $continuation")
                _homeListState.value = ListState.PAGINATING
                homeRepository
                    .getHomeDataContinue(
                        continuation,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                    ).collect { home ->
                        when (home) {
                            is Resource.Success -> {
                                _continuation.value = home.data?.first
                                val newItems = home.data?.second ?: listOf()
                                _homeItemList.update { it + newItems }
                                if (home.data?.first.isNullOrEmpty()) {
                                    _homeListState.value = ListState.PAGINATION_EXHAUST
                                } else {
                                    _homeListState.value = ListState.IDLE
                                }
                            }

                            is Resource.Error -> {
                                _continuation.value = null
                                Logger.w(tag, "getContinueHomeItem: ${home.message}")
                                showSnackBarErrorState.emit(home.message ?: "Unknown error")
                                _homeListState.value = ListState.PAGINATION_EXHAUST
                            }
                        }
                    }
            }
        }
    }

    fun toggleShortcut(shortcut: Shortcut) {
        viewModelScope.launch { Shortcut.toggle(dataStoreManager, shortcut) }
    }

    /** Edit Home's changes land as they are made: Home behind the sheet is the preview. */
    fun updateHomeLayout(change: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch {
            storeLock.withLock {
                val now = HomeLayout.parse(dataStoreManager.getString(HOME_LAYOUT_KEY).first())
                dataStoreManager.putString(HOME_LAYOUT_KEY, HomeLayout.encode(change(now)))
            }
        }
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }

    companion object {
        private const val HOME_LAYOUT_KEY = "home_layout"

        // Home params
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}

/**
 * A Shortcuts tile on Home: something from the library, pinned by hand. Stored as a snapshot of the
 * card when it was pinned; list order is display order.
 */
@Serializable
data class Shortcut(
    val kind: Kind,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val thumbnail: String? = null,
) {
    /** [COLLECTION] is one of Library's four; its [id] is the LibraryDynamicPlaylistType params. */
    enum class Kind { COLLECTION, LOCAL_PLAYLIST, PLAYLIST, OWN_PLAYLIST, ALBUM, PODCAST }

    // One playlist whether it came from your library or a feed card, with or without YouTube's VL.
    fun sameAs(other: Shortcut) = kind.group() == other.kind.group() && id.removePrefix("VL") == other.id.removePrefix("VL")

    private fun Kind.group() = if (this == Kind.OWN_PLAYLIST) Kind.PLAYLIST else this

    companion object {
        const val MAX = 8
        private const val KEY = "home_shortcuts"
        private val json = Json { ignoreUnknownKeys = true }

        // Home and every card menu write the one list, so the lock is the list's, not a view model's.
        private val lock = Mutex()

        fun flow(store: DataStoreManager): Flow<List<Shortcut>> = store.getString(KEY).map(::parse)

        /** Pins [shortcut], or takes it off when it is already there. A full grid takes nothing. */
        suspend fun toggle(
            store: DataStoreManager,
            shortcut: Shortcut,
        ) = edit(store) { list ->
            when {
                list.any { it.sameAs(shortcut) } -> list.filterNot { it.sameAs(shortcut) }
                list.size < MAX -> list + shortcut
                else -> list
            }
        }

        suspend fun remove(
            store: DataStoreManager,
            shortcut: Shortcut,
        ) = edit(store) { list -> list.filterNot { it.sameAs(shortcut) } }

        // Reads the store, not a collected copy: two quick taps would both see the list before either write.
        private suspend fun edit(
            store: DataStoreManager,
            change: (List<Shortcut>) -> List<Shortcut>,
        ) = lock.withLock { store.putString(KEY, encode(change(parse(store.getString(KEY).first())))) }

        fun parse(raw: String?): List<Shortcut> = raw?.let { runCatching { json.decodeFromString<List<Shortcut>>(it) }.getOrNull() }.orEmpty()

        fun encode(list: List<Shortcut>): String = json.encodeToString(list)
    }
}

