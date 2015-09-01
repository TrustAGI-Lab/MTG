#-----------------------------------------------------------------------
# File    : makefile    (directory: moss)
# Contents: build molecular substructure miner (MoSS aka MoFa)
# Author  : Christian Borgelt
# History : 16.07.2002 file created
#           13.05.2006 other compilers added
#           18.07.2006 creation of a jar for the GUI version added
#           25.10.2006 creation of javadoc added
#-----------------------------------------------------------------------
SHELL=/bin/bash

all:        moss

#-----------------------------------------------------------------------
# Molecular Substructure Miner
#-----------------------------------------------------------------------
moss:   *.java
	cd ..; javac -Xlint moss/MoSS.java
#	cd ..; \
#       gcj -O3 -Wall --main=moss.MoSS  moss/*.java -o moss/mossgui; \
#       gcj -O3 -Wall --main=moss.Miner moss/*.java -o moss/moss
#	cd ..; jikes +P +Pall moss/MoSS.java

# The jikes command is a relict from earlier versions. Since the
# current code uses generics, but jikes has not been updated to be
# able to compile generics, jikes cannot compile the current code.

#-----------------------------------------------------------------------
# Distribution Packages
#-----------------------------------------------------------------------
dist:
	$(MAKE) clean jdoc src jdclean
	$(MAKE) moss jar clean

src:
	cd ..; \
        zip -r -q moss.zip moss/{*.java,makefile,run,bench,doc} \
                           moss/data/{*.smi,*.sln,example.nel} \
                           -x moss/data/ic93.sln

jar:
	cd ..; \
	echo "Main-Class: moss.MoSS" > manifest; \
	jar cfm moss.jar manifest moss/*.class; \
	rm -f manifest

#-----------------------------------------------------------------------
# Documentation
#-----------------------------------------------------------------------
jdoc:
	cd doc/java; rm -rf *; \
        javadoc -quiet -classpath ../../.. moss

#-----------------------------------------------------------------------
# Clean up
#-----------------------------------------------------------------------
clean:
	rm -f *.class *.sub *.ids *.tmp moss mossgui

jdclean:
	rm -rf doc/java/*
