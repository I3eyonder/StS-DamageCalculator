package dmgcalculator.publisher

import dmgcalculator.interfaces.PlayerEndTurnSubscriber

object PlayerEndTurnPublisher {

    private val playerEndTurnSubscribers = mutableListOf<PlayerEndTurnSubscriber>()

    fun publish() {
        playerEndTurnSubscribers.forEach {
            it.onPlayerEndTurn()
        }
    }

    fun subscribe(subscriber: PlayerEndTurnSubscriber) {
        playerEndTurnSubscribers.add(subscriber)
    }
}
