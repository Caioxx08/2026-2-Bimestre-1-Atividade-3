FROM eclipse-temurin:17-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl gzip \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fL https://github.com/VirtusLab/scala-cli/releases/latest/download/scala-cli-x86_64-pc-linux.gz \
    | gzip -d > /usr/local/bin/scala \
    && chmod +x /usr/local/bin/scala

WORKDIR /app
COPY src ./src

EXPOSE 5000

CMD ["scala", "run", "src/scala/ProdutorConsumidor.scala"]
