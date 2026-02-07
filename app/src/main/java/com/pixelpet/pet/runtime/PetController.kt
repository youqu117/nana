package com.pixelpet.pet.runtime

import com.pixelpet.pet.view.PetView

class PetController(
    private val petView: PetView,
    private val petRuntime: PetRuntime
) : PetApi {
    fun update() {
        val now = System.currentTimeMillis()
        petRuntime.tick(now)
        petView.updateState(petRuntime.state)
    }

    override fun onTap() {
        petRuntime.handleTap(System.currentTimeMillis())
        update()
    }

    override fun requestAction(actionId: String) {
        // Todo: Implement specific actions
    }

    override fun notifyEvent(eventId: String) {
        // Todo: Implement event handling
    }

    fun onDoubleTap() {
        petRuntime.handleDoubleTap(System.currentTimeMillis())
        update()
    }

    fun onLongPress() {
        petRuntime.handleLongPressStart()
        update()
    }
    
    fun onRelease() {
        petRuntime.handleRelease()
        update()
    }
}



