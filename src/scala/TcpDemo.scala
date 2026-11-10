import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import java.nio.charset.StandardCharsets

object TcpDemo:

  def main(args: Array[String]): Unit =
    args.headOption match
      case Some("servidor") => executarServidor()
      case Some("cliente") if args.length >= 2 => executarCliente(args(1))
      case _ =>
        println("Uso:")
        println("  scala run src/scala/TcpDemo.scala -- servidor")
        println("  scala run src/scala/TcpDemo.scala -- cliente IP_DO_SERVIDOR")

  private def executarServidor(): Unit =
    val servidor = new ServerSocket(5000)
    println("Servidor aguardando conexão na porta 5000...")

    try
      val socket = servidor.accept()
      println(s"Cliente conectado: ${socket.getInetAddress.getHostAddress}")

      val entrada = new BufferedReader(
        new InputStreamReader(socket.getInputStream, StandardCharsets.UTF_8)
      )
      val saida = new PrintWriter(socket.getOutputStream, true, StandardCharsets.UTF_8)

      try
        var linha = entrada.readLine()
        while linha != null do
          println(s"Servidor recebeu: $linha")
          saida.println(s"Mensagem recebida: $linha")
          linha = entrada.readLine()
      finally
        entrada.close()
        saida.close()
        socket.close()
    finally
      servidor.close()

  private def executarCliente(host: String): Unit =
    val socket = new Socket(host, 5000)
    val saida = new PrintWriter(socket.getOutputStream, true, StandardCharsets.UTF_8)
    val entrada = new BufferedReader(
      new InputStreamReader(socket.getInputStream, StandardCharsets.UTF_8)
    )

    try
      List("Olá, servidor!", "Esta mensagem foi enviada por TCP.").foreach(saida.println)
      socket.shutdownOutput()

      var resposta = entrada.readLine()
      while resposta != null do
        println(s"Resposta do servidor: $resposta")
        resposta = entrada.readLine()
    finally
      entrada.close()
      saida.close()
      socket.close()
