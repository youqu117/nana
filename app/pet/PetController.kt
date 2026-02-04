package app.pet

class PetController(private val petView: PetView) {
    fun playTapFeedback() {
        petView.playTapReaction()
    }
}
