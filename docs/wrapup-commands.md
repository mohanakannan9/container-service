## Background and Problem

There are several use cases where it would be nice to run a little bit of code on the files produced by a container before they get uploaded to XNAT.

Consider two such cases related to Freesurfer: snapshots and assessor XML generation. The Freesurfer process creates a lot of images and a lot of data. To make those 3D images easily viewable on a report page we can generate a series of 2D "snapshots"; we can run a script that loads the 3D images, steps through them on different planes, and saves out the snapshot image series. To make the data easily searchable, we can add it to the XNAT database using an assessor datatype; we can run a script on the output data files, reading them in and writing them into an XML structured document according to the datatype schema.

We could wrap all these functions into one large docker image. It could bundle up Freesurfer and all the XNAT-specific post-processing scripts we want to run. However, this falls short of the modularity goals we have for the container service. We want to be able to use an off-the-shelf Freesurfer docker image to generate the Freesurfer files, then run our own separate post-processing images as needed.

## Solution: Wrapup Commands

This is the use case for the wrapup image and the wrapup command that descibes it. We can run a "main" container which will generate some output files. Then before those files are uploaded to XNAT we can run a "wrapup" container on them, which will process the output files using some additional docker image. We could have one small wrapup container image to generate the assessor XML from the output files, and another to generate the Freesurfer snapshots.

To include a wrapup command in your command (see [full section below](#referencing-a-wrapeup-command-from-a-main-command)) you need to include the `"via-wrapup-command"` on a wrapper's `output-handler`. This will trigger the container service to mount the command output—which is to be handled by the output hander—to the wrapup container's `input` mount, and a writable directory to the wrapup container's `output` mount.

## When Wrapup Commands are Launched

When the container service launches a "main" container, it notes down information about that container in the database. It periodically checks in with docker for new information about the containers, updating any stored information about the containers it knows. When it detects that a container has finished, it starts running a process to get the container's logs and output files. It is at this point that the wrapup containers come in.

When the container service sees that a main command has requested a wrapup command be used on its output handlers, the main container's finalization process will be stopped. Any wrapup commands that are used by the main command will have containers launched. All these wrapup containers are started in parallel. Once all of the wrapup containers finish, the container service will resume the process of finalizing the main container.

## Writing a Wrapup Command

The wrapup command is a command, and as such it must follow the [command definition](https://wiki.xnat.org/display/CS/Command). However, wrapup commands cannot use the full set of features that most commands can. Wrapup commands cannot define any inputs, outputs, mounts, or wrappers. The mounts are always the same, `/input` and `/output`, so they need not be specified.

For a command to be recognized as a setup command, it **must** have the property

    "type": "docker-wrapup"

This is in contrast to standard commands, which have `"type": "docker"` (usually implicitly, since that is the default value).

The only properties that can be set in a setup command are

* `name` (**required**)
* `description` (optional, but recommended)
* `label` (optional, but recommended)
* `version` (optional, but recommended)
* `type` (**required** - must always have the value `"docker-wrapup"`)
* `command-line` (**required**)
* `working-directory` (optional)

Here is an expamle command JSON for a wrapup command.

    {
        "name": "wrapup",
        "description": "A wrapup command.",
        "version": "1.0",
        "type": "docker-wrapup",
        "command-line": "my-wrapup-script /input /output"
    }


## Referencing a Wrapup Command from a Main Command

This is a simple matter of setting one property on a Command Wrapper Output Handler. The Output Handler already references a Command Output, which in turn references a Command Mount. By setting a value for the Command Wrapper Output Handler property `"via-wrapup-command"`, the container service will take the files from the Command Output and mount them to `/input` in a container created from the indicated wrapup command, along with a writable directory mounted to `/output`.

The value for the `"via-wrapup-command"` property should be in the docker image format: `repo/image:version`. Additionally, we support one additional property in this value: the command name. This allows you to create one wrap image which can have multiple commands. The full format of the `"via-wrapup-command"` property is `repo/image:version:commandname`.

For example, say we used the command [in the section above](#writing-a-wrapup-command) to describe an image `xnat/wrapper-command:1.0`. Here is a snippet from an example command that references that wrapup command via the property `"via-wrapup-command": "xnat/wrapup-image:1.0:wrapup"`. Note also that this command has two output handlers for the same command output, one which uses a wrapup command and one which does not.

    {
        "name": "command-that-uses-a-wrapup-command",
        ...,
        "mounts": [
            {
                "name": "output-files",
                "path": "/output"
            },
            ...
        ],
        "outputs": [
            {
                "name": "command-output",
                "mount": "output-files"
            }
        ]
        ...,
        "xnat": [
            {
                "name": "wrapper-that-uses-wrapup-command",
                ...
                "output-handlers": [
                    {
                        "name": "output-handler-with-wrapup-command",
                        ...
                        "accepts-command-output": "command-output",
                        "via-wrapup-command": "xnat/wrapup-image:1.0:wrapup",
                        "label": "POST_PROCESSED"
                    },
                    {
                        "name": "vanilla-output-handler",
                        ...
                        "accepts-command-output": "command-output",
                        "label": "PROCESSED"
                    }
                ],
                ...
            }
        ]
    }

