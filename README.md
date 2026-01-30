apt-repo
========

[![CI](https://github.com/ctron/apt-repo/actions/workflows/ci.yaml/badge.svg)](https://github.com/ctron/apt-repo/actions/workflows/ci.yaml)

APT Repository Generator


Note:
To use the Zstd compression you have to add an additional dependency to the plugin like this:

<plugin>
    <groupId>de.dentrassi.build</groupId>
    <artifactId>apt-repo</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.github.luben</groupId>
            <artifactId>zstd-jni</artifactId>
        </dependency>
    </dependencies>
</plugin>
