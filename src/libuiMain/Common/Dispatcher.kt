import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

class Dispatcher {
    private val queue: MutableList<() -> Unit> = mutableListOf()
    private val queueWorker = Worker.start()

    init {
        queueWorker.execute(TransferMode.UNSAFE, { this.queue }) {
            while (true) {
                if (it.isEmpty()) { continue }
                val function = it.removeAt(0)
                function.invoke()
            }
        }
    }

    fun queueFunction(function: () -> Unit) {
        queue.add(function)
    }
}
