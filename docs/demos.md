# Use case demos

## Prerequisites
These instructions will get you an XNAT with container service running at https://xnat-31.xnat.org. Throughout the text, I will refer to this XNAT root URL as ${XNAT}, so you can substitute your own different URL if you wish.

### XNAT 1.7
You need to have an XNAT 1.7 up and running. There are a lot of ways to do that; this is one way, in which .

* Clone the [xnat-web repo](https://bitbucket.org/xnatdev/xnat-web)
* Clone the [xnat-vagrant repo](https://bitbucket.org/xnatdev/xnat-vagrant)
* Create a directory on your local machine into which we can put jars that we want XNAT to run. I will call this `/path/to/plugins`.
* In `xnat-vagrant` create a file in `configs/xnat-dev/local.yaml` with the following contents:

```yaml
name: xnat
vm_ip: 10.1.7.31
shares:
  '/path/to/plugins':
    - '/data/xnat/home/plugins'
```

* Open a terminal to `xnat-vagrant`, and run the following command:

    $ ./run xnat-dev setup

* While that runs, open another terminal to `xnat-web`. Run the following command:

    $ ./gradlew war

* Create a file: `xnat-web/gradle.properties` with the following contents:

```properties
deployHost=xnat-31.xnat.org
deployPort=8080
deployContext=/
deployUser=deploy
deployPassword=deploy
```

* Once the `xnat-vagrant` setup is finished, in the `xnat-web` terminal, run the following command:

    $ ./gradlew cargoRedeployRemote

### Container service and dependencies
Right now the container service needs to be built from source. Once we have a release version, you can just download it.

Gather all the jars below into your local plugins directory `/path/to/plugins`. Then restart your XNAT 1.7.

#### Container Service jar
* Clone the [container-service repo](https://github.com/nrgXnat/container-service)
* Build the jar: `[container-service]$ ./gradlew clean jar`
* Copy the jar to your plugins directory `$ cp build/libs/containers-1.0-SNAPSHOT.jar /path/to/plugins/`.

#### Transporter jar
* Clone the [transporter repo](https://github.com/nrgXnat/xnat-transporter)
* Build the jar: `[xnat-transporter]$ ./gradlew clean jar`
* Copy the jar to your plugins directory `$ cp build/libs/transporter-1.0-SNAPSHOT.jar /path/to/plugins/`.

#### Docker client
* Download the **shaded** jar from [bintray](https://bintray.com/bintray/jcenter/com.spotify%3Adocker-client) or [maven central](http://search.maven.org/#search%7Cga%7C1%7Ccom.spotify.docker-client).

### Setup

#### Sitewide setup
If this is a new XNAT, you'll have to go through the sitewide setup. Log in and you'll be redirected to a screen with a bunch of options. The defaults are all fine, so hit "Save all".

#### Add docker server configuration to XNAT
If you aren't using the most basic docker/XNAT configuration (XNAT machine has read/write access to the docker socket in the default location) you need to tell XNAT how to reach your docker server. Use `curl` or some other HTTP client to POST a bit of JSON to XNAT:

    $ curl -u ${username}:${password} -X POST -H 'Content-type: application/json' -d '{"host": "'${DOCKER_HOST}'"}' ${XNAT}/xapi/docker/server

Where `${username}` and `${password}` are the credentials of an admin account, ${XNAT} is your XNAT's URL, and ${DOCKER_HOST} is your docker's URL (which could be http://... or https://... for remote docker servers, or unix://... if XNAT can read the docker socket). If your docker requires a certificate for communication, the certificate file must be readable by XNAT; change the body of the JSON that you post above to `'{"host": "'${DOCKER_HOST}'", "cert-path": "'${DOCKER_CERT_PATH}'"}'`.
