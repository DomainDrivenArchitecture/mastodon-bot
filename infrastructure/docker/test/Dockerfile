FROM mastodon-bot

RUN mkdir -p /usr/share/man/man1

RUN apt update && apt -yq install apt-utils openjdk-11-jre-headless curl > /dev/null

RUN curl -L -o /tmp/serverspec.jar https://github.com/DomainDrivenArchitecture/dda-serverspec-crate/releases/download/2.0.1/dda-serverspec-standalone.jar

COPY serverspec.edn /tmp/serverspec.edn

RUN java -jar /tmp/serverspec.jar /tmp/serverspec.edn -v
