# JBoss Fuse Maven Plugin
The jboss-fuse-maven-plugin is used to deploy and test your JBoss Fuse application. There is also the ability to deploy application's dependencies and apply custom configuration.

## Goals Overview
* **fuse:start** downloads, applies custom configuration and starts JBoss Fuse.
* **fuse:shutdown** stops JBoss Fuse.

### fuse:start

```
mvn fuse:start
```

#### Description

Downloads, applies custom configuration, installs dependencies and starts JBoss Fuse.
At the first run, downloads JBoss Fuse by https://repository.jboss.org/nexus/content/groups/ea/org/jboss/ and saves it in *M2_HOME* directory. JBoss Fuse zip file, is copied and unzipped in target directory.

### fuse:shutdown

```
mvn fuse:shutdown
```

#### Description

Stops JBoss Fuse


### Configuration

| Parameter | Type | Required | Description | Default |
|---|---|---|---|---|
| etc | String | False | The cfg files list to copy in the etc directory| null |
| features | String | False | The features list to install | null |
| bundles | String | False | The bundles list to install | null |
| cfg | List | False | The configuration list to apply to JBoss Fuse | null |
| timeout | Long | False | The timeout, in milliseconds, to wait for until JBoss Fuse is started | 60000 |

##### cfg Parameter

| Option | Type | Description |
|---|---|---|
| APPEND | String | Appends properties in the destination file |
| REPLACE | String | Replaces target string with replacement string in the destination file |
| COPY | String | Copies the source file in the destination directory |

###### APPEND Option
Appends properties in the destination file

| Parameter | Type | Required | Description |
|---|---|---|---|
|destination|String|True|The destination file path|
|properties|List|True|List of properties to append to destination file|

###### REPLACE Option
Replaces target string with replacement string in the destination file

| Parameter | Type | Required | Description |
|---|---|---|---|
|destination|String|True|The destination file path|
|target|String|True|The string to be replaced|
|replacement|String|True|The replacement string|

###### COPY Option
Copies the source file in the destination directory

| Parameter | Type | Required | Description |
|---|---|---|---|
|source|String|True|The file to be copied in destination directory |
|destination|String|True|The destination file path|

## Examples
```xml
<?xml version="1.0" encoding="UTF-8"?>
<plugin>
	<groupId>it.imolinfo.maven.plugins</groupId>
	<artifactId>jboss-fuse-maven-plugin</artifactId>
	<version>2.0.1</version>
	<configuration>
    	<etc>
        	cfg/it.imolinfo.maven.plugins.samples.cfg,<!-- Copy cfg file in the etc directory -->
        	cfg/user.properties <!-- Override user.properties file -->
    	</etc>
    	<features>
        	camel-http4,
        	camel-http,
        	camel-cmis
    	</features>
    	<bundles>
        	mvn:org.jgroups/jgroups/3.6.11.Final,
        	file://${basedir}target/bundle.jar
    	</bundles>
	</configuration>
	<executions>
    	<execution>
        	<id>Start Fuse</id>
        	<phase>pre-integration-test</phase>
        	<goals>
          		<goal>start</goal>
        	</goals>
    	</execution>
    	<execution>
        	<id>Shutdown Fuse</id>
        	<phase>post-integration-test</phase>
        	<goals>
            	<goal>shutdown</goal>
        	</goals>
    	</execution>
	</executions>
</plugin>
```
