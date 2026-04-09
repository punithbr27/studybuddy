FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy source code and libraries
COPY src/ src/
COPY lib/ lib/
COPY web/ web/

# Compile
RUN javac -cp "lib/sqlite-jdbc-3.45.1.0.jar:lib/postgresql-42.7.1.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar:src" \
    src/main/model/*.java \
    src/main/pattern/*.java \
    src/main/controller/*.java \
    src/main/server/*.java \
    src/main/Main.java

# Run
CMD ["java", "-cp", "lib/sqlite-jdbc-3.45.1.0.jar:lib/postgresql-42.7.1.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar:src", "main.Main"]
