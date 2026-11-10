import scala.io.Source
import java.io.{BufferedWriter, OutputStreamWriter}
import java.nio.charset.StandardCharsets

object ProcessosDemo:

  def main(args: Array[String]): Unit =
    if args.contains("filho") then
      executarFilho()
    else
      executarPai()

  // O processo filho recebe mensagens pela entrada padrão.
  private def executarFilho(): Unit =
    for linha <- Source.stdin.getLines() do
      println(s"Processo filho recebeu: $linha")

  // O processo pai inicia outra execução deste programa.
  private def executarPai(): Unit =
    val processo = new ProcessBuilder(
      "scala",
      "run",
      "src/scala/ProcessosDemo.scala",
      "--",
      "filho"
    ).redirectErrorStream(true).start()

    val entradaFilho = new BufferedWriter(
      new OutputStreamWriter(processo.getOutputStream, StandardCharsets.UTF_8)
    )

    val mensagens = List("mensagem 1", "mensagem 2", "mensagem 3")

    mensagens.foreach { mensagem =>
      entradaFilho.write(mensagem)
      entradaFilho.newLine()
    }

    entradaFilho.close()

    val saidaFilho = Source.fromInputStream(processo.getInputStream)
    try
      saidaFilho.getLines().foreach(println)
    finally
      saidaFilho.close()

    val codigo = processo.waitFor()
    println(s"Processo filho finalizado com código: $codigo")
