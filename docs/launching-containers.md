<!-- id: 38338912 -->

# Launch a Container
When a user wants to use the container service to launch a container from a command, we provide REST API endpoints to do so:

    Request (one of)
        POST /xapi/commands/{id}/wrappers/{name}/launch
        POST /xapi/wrappers/{id}/launch
        POST /xapi/projects/{project}/commands/{id}/wrappers/{name}/launch
        POST /xapi/projects/{project}/wrappers/{id}/launch
    Headers
        Content-type: application/json
    Body
        {"paramName": "paramValue", ...}

The different APIs can be used for very slightly different purposes. The `/projects/{project}` APIs are suited for launching containers within the context of a project, which means the project-specific configuration will be used. The non-project APIs will use the site-wide configuration.

These endpoints are the fastest way to go directly from zero to launching a container. But they aren't very usable by anyone but expert users because they require complete foreknowledge. The user must know

* either the ID of the command + name of the command wrapper or the ID of the wrapper,
* which of the container's parameters must be supplied at runtime, and
* all the appropriate values to give those parameters.

Clearly, there should be a simpler way to launch containers. Users should be required to know as little as possible about what they want to launch, and the UI can help them through the process as much as possible. There should be a multi-stage process a user can step through to launch a container on some appropriate object in the XNAT model: project, subject, experiment, etc.

This document outlines this multi-stage flow.

## Caveat
The assumption behind this set of APIs is that the UI will make one request for one XNAT object. When the UI requests a list of commands that can be launched *here*, or requests a launch UI with an ID, there will be one single XNAT object that the *here* and the ID refer to. This implies two things, one on the UI side and one on the command/wrapper definition side:

1. As the user navigates around the XNAT UI, there will often be more than one object loaded on a page. For instance, on an image session page obviously the XNAT image session object is loaded. But also all of the session's scans, resources, and assessors are loaded. Since each request for commands to launch must have only a single object, this implies that many separate requests can be made on each page load.
2. The command wrapper's `external-inputs` are the entry points where XNAT objects can be inserted so their files and properties can be used to launch a container. The properties of those XNAT objects can be pulled out by the `derived-inputs`, into the underlying command's `inputs`, and ultimately provide the files and command line string needed to launch a container. However, while in general the `external-inputs` may contain an unbounded list of XNAT objects, only those command wrappers with a single external input will be launchable through the UI. The front end code can only request a launch UI for a single object, and the back end will interpret that as the wrapper's external input. If the wrapper has more than one external input, then there is no way for the back end to know which input corresponds to this object, or how to satisfy the other inputs.

# UI Step 1: What can I launch here?

This is the first step in the launch flow. As a user is clicking around XNAT, looking at various pages, in the background the UI will load information from the container service about the possible containers that could be launched on whatever the user sees at this moment. This API is fairly lightweight in terms of time and data—the backend should just retrieve some information and send it back, without "thinking" too much about the results—because the UI will presumably make a lot of requests to this endpoint that the user doesn't care about and which will just be wasted.

The purpose is to have a small baseline of information that the user can use to advance to the next stage of the flow if they choose to, but which has not wasted too much time or bandwidth if they aren't interested.

## Request
### REST Endpoint

The API is very simple. This is intended to be used by the UI pretty much everywhere, just swapping out the contextual query parameters.

    Request
        GET /xapi/commands/available

### Query Params
The UI will need to provide query parameters on that request defining the "`context`", i.e. what XNAT object the user is "looking at", or the XNAT object on which they might want to launch a container. On a given page there may be several objects for which the UI would need to make these requests. For instance, on an image session page, the UI may want to request "What can I launch on this image session?" but also request "What can I launch on this scan?" for each scan under the session.

Required params:

* *`xsiType`* - The XSI type of the object. E.g. `xnat:mrSessionData`,
    `xnat:projectData`, `xnat:ctScanData`.
* *`project`* - The ID of the project in which the request is being made.

I considered requiring a unique identifier for the particular object, but I do not think that should be necessary. Any object-specific processing would be too heavy for this particular API. The request can, I think, be as simple as "What can I launch on an 'xnat:fooBarData' in project 'XYZ'?"

## Backend processing
When this request is made, the container service backend will use the params to retrieve and filter a list of command wrappers. We have two levels of filtering, corresponding one-to-one with the two required query params:

* "What can be launched in this project?" - Includes checking...
    * whether each command is explicitly enabled/disabled on the site,
    * explicitly enabled/disabled on the project,
    * whether the project allows launching commands that aren't explicitly enabled,
    * whether the user has permissions to launch commands on this project
