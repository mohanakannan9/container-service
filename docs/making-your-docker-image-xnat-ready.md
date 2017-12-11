<!-- id: 38339164 -->

If you have a docker image and a command that you want to execute with the XNAT Container Service, you can smooth the process of installation by making the docker image "XNAT ready". By that, we mean that your image is packaged along with the command metadata. When an XNAT user pulls down the image, the Container Service is able to read the commands and save them into the database, and they can start running containers right away.

Making an image XNAT ready is simple a pretty simple process. At a high level, here are the steps:

1. Serialize your command(s) from JSON into one string.
2. Save a label on your docker image with the key `"org.nrg.commands"` and the value the command string from step 1.

And that's all there is! If you push your image up to docker hub, the commands will go along as part of the image's labels. Then when you or another user pulls the image down, the container service will read the labels and install the command; see ([Installing a Command from an XNAT-Ready Image](#installing-a-command-from-an-xnat-ready-image)).

## Serializing commands to a string
To serialize the command JSON to a string, all the double quote (`"`) characters need to be escaped (`\"`) and the whole thing surrounded by double quotes. Instead of the command looking like this

    {
        "property": "value", ...
    }

it needs to look like this

    "[{\"property\": \"value\"}]"

This is a task that is simple in principle but tedious in practice. To smooth the process, we wrote a python script to convert a command JSON file to a string: [command2label.py](https://github.com/NrgXnat/docker-images/blob/master/command2label.py). This script takes in the paths to one or more JSON files, which should contain command definitions that apply to a particular docker image. It will read them all, serialize them to a string, and print them to the screen as a Dockerfile `LABEL` command. And to use that, see the [next section](#writing-the-commands-to-the-image-labels).

Note that, even if you only have one command, you should still serialize it as a single-item list. When the container service goes to read this value, it will attempt to deserialize the value it reads as a list of commands. So, in other words, your command(s) serialized into a string should always look like `"[one or more commands in here]"`.

## Writing the commands to the image labels
Once you are able to [serialize your commands to a string](#serializing-commands-to-a-string), you need to write that string into your docker image's labels. [Docker labels](https://docs.docker.com/engine/userguide/labels-custom-metadata/) are "a mechanism for applying metadata to Docker objects". We will use them as a way to save the commands along with the image. Then wherever the image goes, the commands go too.

The way you write the commands to the image labels is to use the `LABEL` instruction in the image's Dockerfile. The syntax for the Dockerfile instruction is

    LABEL key="value"

To make the image XNAT ready, the `value` will be a list of commands and the `key` will be `org.nrg.commands`. When you build your image with this instruction in the Dockerfile, the commands will be written into the image's labels. When an XNAT user pulls that image down through the container service, the container service will check the image's labels for the key `org.nrg.commands`. If it finds a value, it tries to deserialize it as a list of commands. If that succeeds, it saves all those commands into the database. In one step, the user can go from having no docker image to having your image and as many commands as are defined for your image, ready to run.

## Examples
You can see several examples in the various Dockerfiles in the [github.com/nrgxnat/docker-images](https://github.com/nrgxnat/docker-images) repo.

## Installing a Command from an XNAT-Ready Image

You can do this in two different ways, depending on if you need to pull down the image from Docker Hub or if you have the image already on your docker server.

When you pull down an XNAT-Ready image from Docker Hub using the container service, the commands in the labels will be read by default. You can pull an image from Docker Hub using the UI; click the `Add new Image` button in `Administer > Plugin Settings > Container Service > Commands and Images`. Provide the image name (`account/image`) and the version. You can also use the REST API directly; `POST` to `/xapi/docker/pull` with the query param `image=(account)/(image):(version)`.

Alternatively, if the image is already on your Docker server, you can instruct the Container service to look for Commands in its labels. There is no way to do this through the UI, only through the REST API. Issue a `POST` to `/xapi/images/save` with the query param `image=(account)/(image):(version)`.

Once you do one of these things (a new image is pulled down, or you `POST` to `/xapi/images/save`) the Container Service will check in the image's labels for one with the key `"org.nrg.commands"`. If it finds that, it will parse the value as a list of Commands. If that succeeds, it will attempt to save each command in the list.
