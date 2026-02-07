package com.pixelpet.pet.runtime

interface PetApi {
    fun onTap()
    fun requestAction(actionId: String)
    fun notifyEvent(eventId: String)
}

