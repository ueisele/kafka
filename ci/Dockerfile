FROM registry.access.redhat.com/ubi8/ubi-minimal:8.4 as build

# This affects how strings in Java class files are interpreted.
# We want UTF-8 and this is the only locale in the base image that supports it
ENV LANG="C.UTF-8"

RUN microdnf install -y dnf \
    && dnf update -y \
    && dnf install -y git openssl wget tar procps krb5-workstation iputils iproute hostname vim \
    # https://docs.azul.com/zulu/zuludocs/ZuluUserGuide/PrepareZuluPlatform/AttachYumRepositoryRHEL-SLES-OracleLinuxSys.htm
    && rpm --import https://www.azul.com/files/0xB1998361219BD9C9.txt \
    && dnf -y install https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm \
    && dnf clean all \
    && rm -rf /tmp/* \
    && useradd --no-log-init --create-home --shell /bin/bash appuser

# Zulu openJDK
ARG ZULU_OPENJDK_MAJOR=11
ARG ZULU_OPENJDK_MINOR
ENV ZULU_OPENJDK_PACKAGE="zulu${ZULU_OPENJDK_MAJOR}-jdk-headless"
ENV JAVA_HOME="/usr/lib/jvm/zulu${ZULU_OPENJDK_MAJOR}"
RUN dnf -y install ${ZULU_OPENJDK_PACKAGE}$([[ -n "${ZULU_OPENJDK_MINOR}" ]] && echo "-${ZULU_OPENJDK_MAJOR}.${ZULU_OPENJDK_MINOR}" ) \
    && dnf clean all \
    && rm -rf /tmp/*

# 7u301, 8u291, 11.0.11, 16 disabled TLSv1 and TLSv1.1 by default
# https://bugs.openjdk.java.net/browse/JDK-8256490
ARG REMOVE_FROM_DISABLED_ALGORITHMS
RUN for alg in $(echo $REMOVE_FROM_DISABLED_ALGORITHMS | sed "s/,/ /g"); do \
        security_file=$([[ -e ${JAVA_HOME}/conf/security/java.security ]] && echo ${JAVA_HOME}/conf/security/java.security || echo ${JAVA_HOME}/jre/lib/security/java.security); \
        sed -i "s/^\(jdk.tls.disabledAlgorithms=\)\(.*\)${alg},\(.*\)$/\1\2\3/" ${security_file}; \
    done

USER appuser

RUN mkdir -p /home/appuser/.gradle
VOLUME ["/home/appuser/.gradle"]

WORKDIR /home/appuser/kafka