# JBoss Fuse Maven Plugin
The jboss-fuse-maven-plugin is used to deploy and test your JBoss Fuse application. There is also the ability to deploy application's dependencies and apply custom configuration.

## Goals Overview
* **fuse:start** downloads, applies custom configuration and starts JBoss Fuse.
* **fuse:deploy** deploys dependencies and applications.
* **fuse:shutdown** stops JBoss Fuse.

### fuse:start

```
mvn fuse:start
```

#### Description

Downloads, applies custom configuration and starts JBoss Fuse.
At the first run, downloads JBoss Fuse by https://repository.jboss.org/nexus/content/groups/ea/org/jboss/ and saves it in *M2_HOME* directory. JBoss Fuse zip file, is copied and unzipped in target directory.

#### Configuration

| Parameter | Type | Required | Description | Default |
|---|---|---|---|---|
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

#### Example

```xml
...
<configuration>
   <!-- Set the JBoss Fuse start timeout-->
   <timeout>120000</timeout>
   <cfg>
      <!-- Append javax.net.ssl.keyStoreType = pkcs12 property in etc/system.properties JBoss Fuse file -->
      <param>
         <option>APPEND</option>
         <destination>etc/system.properties</destination>
         <properties>
            <property>
               <name>javax.net.ssl.keyStoreType</name>
               <value>pkcs12</value>
            </property>
         </properties>
      </param>
      <!-- Replace pkcs12 with jks in etc/system.properties JBoss Fuse file -->
      <param>
         <option>REPLACE</option>
         <destination>etc/system.properties</destination>
         <target>pkcs12</target>
         <replacement>jks</replacement>
      </param>
      <!-- Copy  ~/conf/system.properties in etc/system.properties JBoss Fuse directory-->
      <param>
         <option>COPY</option>
         <source>~/conf/system.properties</source>
         <destination>etc/system.properties</destination>
      </param>
   </cfg>
</configuration>
...
```

### fuse:deploy

#### Description

Deploys dependencies and applications.
Copy dependencies file (*.jar*, *.xml*) in JBoss Fuse deploy directory and wait until deployed bundles state is *Active*.


#### Configuration

| Parameter | Type | Required | Description | Default |
|---|---|---|---|---|
| deployments | List | False | The deployment list to deploy in JBoss Fuse | null |

##### deployment Parameter

| Option | Type | Rquired | Description | Default |
|---|---|---|---|---|
| source | String | True | The deployment file to be copied in JBoss Fuse deploy directory ||
| expectedStatus | String |False| The expected bundle status |.*Active|
| expectedContextStatus | String | False| The expected bundle context (blueprint, spring) status | null|
| deploymentName | String |False| The bundle name that will show on JBoss Fuse console| source file name |
| timeout | Long | False | The timeout, in milliseconds, to wait for bundle's status until it doesn't matches to *expectedStatus* and *expectedContextStatus*  | 60000 |
| waitTime | Long | False | The time, in milliseconds, to wait for check bundle's status | null|

#### Example

```xml
...
<configuration>
   <deployments>
      <param>
         <source>src/main/resources/features-to-install.xml</source>
      </param>
      <param>
         <source>target/test-datasources-bundle.jar</source>
         <expectedContextStatus>Created</expectedContextStatus>
         <deploymentName>test-datasources</deploymentName>
      </param>
      <param>
         <source>target/${project.build.finalName}.jar</source>
         <expectedContextStatus>Created</expectedContextStatus>
         <deploymentName>${project.name}</deploymentName>
         <timeout>120000</timeout>
      </param>
   </deployments>
</configuration>
...
```
### fuse:shutdown

```
mvn fuse:shutdown
```

#### Description

Stops JBoss Fuse

## Examples
```xml
<?xml version="1.0" encoding="UTF-8"?>
<plugin>
   <groupId>it.imolinfo.maven.plugins</groupId>
   <artifactId>jboss-fuse-maven-plugin</artifactId>
   <version>1.0.1</version>
   <executions>
      <execution>
         <id>Start Fuse</id>
         <goals>
            <goal>start</goal>
         </goals>
         <phase>pre-integration-test</phase>
         <configuration>
            <cfg>
               <param>
                  <option>APPEND</option>
                  <destination>etc/system.properties</destination>
                  <properties>
                     <property>
                        <name>javax.net.ssl.keyStoreType</name>
                        <value>pkcs12</value>
                     </property>
                  </properties>
               </param>
               <param>
                  <option>REPLACE</option>
                  <destination>etc/system.properties</destination>
                  <target>bind.address=0.0.0.0</target>
                  <replacement>bind.address=127.0.0.1</replacement>
               </param>
               <param>
                  <option>COPY</option>
                  <source>~/conf/users.properties</source>
                  <destination>etc/users.properties</destination>
               </param>
            </cfg>
         </configuration>
      </execution>
      <execution>
         <id>Deploy</id>
         <phase>pre-integration-test</phase>
         <goals>
            <goal>deploy</goal>
         </goals>
         <configuration>
            <deployments>
               <param>
                  <source>src/main/resources/features-to-install.xml</source>
               </param>
               <param>
                  <source>target/test-datasources-bundle.jar</source>
                  <expectedContextStatus>Created</expectedContextStatus>
                  <deploymentName>test-datasources</deploymentName>
               </param>
               <param>
                  <source>target/${project.build.finalName}.jar</source>
                  <expectedContextStatus>Created</expectedContextStatus>
                  <deploymentName>${project.name}</deploymentName>
                  <timeout>120000</timeout>
               </param>
            </deployments>
         </configuration>
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
