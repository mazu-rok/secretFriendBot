FROM openjdk:17
LABEL Description="Secret friend telegram bot"

COPY ./target/secret-friend-bot.jar /workspace/secret-friend-bot.jar

WORKDIR /workspace
CMD java $JAVA_OPTS -jar ./secret-friend-bot.jar