* "What can be launched on this object?" - Each command wrapper's `context` list contains XSI types on which the wrapper can be launched. We can check whether the XSI type from the request is in the list for each command wrapper. However, we must also check "child types"; if the request comes in with an XSI type `"xnat:mrSessionData"` and a command wrapper's `context` contains `"xnat:imageSessionData"`, that must be considered a match because `"xnat:mrSessionData"` **is** an `"xnat:imageSessionData"`.

## Response

An array of objects, each containing summary information about a single command + wrapper pair that can be used to launch a container. I.e., an array of the form:

    [
      {
          "command-id": 0,
          "command-name": "foo",
          "command-description": "A foo command",
          "wrapper-id": 0,
          "wrapper-name": "bar",
          "wrapper-description": "A foo that bars",
          "enabled": true,
          "image-name": "xnat/foo",
          "image-type": "docker",
          "root-element-name": "alpha"
      },
      {
          "command-id": 1,
          "command-name": "boo",
          "command-description": "A scary ghost command!",
          "wrapper-id": 12,
          "wrapper-name": "berry",
          "wrapper-description": "A breakfast cereal for children. See https://en.wikipedia.org/wiki/Monster_cereals.",
          "enabled": false,
          "image-name": "xnat/bar",
          "image-type": "singularity",
          "root-element-name": "omega"
      },
      ...
    ]

Most of the entries here are self-explanitory; the only one that bears explanation is `"root-element-name"`. The value of this field is the name of the wrapper's sole external input. (Recall that, if a wrapper has more than one external input, it will not be considered available to launch from the UI. [See note.](#caveat)) It is called the "root element" here because it can be used as the root of a tree of inputs, each deriving their value from some property of this input or one of its descendant inputs. Later, when a request is made to [get a launch UI](#create-a-launch-ui), the name used here will be used as a key, and will be given a value which is the ID of some XNAT object.

# UI Step 2: Create a Launch UI

Once a user clicks a button saying "Launch container `foo-bar`" and signals their intent to launch a container, the UI initiates the next step in the launch flow. At this stage it knows which particular command wrapper the user wants to launch, and which particular XNAT object they wish to use as an input. The UI will need to request more information from the container service, which it will use to render the launch UI.

## Request

### REST Endpoint

This is another simple `GET`. Most often, the user will be within some project context, and the UI will request the API:

    GET /xapi/projects/{project}/commands/{id}/wrappers/{name}/launch

Alternatively, if the wrapper ID is known, this API can be used:

    GET /xapi/projects/{project}/wrappers/{id}/launch

Those APIs will use any project-level configuration that exists. This allows each project to customize the default parameters of the command and wrapper to suit their data.

If, instead, one wishes to eschew the project-level configuration and use only the site-wide configuration, one can request one of

    GET /xapi/commands/{id}/wrappers/{name}/launch

or

    GET /xapi/wrappers/{id}/launch


### Query Params

