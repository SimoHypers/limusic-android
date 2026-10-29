package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.listentogether.DeviceLink
import com.maxrave.domain.data.model.listentogether.PairResult
import com.maxrave.domain.repository.DeviceLinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Settings > Integrations > Devices: pairing with Limusic desktop and the linked devices. */
class DeviceLinkViewModel(
    private val repository: DeviceLinkRepository,
) : ViewModel() {
    val link: StateFlow<DeviceLink> = repository.link

    private val _pairing = MutableStateFlow(false)
    val pairing: StateFlow<Boolean> = _pairing.asStateFlow()

    /** Why the last Link didn't work, until the next attempt. */
    private val _pairError = MutableStateFlow<PairResult?>(null)
    val pairError: StateFlow<PairResult?> = _pairError.asStateFlow()

    /** [invite] is the code from the desktop, or `CODE@host` on a custom server. */
    fun pair(invite: String) {
        val (server, code) = ListenTogetherViewModel.parseInvite(invite) ?: return
        if (_pairing.value || code.isEmpty()) return
        viewModelScope.launch {
            _pairing.value = true
            _pairError.value = null
            val result = repository.pair(if (server == ListenTogetherViewModel.DEFAULT_SERVER_URL) "" else server, code)
            _pairError.value = result.takeIf { it != PairResult.Linked }
            _pairing.value = false
        }
    }

    fun unlink(deviceId: String) = repository.unlink(deviceId)

    fun unlinkThisPhone() = repository.unlinkThisPhone()

    fun playOn(deviceId: String?) = repository.playOn(deviceId)
}
