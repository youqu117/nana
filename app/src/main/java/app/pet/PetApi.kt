package app.pet

interface PetApi {
    fun onTap()
    fun requestAction(actionId: String)
    fun notifyEvent(eventId: String)
}
