<!-- id: 53674225 -->

## Docker

[Docker](https://www.docker.com) is a piece of software that manages [images](#image) and [containers](#container). The software is produced by a corporation, [Docker, Inc.](https://www.docker.com/company), based on a core open-source project (which used to also be called Docker but is now called [Moby](https://github.com/moby/moby)).

Docker runs as a daemon process. It can be run on anything from servers to a laptop. It can be run on the same machine as XNAT, or a remote machine, or XNAT can even be run as a container by docker itself! (See the [xnat-docker-compose](https://github.com/NrgXnat/xnat-docker-compose) project for information about that.)

## Image

> [An] image is a lightweight, stand-alone, executable package of a piece of software that includes everything needed to run it: code, runtime, system tools, system libraries, settings.

From [What is a Container](https://www.docker.com/what-container)

The image is the distributable artifact of the author of a piece of software or an application. If you want some piece of software to be executed through the container service, it must first be packaged into a docker image.

## Container

A container is a particular instance of an image. The image is immutable; it gets pulled down or built and sits there unchanging. The container, on the other hand, is alive. It is born when we tell docker to run some command in an image. It lives as long as that process we started lives. Then, when the process ends, the container dies. Running another command from the same image will create a new container with its own life cycle.

## Container Service

The Container Service is an XNAT Plugin. It gives XNAT users the ability to launch processing jobs as docker containers, provided there is a suitable docker image to do that job which can be described by a [command](#command).

The Container Service is compatible with XNAT versions greater than or equal to 1.7.3. You install the Container Service by putting the "fat" jar (i.e. a jar packaged with dependencies) into your XNAT's `plugins` directory. You can always find the latest Container Service fat jar on the [Releases](https://github.com/NrgXnat/container-service/releases/latest) page of the [Container Service GitHub repository](https://github.com/NrgXnat/container-service).

<ac:structured-macro ac:name="tip" ac:schema-version="1" ac:macro-id="9374bcc1-9eb9-457e-a5ee-8786bd50f97a"><ac:rich-text-body>
The Container Service plugin requires XNAT 1.7.3 or later, but it is tested for compatibility with the most recent version of XNAT. We always recommend running the latest version of XNAT.
</ac:rich-text-body></ac:structured-macro>

## Command

The Command is a structured metadata object that describes how to run a docker image. It describes the image, its inputs and outputs, and where it expects to find files.

You can read a lot more about the Command and how to write one [here](https://wiki.xnat.org/display/CS/Command).

## Command Wrapper

Also known as "wrapper" for short. This is a structured metadata object that describes how to take XNAT data—in the form of XNAT objects like Projects and Sessions and so on, their properties, and their files—and give those things to a Command's inputs and mounts.

Any docker image could be described by a Command. The format is intended to be generic. But the true utility of the Container Service comes when a Command also has one or more Command Wrappers. The Wrappers are what enable the Container Service to show prompts to launch containers from various XNAT pages, to automatically pull data from XNAT objects and feed it to containers, and to store the resulting container output files back into XNAT.

Why is it named the Command "Wrapper"? Because it "wraps" around the Command's inputs and outputs. The Command's inputs and outputs are intrinsic to the docker image described by the Command. The Wrapper puts an XNAT-y layer around the Command. The inputs of the combined Command + Wrapper are XNAT objects. It doesn't have any outputs because all the Command's outputs must be uploaded back to some XNAT input object.

The Command Wrapper as an object is dependent on a Command object, so it is stored inside the Command. As such, there is more information about the Command Wrapper format and how to write one on the [Command](https://wiki.xnat.org/display/CS/Command) page.

## XNAT-Ready Docker Image

In the simplest form, a docker image and the Command that descibes it are separate entities. The docker image gets pulled down from Docker Hub or built from a Dockerfile, and the Command lives in a JSON file or inside an XNAT database. They are linked in that they describe the same thing, but they lead independent lives. Wouldn't it be nice if you could package the two things together?

You can package the Command definition with the docker image; we call this making the image "XNAT ready". In short, you have to write the Command JSON into the image's "labels".

You can find out more about this on the wiki: [Making your Docker Image "XNAT Ready"](https://wiki.xnat.org/pages/viewpage.action?pageId=38339164).

## Docker Swarm

[Docker Swarm](https://docs.docker.com/engine/swarm/) is Docker's native solution for administering a cluster of Docker "nodes". In this way you can have multiple compute machines that can receive and process jobs through Docker.

The Container Service supports executing Docker Swarm with a simple checkbox. To turn on or off Docker Swarm mode in the Container Service, simply go to Administration > Plugin Settings, and edit the Docker server configuration.