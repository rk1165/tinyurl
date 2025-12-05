PORT ?=8080
run:
	./gradlew bootRun -Dserver.port=$(PORT)

debug:
	./gradlew bootRun --debug-jvm

build:
	./gradlew clean build

clean:
	mysql -u root < sql/clean.sql

PHONY: run debug build clean