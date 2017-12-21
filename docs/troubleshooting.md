<!-- id: 53674156 -->

Answers to problems you might see when using the container service. If you have a different question, or you have one of these questions but the answers do not help, let us know! Leave a comment on this page or post a question on the [xnat_discussion google group](https://groups.google.com/forum/#!forum/xnat_discussion).

## My command isn't showing up in the Run Container menu

There could be a lot of reasons for this, so there will be a lot of things to check.

### Has your command been added to the site?

A site administrator will have to verify this. Go to `Administer > Plugin Settings > Images and Commands`. Verify that the image and command you expect to see shows up there.

If they are not there, you need to add the image to your docker server, add the command to the site, and enable on your project.

### Has the command wrapper been enabled on your project?

A project owner or site admin will have to verify this. Go to the project's page, then click `Project Settings` in the Actions box. Click `Command Configurations`. Make sure the command wrapper that you expect to see shows up there and is enabled.

### Does the wrapper have a description?

This is a known bug in the container service that we intend to fix soon. If your wrapper does not have anything set in its `"description"` field, it will not show up in the Run Container menu. A site admin can fix this by edit the command (in `Administer > Plugin Settings > Images and Commands`, find the command, hit the Edit button), find the wrapper within the command JSON, and add any description to the wrapper.

### Does your page's XSI type match the wrapper's context?

To verify this one, you need to know what you are looking at. A session? A subject? An assessor? What kind? Is it an MR session? A PET session? A Freesurfer assessor? Each of these different kinds of items has what we call an XSI type. A wrapper can provide a set of XSI types on which it can be run in its `"contexts"` property.

These are the checks that have to pass for a wrapper to show up in the Run Containers menu:

* If the wrapper does specify something in its `"contexts"`, then the XSI type of the thing you are looking at has to match one of those.
* If the wrapper has nothing in its `"contexts"`, but it does have one external input, that external input defines a default context. The XSI type of the thing you are looking at has to match that.
* If the wrapper has zero external inputs, or more than one, it will not show up in the Run Containers menu.

## When I launch my command I get the error message "Failed to Start"

When you get the message `"Failed to start"` it means that the container service was able to resolve your command, and could _create_ a container from it, but could not _start_ that container.

Usually this means something is wrong with your image. You should test whether you can run commands in your image manually. If you see any errors that look like this:

    docker: Error response from daemon: OCI runtime create failed: container_linux.go:295: starting container process caused "exec: \"foo\": executable file not found in $PATH": unknown.

...that indicates that whatever command you tried to run could not be run.

Note that, whenever the container service runs a command in a container, it does two things:

1. Overrides any `entrypoint` in the image.
2. Executes the resolved command inside a shell. Let me elaborate on that.
    1. The command says to run the command-line `foo #arg1# #arg2#`.
    2. You send in the input arguments `arg1=a`, `arg2=b`.
    3. The resolved command has the command-line `foo a b`.
    4. The container will run the command `/bin/sh -c "foo a b"`.

Make sure your image has a functioning shell at `/bin/sh`, or this will not work. (If it does not have a shell, this likely means something is very wrong.)

## My command launched a container, but it didn't do anything

This could be several things.

### The container started and failed

A site administrator needs to get the container's logs. (Sorry that this is limited to site admins. The reason is that there is some potentially sensitive information there, so we can't just allow anyone to do this. It is a feature we want to open up more in the future.)

Go to `Administer > Plugin Settings > Command History`. Find the row for the container execution that failed. The command column is a link; click it. That will bring up a dialog with lots of information about this container execution. At the bottom will be buttons to view the stdout and stderr logs.

Review the logs and try to figure out what went wrong.

### The container seemingly ran to completion, but I didn't get any files

In order for the container service to upload files for you, a lot of things need to be set up the right way, and the command and wrapper need to have the right properties.

* The command needs to have a `mount`
* Whatever you run in your container needs to write stuff to the location designated in that `mount`
* Your command needs to have an `output` that refers to the `mount`.
* Your wrapper needs to have some external input
* Your wrapper needs to have an `output handler` that takes files from the `output`, and uploads them as a new resource on the external input.

If all that is true, and you're still not seeing any files, let us know!
