import java.util.concurrent.ArrayBlockingQueue

object ProdutorConsumidor:

  // Fila limitada compartilhada pelas duas threads.
  private val fila = new ArrayBlockingQueue[Option[Int]](5)

  def main(args: Array[String]): Unit =
    val produtor = new Thread(() =>
      for numero <- 1 to 10 do
        fila.put(Some(numero))
        println(s"Produtor: produziu $numero")

      // None representa o fim da produção.
      fila.put(None)
      println("Produtor: encerrou a produção")
    )

    val consumidor = new Thread(() =>
      var continuar = true

      while continuar do
        fila.take() match
          case Some(numero) =>
            println(s"Consumidor: recebeu $numero")
          case None =>
            println("Consumidor: recebeu o sinal de encerramento")
            continuar = false
    )

    produtor.start()
    consumidor.start()

    produtor.join()
    consumidor.join()

    println("Programa finalizado.")
