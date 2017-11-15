## Background and Problem

There are several use cases where it would be nice to run a little bit of code before launching a container.

One such case is when running the BIDS Apps (CS-237). The _raison d'être_ of the BIDS Apps is that they run on BIDS-formatted data. The user launches them by sending in a path, and it is implied that the data found at that path will be formatted in the BIDS structure. However, this is incompatible with how the container service typically works, which is by mounting data directly from the archive into a container and kicking off a process. When our data is in the archive it isn't stored in the BIDS structure, it is stored in the XNAT archive structure.

We do have a container ([xnat/dcm2bids]()) that ostensibly converts DICOM data in XNAT to BIDS format. But it merely generates the NIFTI files and BIDS JSON metadata; it keeps all that data in the archive format rather than storing it all at rest in BIDS format. Even after running this container on our data we still can't launch any BIDS Apps directly, because they would still not be able to read the data in the XNAT archive format. The dicom2bids container gets us half way to launching BIDS Apps; we need an extra push to get all the way.

## Solution: Setup Commands
The way we solve this problem is by introducing a new type of command called a "Setup Command". The job of a Setup Command will be to take data that is in the XNAT archive format and stage it / move it / convert it into whatever format is required for the main command. The author of a "Main" Command (for instance, a command that describes a BIDS App) who has a need for a Setup Command will have to first create the Setup Command (and the underlying docker image that defines the Setup Container) and reference it in their Main Command. At launch time, the Container Service will launch a container from the Setup Command before launching the main container from the Main Command, giving the setup container a chance to set up the files in the right way for the main container.

## How a Setup Command Changes the Container Launch Flow

When the Container Service goes to launch a container from a Command, if it sees that the Command makes reference to a Setup Command, it will insert a setup container before launching the main container. To see where and how that happens, let us examine the launch sequence with and without a Setup Command.

### Without Setup Command

First we will examine the Command Resolution and Container Launch sequence for a Command that does not reference a Setup Command. Here is a (very abbreviated) version of finding the file paths for a mount.

1. The Command has a mount named `input`. The mount defines the path inside the container, which we will call `p`.
2. The Command Wrapper has an input which provides files for mount `input`.
3. That Command Wrapper input has a value which is some XNAT object. Find its path in the archive. Call that `A`.
4. We will mount the archive path `A` to the container path `p`.

Once the rest of the Command Resolution process is complete, the container will be launched with a mount which takes path `A` outside the container to path `p` inside the container.

### With Setup Command

Now we will examine the same process, only this time the Command Wrapper input references a Setup Command.

1. The Command has a mount named `input`. The mount defines the path inside the container, which we will call `p`.
2. The Command Wrapper has an input which provides files for mount `input`.
3. That Command Wrapper input has a value which is some XNAT object. Find its path in the archive. Call that `A`.
4. The Command Wrapper input has a value for its `"via-setup-command"` property. We must now resolve that Setup Command.
5. A Setup Command always has two mounts, with inside-the-container paths `/input` and `/output`.
6. We will mount the archive path `A` to the setup container's `/input`.
7. We make a new writable directory `B` and mount it to the setup container's `/output`.
8. We mount the exact same path `B` to the main container's mount path `p`.

Once the rest of the Command Resolution process is complete, we will create two containers:

1. The setup container. This will have two mounts:
    1. The archive path `A` to `/input`
    2. A writable directory `B` to `/output`
2. The main container, which has one mount: `B` to `p`.

Whatever the setup container writes to its output is used as the input to the main container's mount.

<ac:structured-macro ac:name="tip" ac:schema-version="1" ac:macro-id="9374bcc1-9eb9-457e-a5ee-8786bd50f97a"><ac:rich-text-body>
Though we _create_ the setup and main containers at the same time, we only _launch_ one: the setup container. Once the setup container is complete, the Container Service will launch the main container. If the setup container fails, the main container will not be launched, but will receive a status "Failed Setup".
</ac:rich-text-body></ac:structured-macro>

## Creating a Setup Docker Image
<!-- TODO -->

## Writing a Setup Command
<!-- TODO -->
The setup command will not have the full features of a standard command. It cannot define any inputs, outputs, mounts, or wrappers. It can only define its command line string. When launched, the setup command will be given the input files mounted read-only at `/input`, and an empty writable mount at `/output`. It will also be given the Resolved Command, serialized into a JSON file, at `/resolved-command.json`; this is to allow the setup container to read any of the original command's inputs or outputs in case it needs to figure anything out.


setup commands must be XNAT-ready, i.e. their command definitions must be in the labels.

## Referencing a Setup Command in a Main Command

<!-- TODO -->
