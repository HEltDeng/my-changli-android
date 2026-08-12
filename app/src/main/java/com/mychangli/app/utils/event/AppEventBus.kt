package com.mychangli.app.utils.event

import kotlinx.coroutines.flow.MutableSharedFlow

/** V1 only needs the main-tab selection event. */
object AppEventBus {
    val selectEvent = MutableSharedFlow<SelectEvent>(extraBufferCapacity = 1)
}
