#!/bin/bash

# Study Buddy - Run Script
# This script compiles and starts the server WITHOUT clearing the database.

echo "🔨 Compiling Study Buddy..."
javac -cp "lib/sqlite-jdbc-3.45.1.0.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar:src" src/main/model/*.java src/main/pattern/*.java src/main/controller/*.java src/main/server/*.java src/main/Main.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo "🚀 Starting server..."
    java -cp "lib/sqlite-jdbc-3.45.1.0.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar:src" main.Main
else
    echo "❌ Compilation failed. Please check for errors."
fi
