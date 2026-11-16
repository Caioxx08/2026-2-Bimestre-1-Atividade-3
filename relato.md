# Relatório sobre implementação de comunicação entre tarefas em Scala

## Introdução

Este relato faz parte do processo avaliativo da disciplina de Sistemas Operacionais do curso superior de Análise e Desenvolvimento de Sistemas, ofertado pela Diretoria Acadêmica de Gestão e Tecnologia da Informação no Campus Natal-Central do Instituto Federal de Educação, Ciência e Tecnologia do Rio Grande do Norte.

O objetivo é apresentar uma implementação de comunicação entre tarefas utilizando a linguagem Scala, abordando comunicação entre threads, entre processos no mesmo computador e comunicação por rede utilizando sockets TCP.

O grupo de trabalho foi formado por Loíde Barros Fragoso, Angelo Evaristo Porfirio e Caio Arthur Cardozo da Silva.

## Comunicação entre tarefas em Scala

### Informações gerais

A comunicação entre tarefas permite que partes de um programa ou programas diferentes troquem dados e coordenem suas execuções. Isso é importante para dividir responsabilidades, executar atividades de forma concorrente e compartilhar informações.

O Docker foi escolhido para padronizar o ambiente de execução. Com ele, a aplicação é executada em um contêiner com Java e Scala CLI instalados, reduzindo diferenças entre as configurações das máquinas utilizadas pelo grupo.

A configuração está definida no arquivo `Dockerfile`. A imagem base é `eclipse-temurin:17-jdk`, que fornece o ambiente Java necessário para executar Scala. Em seguida, o arquivo instala ferramentas de download e o Scala CLI, copia a pasta `src` para o contêiner e define o programa principal como comando padrão.

### Comunicação entre tarefas com linhas de execução no mesmo processo

O programa utiliza duas threads: uma produtora e outra consumidora. A comunicação ocorre por meio de uma fila compartilhada do tipo `ArrayBlockingQueue`, com capacidade limitada a cinco elementos.

A thread produtora insere os números de 1 a 10 na fila. A thread consumidora retira os elementos e os apresenta no terminal. Como a fila é bloqueante, a operação de retirada aguarda quando não há elementos disponíveis e a operação de inserção aguarda quando a fila está cheia. Isso permite sincronizar as duas threads sem precisar implementar manualmente o controle de acesso à fila.

O tipo `Option[Int]` permite utilizar `Some(numero)` para representar um dado e `None` como sinal de encerramento. Depois de inserir todos os números, a produtora coloca esse sinal na fila. Ao recebê-lo, a consumidora encerra o laço. Por fim, o método `join()` faz a thread principal aguardar a finalização das duas threads.

```bash
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
```

A saída apresenta mensagens do produtor e do consumidor. A ordem exata pode variar entre execuções, pois as threads são concorrentes. O resultado esperado é que os números de 1 a 10 sejam produzidos e recebidos, seguido da mensagem de encerramento.


### Comunicação entre tarefas em processos diferentes no mesmo computador

O programa inicia uma segunda execução de si mesmo por meio de `ProcessBuilder`. Assim, existem dois processos independentes: o processo pai e o processo filho.

O processo pai envia três mensagens ao processo filho por meio da entrada padrão do processo (`stdin`). O filho lê cada linha recebida e escreve uma resposta na saída padrão (`stdout`). O pai lê essa saída e a apresenta no terminal. O fechamento do fluxo de entrada indica ao filho que não há mais mensagens.

```bash
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
```


### Comunicação entre tarefas em processos diferentes em computadores diferentes

Ele utiliza sockets TCP, permitindo que dois programas em execução em máquinas diferentes troquem mensagens pela rede.

O programa possui dois modos. No modo servidor, cria um `ServerSocket` na porta 5000 e aguarda uma conexão. Quando um cliente se conecta, o servidor lê as mensagens recebidas e envia uma resposta para cada uma. No modo cliente, o programa conecta-se ao endereço IP do servidor, envia mensagens e apresenta as respostas recebidas.


```bash
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
```

O resultado ideal é que o servidor mostre as mensagens enviadas pelo cliente e que o cliente mostre as respostas do servidor. 

### Problemas encontrados e soluções

Nosso maior problema no desenvolvimento do trabalho, foi por não sabermos utilizar com tanto domínio a linguagem java, já que ela é a base para scala como mostramos.

## Considerações finais

A implementação apresenta três formas de comunicação: compartilhamento de uma fila entre threads, troca de dados entre processos por fluxos de entrada e saída e comunicação em rede por sockets TCP. Com isso, é possível observar diferenças entre concorrência dentro de um mesmo processo, comunicação entre processos locais e troca de mensagens pela rede.

O trabalho permite praticar conceitos importantes de Sistemas Operacionais, como threads, processos, sincronização, comunicação entre processos e sockets. Recomenda-se que os próximos alunos executem cada etapa gradualmente, validem os resultados no ambiente Docker e registrem capturas de tela das execuções para documentar a experiência.