The query parameters should contain a value for whichever wrapper parameter was listed as `"root-element-name"` on the [commands available](#what-can-i-launch-here) response.

The best practice for these values is to use REST-style identifiers. This means that, instead of using an object's ID as the parameter value (say `XNAT_E00001` for a session) you would use the REST path (`/experiments/XNAT_E00001`). While that isn't strictly necessary for objects like project, subjects, and sessions, it is necessary for scans and resources which do not have globally unique identifiers.

#### Examples

    Request
        GET /xapi/commands/available
    Params
        project: foo
        xsiType: xnat:imageSessionData

    Response
        [{..., "wrapper-id": 1, ..., "root-element-name": "a-parameter", ...}]

    Request
        GET /xapi/projects/foo/wrappers/1/launch
    Params
        a-parameter: /experiments/XNAT_E00001

    Request
        GET /xapi/commands/available
    Params
        project: foo
        xsiType: xnat:imageScanData

    Response
        [{..., "wrapper-id": 2, ..., "root-element-name": "input-scan", ...}]

    Request
        GET /xapi/projects/foo/wrappers/2/launch
    Params
        input-scan: /experiments/XNAT_E00001/scans/1

## Backend Processing

The backend will do what I call the "pre-launch". This involves going through all the command and command wrapper inputs, and using the provided XNAT object's properties to stick values into the inputs.

Assumptions:

* If any command wrapper inputs are `required`, and are not one of the "simple" types—string, boolean, number—and we can't derive some value for them given the context we have, then we can return an error.
* There can be inputs with multiple potential matches. This will not be considered a failure, as the user will have a chance to resolve the ambiguity in the launch UI.

Command resolution is a topic that deserves its own documentation, but that has not yet been written.

## Response

The object that gets returned has summary information about the container to be launched, and a flattened version of the resolved parent-child input value tree. (There will eventually be a full document about command resolution and the input value tree, but currently there is not.)

At the top level, the object is pretty simple. But there are several nested object types that can appear; they will each get their own section below.

    {
        "command-id": 0,
        "command-name": "",
        "command-description": "",
        "wrapper-id": 0,
        "wrapper-name": "",
        "wrapper-description": "",
        "image-name": "",
        "image-type": "",  // Currently only "docker"
        "inputs": {
            "an input name": LaunchUiInput,
            "a different input name": LaunchUiInput
        }
    }

`LaunchUiInput` contains information about which input is this one's parent (if any) and which are this one's children (if any).

    {
        "label": "",
        "description": "",
        "advanced": "",
        "required": "",
        "parent": "",  // The name of this input's parent input
        "children": [""],  // The names of this input's children
        "ui": {
            "a parent value": LaunchUiInputValuesAndType,
            "another parent value": LaunchUiInputValuesAndType
        }

    }

`LaunchUiInputValuesAndType`:

    {
        "values": ["a value", "another value"]
        "type": one of "default", "text", "number", "boolean", "select", "hidden", or "static"
    }

The reason this object is complex is that the relationship between values get a little hairy. For a given input it is not sufficient to list all the possible input values it can take. We have to list those values that are allowed _given that the input's parent takes a particular value_.

Let's go through an example. Consider an input `A` which is a `Session`, and a child input `B` which is a `Scan`. Let's say `A` can take on two different values `/experiments/XNAT_E00001` and `/experiments/XNAT_E00002`. Now what about `B`'s values? In general, `B` might take on different possible values which depend on the value of `A`. So if `A`'s value is `/experiments/XNAT_E00001` then `B` might be `/experiments/XNAT_E00001/scans/1` or `/experiments/XNAT_E00001/scans/2`; if `A`'s value is `/experiments/XNAT_E00002` then `B` can only take the values `/experiments/XNAT_E00002/scans/1` or `/experiments/XNAT_E00002/scans/2`.

We need the list of `LaunchUiInput` objects to express this complex relationship between parent value and child values. We don't want the UI to allow the user to select value `/experiments/XNAT_E00001` for input `A` and `/experiments/XNAT_E00002/scans/1` for input `B`. Those values refer to unrelated objects and aren't valid together. When the user selects a value for `A`, they should see only those values for `B` that are valid given the previous selection.

## UI

The UI parses the object and displays it to the user.

# UI Step 3: Launch a Container

At this stage, the user has filled out a form with all the command's parameters and reviewed them. When the user submits the launch, the UI will POST that form. The backend will use it to launch a container.

## Request

The request to launch the container is identical to what was detailed above. Except that the user doesn't have to memorize all the IDs and parameters, because all that information was given to them step-by-step by the APIs.

    Request (one of...)
        POST /xapi/commands/{id}/wrappers/{id}/launch
        POST /xapi/wrappers/{id}/launch
        POST /xapi/projects/{project}/commands/{id}/wrappers/{id}/launch
        POST /xapi/projects/{project}/wrappers/{id}/launch
    Headers
        Content-type: application/json
    Body
        {"paramName": "paramValue", ...}

This looks very similar to the API to get the launch UI. The difference is that this API uses a `POST` rather than a `GET`. As such, the params can be formatted in JSON in the body of the request, rather than in the query params. (Though, full disclosure, if you don't want to use JSON you can put the param names and values into the query params on this `POST` request instead of in the body. That would make it even more similar to the `GET`.)

## Backend Processing

At this point we have already done the initial "pre-resolution" part of command resolution process: deriving the wrapper inputs from the XNAT objects. Now we can accept the user's final input values and continue with the rest of the resolution: using the submitted values to satisfy the command's inputs, and fill out all the required launch items (command-line string, mounts, mounted files, environment variables, etc.).

## Response

The launch report contains a summary of what was launched and whether the launch was successful. If it was successful, the report contains the ID of the docker container that was launched; if not, it contains the error message that was received from docker or generated inside the container service during the resolution and launch process.

    {
        "command-id": 0,
        "wrapper-id": 0,
        "wrapper-name": "",  // Note that this may be blank if you requested the launch using only the wrapper ID
        "params": {
            "some-param-name": "some-param-value"
        },
        "status": "success" or "failure",
        "container-id": "a long hash",  // if "status" is "success"
        "message": "an error message"  // if "status" is "failure"
    }