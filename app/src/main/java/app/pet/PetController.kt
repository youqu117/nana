package app.pet

class PetController(
    private val petView: PetView,
    private val petRuntime: PetRuntime
) {
    fun update() {
        val now = System.currentTimeMillis()
        petRuntime.tick(now)
        petView.updateState(petRuntime.state)
    }

    fun onTap() {
        petRuntime.handleTap(System.currentTimeMillis())
        update()
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

