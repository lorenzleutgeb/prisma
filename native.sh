#!/bin/bash

# Give up on errors.
set -e

# Make sure we have built a JAR.
gradle generateGrammarSource bundleJar

mkdir -p build/native

# Compile!
native-image -jar build/libs/prisma-bundled.jar -H:Name=build/native/prisma
