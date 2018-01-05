<!-- id: 53674023 -->

When you launch a container with the container service, what happens in there? How does it take your inputs and produce a container?

The first thing that happens is that it pulls up the command and wrapper you specified. It uses your input arguments to "resolve" the command and wrapper. That is a complicated process and has its own page: [Command Resolution](https://wiki.xnat.org/display/CS/Command+Resolution). This page is about what happens _after_ command resolution.

Three environment variables are defined for the container: `XNAT_HOST`, `XNAT_USER`, and `XNAT_PASS`. `XNAT_HOST` is the URL of the XNAT, while `XNAT_USER` and `XNAT_PASS` are an alias token which can be used as a substitute for the username and password. These are provided in case you need to connect back to the XNAT REST API from inside your container.

Next the container (or, if the docker server is set to swarm mode, the service) is created. Most everything is created exactly as you would expect from the resolved command:

* The container's image is set to the `image` field of the command
* The environment variables are set directly from the command, plus the `XNAT_*` variables mentioned above
* The resolved paths of the mounts are mounted
* The resolved ports are set
* The working directory is set

The only potentially tricky bits are these:

* The resolved command-line string is prefixed with `/bin/sh -c`. So if the command-line string that got resolved was `foo arg1 arg2`, then the command that will be run in the container is `/bin/sh -c "foo arg1 arg2"`.
* Any `entrypoint` from the parent image is overriden. This is for containers only; services already override entrypoints.
* For services, the restart condition is set to `"none"`. This is how we are able to execute the command once and only once.
* For services, the number of replicas is set to 0. This allows us to split the acts of creating and starting the service in the same way we do for containers.

Once the container or service is created, we create a `Container` object in the database to record properties of the launch.

If the container does not require any setup containers to run first, then the container or service we just created is started. For containers, this is a simple API call; for services, we update the service spec to have `replicas=1`.

If, on the other hand, setup containers are required to run before the main container, the setup containers are launched at this time. Their statii are updated periodically. Once they all finish successfully, the main container or service is started.
