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

A implementação principal está em `src/scala/ProdutorConsumidor.scala`. O programa utiliza duas threads: uma produtora e outra consumidora. A comunicação ocorre por meio de uma fila compartilhada do tipo `ArrayBlockingQueue`, com capacidade limitada a cinco elementos.

A thread produtora insere os números de 1 a 10 na fila. A thread consumidora retira os elementos e os apresenta no terminal. Como a fila é bloqueante, a operação de retirada aguarda quando não há elementos disponíveis e a operação de inserção aguarda quando a fila está cheia. Isso permite sincronizar as duas threads sem precisar implementar manualmente o controle de acesso à fila.

O tipo `Option[Int]` permite utilizar `Some(numero)` para representar um dado e `None` como sinal de encerramento. Depois de inserir todos os números, a produtora coloca esse sinal na fila. Ao recebê-lo, a consumidora encerra o laço. Por fim, o método `join()` faz a thread principal aguardar a finalização das duas threads.

Código completo: [src/scala/ProdutorConsumidor.scala](src/scala/ProdutorConsumidor.scala).

Para construir a imagem e executar o programa:

```bash
docker build -t atividade3-scala .
docker run --rm atividade3-scala
```

A saída apresenta mensagens do produtor e do consumidor. A ordem exata pode variar entre execuções, pois as threads são concorrentes. O resultado esperado é que os números de 1 a 10 sejam produzidos e recebidos, seguido da mensagem de encerramento.

**Registro de execução:** inserir aqui uma captura de tela do terminal após executar os comandos no computador do grupo. A execução ainda precisa ser validada pelos integrantes.

### Comunicação entre tarefas em processos diferentes no mesmo computador

A demonstração está no arquivo `src/scala/ProcessosDemo.scala`. O programa inicia uma segunda execução de si mesmo por meio de `ProcessBuilder`. Assim, existem dois processos independentes: o processo pai e o processo filho.

O processo pai envia três mensagens ao processo filho por meio da entrada padrão do processo (`stdin`). O filho lê cada linha recebida e escreve uma resposta na saída padrão (`stdout`). O pai lê essa saída e a apresenta no terminal. O fechamento do fluxo de entrada indica ao filho que não há mais mensagens.

Código completo: [src/scala/ProcessosDemo.scala](src/scala/ProcessosDemo.scala).

Para executar a demonstração dentro do contêiner, utilize:

```bash
docker run --rm atividade3-scala scala run src/scala/ProcessosDemo.scala
```

O resultado esperado é a exibição das três mensagens recebidas pelo processo filho e do código de encerramento do processo.

**Registro de execução:** inserir aqui a captura de tela da execução real. O comando depende de a imagem já ter sido construída com o Dockerfile deste repositório.

### Comunicação entre tarefas em processos diferentes em computadores diferentes

Para demonstrar a comunicação por rede, foi criado o arquivo `src/scala/TcpDemo.scala`. Ele utiliza sockets TCP, permitindo que dois programas em execução em máquinas diferentes troquem mensagens pela rede.

O programa possui dois modos. No modo servidor, cria um `ServerSocket` na porta 5000 e aguarda uma conexão. Quando um cliente se conecta, o servidor lê as mensagens recebidas e envia uma resposta para cada uma. No modo cliente, o programa conecta-se ao endereço IP do servidor, envia mensagens e apresenta as respostas recebidas.

Código completo: [src/scala/TcpDemo.scala](src/scala/TcpDemo.scala).

Para testar em computadores diferentes, primeiro construa a imagem em ambas as máquinas. No computador que atuará como servidor, execute:

```bash
docker run --rm -p 5000:5000 atividade3-scala scala run src/scala/TcpDemo.scala -- servidor
```

No outro computador, substitua `IP_DO_SERVIDOR` pelo endereço IP local da máquina servidora:

```bash
docker run --rm atividade3-scala scala run src/scala/TcpDemo.scala -- cliente IP_DO_SERVIDOR
```

É necessário que as duas máquinas estejam conectadas à rede e que a porta TCP 5000 esteja liberada no firewall do servidor. A opção `-p 5000:5000` publica a porta do contêiner no computador servidor.

O resultado esperado é que o servidor mostre as mensagens enviadas pelo cliente e que o cliente mostre as respostas do servidor. Executar os dois contêineres na mesma máquina pode servir para um teste inicial de sockets, mas não comprova comunicação entre computadores diferentes.

**Registro de execução:** inserir capturas de tela do servidor e do cliente executados em computadores distintos. Esta etapa precisa ser realizada e validada pelo grupo em duas máquinas.

### Problemas encontrados e soluções

A configuração de ferramentas e permissões do Docker pode variar de uma máquina para outra. Caso apareça uma mensagem de permissão negada ao acessar o Docker, é necessário verificar se o serviço está ativo e se o usuário possui permissão para utilizá-lo. Também é importante confirmar que a porta 5000 está disponível e liberada no firewall para o teste TCP.

Os exemplos deste relatório foram adicionados ao repositório, mas os comandos e resultados descritos como esperados ainda precisam ser executados e registrados pelo grupo. Portanto, não se afirma que os testes foram concluídos sem a validação prática.

## Considerações finais

A implementação apresenta três formas de comunicação: compartilhamento de uma fila entre threads, troca de dados entre processos por fluxos de entrada e saída e comunicação em rede por sockets TCP. Com isso, é possível observar diferenças entre concorrência dentro de um mesmo processo, comunicação entre processos locais e troca de mensagens pela rede.

O trabalho permite praticar conceitos importantes de Sistemas Operacionais, como threads, processos, sincronização, comunicação entre processos e sockets. Recomenda-se que os próximos alunos executem cada etapa gradualmente, validem os resultados no ambiente Docker e registrem capturas de tela das execuções para documentar a experiência.
