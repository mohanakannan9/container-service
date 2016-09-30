# Functions and Features
To support the use cases.

## Get an image
### Docker Hub
* Connect to Docker Hub
* Accept user input via rest to pull an image from docker hub
* Present a UI to the user which allows him or her to pull an image from docker hub
* Allow custom docker repositories

### Load from a file
* Accept a tarball, save to an image
    * Via rest, with file in body
    * Via rest, with file at a url
    * Show a file picker UI to select a local tarball

## Save a command
* POST json
* Simple UI, big text field into which json is pasted
* Complex UI, in which a new command is built
* Read from docker labels
    * On [image load](#get-an-image) this will be attempted automatically
    * If the image already exists on the server, post to a rest path to trigger the labels to be read and parsed

## Launch a command
First, [save a command](#save-a-command).

### From a session
* POST to a rest path with session ID, command ID, and any required launch parameters
* Click a button to bring up a launch UI

### From an event
* POST to a rest path to register a command ID and an event type (currently session archive and scan archive).
* Trigger the event

## Stage input files
When the user launches the container, container service must know what files the container will need and place them where the container can read them.

### Know what files the container needs
* Command inputs must be labeled as "file" type.
* At launch, container service must be able to resolve a file path (presumably to a resource in the archive) for that input
* Those files must be [transported](#transport-the-files-to-the-execution-machine) to the execution machine
* The files must be bind mounted from their path on the execution machine into the container
* The value for each file input that will be used when launching the container is the file path inside the container where the files were mounted

### Transport the files to the execution machine
* If the docker server can read the XNAT archive, this is a noop. Container service can bind mount the files directly from the archive into the container, read only.
* If the docker server cannot read the XNAT archive, container service must know...
    * Where execution will take place
    * How to move files there

## Import output files
### Know what files the container will produce
* Command outputs can, at this time, only be files
* At launch, container service must be able to resolve an XNAT resource that will be created (or overwritten) with the contents of the container's output

### Save the files into the archive
* Before launch, a 'build' space must be created for the container's execution.
    * Container service must be able to create a writable space on the execution machine.
    * Container service must bind mount the writable space into the container
* After container is done, container service must transport the output files back from the execution machine to a space that XNAT can read
    * If XNAT can read the 'build' space on the execution machine, this is a noop.
    * If XNAT cannot read the 'build' space, container service must transport the output files to a space XNAT can read.
* Output files will be placed into an XNAT resource. Recall that container service already knows the resource because [it determined the resource before launch](#know-what-files-the-container-will-produce).
    * If necessary, container service must create the resource
    * Container service uploads the container's output files into the resource