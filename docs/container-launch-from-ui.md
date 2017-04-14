
When a user wants to use the container service to launch a container from a command, they have a REST API endpoint to do so:

    POST /xapi/commands/{id}/wrappers/{id or name}/launch
    Header: Content-type: application/json
    Body: {"paramName": "paramValue", ...}

This endpoint is the best way to go directly from zero to launching a container. But it isn't very usable by anyone because it requires a lot of foreknowledge. The user must know the IDs of the command and command wrapper, all the parameters that must be supplied, and all the appropriate values to give those parameters.

Clearly, we need a simpler way to launch containers. Users should be expected to know as little as possible about what they want to launch, and the UI can help them through the process as much as possible. There should be a multi-stage process a user can step through to launch a container on some appropriate object in the XNAT model: project, subject, experiment, etc.

This document outlines this multi-stage flow.

## Caveat
The assumption behind this set of APIs is that the UI will make one request for one XNAT object. When the UI requests a list of commands that can be launched *here*, or requests a launch UI with an ID, there will be one single XNAT object that the *here* and the ID refer to. This implies two things, one on the UI side and one on the command/wrapper definition side:

1. As the user navigates around the XNAT UI, there will often be more than one object loaded on a page. For instance, on an image session page obviously the XNAT image session object is loaded. But also all of the session's scans, resources, and assessors are loaded. Since each request for commands to launch must have only a single object, this implies that many separate requests can be made on each page load.
2. The command wrapper's `external-inputs` are the entry points where XNAT objects can be inserted. The properties of those objects are pulled out by the `derived-inputs`, into the underlying command's `inputs`, and ultimately provide the files and command line string needed to launch a container. However, while the `external-inputs` may contain an unbounded list of XNAT objects, only those command wrappers with a single external input will be launchable through the UI. The front end code can only request a launch UI for a single object, and the back end will interpret that as the wrapper's external input. If the wrapper has more than one external input, then there is no way for the back end to know which input corresponds to this object, or how to satisfy the other inputs.


# What can I launch here?

This is the first step in the launch flow. As a user is clicking around XNAT, looking at various pages, in the background the UI could be loading information from the container service about the possible containers that could be launched on whatever the user sees at this moment. This API should be fairly lightweight in terms of time and data— the backend should just retrieve some information and send it back, without "thinking" too much about the results—because the UI will presumably make a lot of requests to this endpoint that the user doesn't care about and which will just be wasted.

The purpose is to have a small baseline of information that the user can use to advance to the next stage of the flow if they choose to, but which has not wasted too much time or bandwidth if they aren't interested.

## Request
### REST Endpoint

The API is very simple. This is intended to be used by the UI pretty much everywhere, just swapping out the contextual query parameters.

    GET /xapi/commands/available

### Query Params
The UI will need to provide query parameters on that request defining the "`context`", i.e. what XNAT object the user is "looking at", or the XNAT object on which they might want to launch a container. On a given page there may be several objects for which the UI would need to make these requests. For instance, on an image session page, the UI may want to request "What can I launch on this image session?" but also request "What can I launch on this scan?" for each scan under the session.

Some query params will be required on each request, and others could optionally be provided under particular circumstances. I haven't nailed down exactly what parameters are in either the required or optional set, but here is my current thinking:

Required params:

* *`xsiType`* - The XSI type of the object. E.g. `xnat:mrSessionData`,
    `xnat:projectData`, `xnat:ctScanData`.
* *`project`* - The ID of the project in which the request is being made.

Optional params

* ... Can't think of any right now.

I considered requiring a unique identifier for the particular object, but I do not think that should be necessary. Any object-specific processing would be too heavy for this particular API. The request can, I think, be as simple as "What can I launch on an 'xnat:fooBarData' in project 'XYZ'?"

## Backend processing
When this request is made, the container service backend will use the params to retrieve and filter a list of command wrappers. We have two levels of filtering, corresponding one-to-one with the two required query params:

* "What can be launched in this project?" - Includes checking...
    * whether each command is explicitly enabled/disabled on the site,
    * explicitly enabled/disabled on the project,
    * whether the project allows launching commands that aren't explicitly enabled,
    * whether the user has permissions to launch commands on this project
* "What can be launched on this object?" - Each command wrapper's `wrapperContext` list contains XSI types on which the wrapper can be launched. We can check whether the XSI type from the request is in the list for each command wrapper. However, we must also check "child types"; if the request comes in with an XSI type `"xnat:mrSessionData"` and a command wrapper's `wrapperContext` contains `"xnat:imageSessionData"`, that must be considered a match because `"xnat:mrSessionData"` **is** an `"xnat:imageSessionData"`.

These two filters are independent, and could be applied in either order. I'm not sure which should be applied first. Both are well-suited to being cached; we could cache high-level API functions that loop over all commands—"what can launch on project `"XYZ"`?", "what can launch on an `"xnat:fooBarData"`?"—or lower-level API functions that operate on a single command wrapper—"can wrapper `"Bar"` be launched on project `"XYZ"`?", "can wrapper `"Bar"` be launched on an `"xnat:foBarData"`?" I guess I will experiment and see if either ordering is demonstrably slower or faster.

## Response

An array of objects, each containing summary information about a single command wrapper that can be used to launch a container. I.e., an array of the form:

    [
      {
          "command-id": 0,
          "command-name": "foo",
          "wrapper-id": 0,
          "wrapper-name": "bar",
          "enabled": true,
          "image-name": "xnat/foo",
          "image-type": "docker",
          "external-input-name": "alpha"
      },
      {
          "command-id": 1,
          "command-name": "boo",
          "wrapper-id": 12,
          "wrapper-name": "berry",
          "enabled": false,
          "image-name": "xnat/bar",
          "image-type": "singularity",
          "external-input-name": "omega"
      },
      ...
    ]

# Create a Launch UI

Once a user clicks some button or submits some form or whatever saying "Yes, I would like to launch command wrapper `foo-sandwich`", the UI initiates the next step in the launch flow. At this stage it knows a particular command wrapper the user wants to launch on a particular XNAT object. It will need to request more information from the container service, which it will use to render the launch UI.

## Request

### REST Endpoint

This is another simple `GET`.

    GET /xapi/commands/{id}/wrappers/{id or name}/launch

### Query Params

Required params:

* *`id`* - The unique identifier of the XNAT object on which the container will be launched

## Backend Processing

The backend will do what I call the "pre-launch". This involves going through all the command and command wrapper inputs, and using the provided XNAT objects' properties to stick values into the inputs.

Assumptions:

* If any command wrapper inputs are `required`, and are not one of the "simple" types—string, boolean, number—and we can't derive some value for them given the context we have, then we can return an error.
* There can be inputs with multiple potential matches. This will not be considered a failure, as the user will have a chance to resolve the ambiguity in the launch UI.

TODO: Expand this section.

## Response

The response will contain information that the UI can parse and can use to drive the creation of a container launch UI. This object is very vague in my mind right now.

* It will probably contain all the same information in the initial "What commands can I launch here?" response.
* It will also contain information about each parameter that will be used to launch the container. This can include things like:
    * Name
    * Description
    * Value - pre-filled with either a default or a context-specific XNAT object ID
    * Type - I'm not sure about this. On the backend I track types like string, boolean, number, but also XNAT object types like project, subject, session, scan, etc. I don't know if the UI will find those types useful or not.

I have a lot of unanswered questions about this object. Let's use an example command wrapper that has one external input—a session—and two derived inputs—a scan under the session and a resource under the scan.

* There will be a need to signal to the UI "there are multiple potential matches for this parameter, and the user must select one". Using the example, this would be if multiple scans on the session match whatever input matcher was defined on the command wrapper. If multiple scans could possibly be used but only one is allowed, the user will need to pick one.
* There will be a need to signal that input choices are connected. Selecting a particular option for one input may constrain the options for another input, even to the point of determining the other input down to one unique option. Going back to the example, once the user has selected a scan, then the resource input should only show the resources under that particular scan. The user should not be able to select a resource from any scan other than the one they just selected. And what's more, it may well be that given the choice of a particular scan, only one resource matches the input matcher. That input would be fully determined, the one remaining resource option should be automatically selected, and the user doesn't need to choose any resource at all.

TODO: Detail the structure of the object.

## UI

The front end will need to parse through the response and use it to generate a UI that can launch a container.

TODO: A lot.

# Launch a Container

At this stage, the user has filled out a form with all the command's parameters and reviewed them. When the user submits the launch, the UI will POST that form. The backend will use it to launch a container.

## Request

Something like

    POST /xapi/commands/{id}/wrappers/{id or name}/launch
    Header: Content-type: application/json
    Body: {"paramName": "paramValue", ...}

This looks very similar, if not identical, to the launching API that we already have. That would not be optimal, because we would have to re-do all the effort that we did in the "pre-launch" phase. So how is this different? Something to think about.

## Backend Processing

At this point we have already done the first part of the as-currently-coded command resolution process: deriving the wrapper inputs from the XNAT objects. That is the "pre-resolution", and it was done in the last section when deriving the launch UI. Now we can accept the user's input and continue with the rest of the resolution: using the submitted values to satisfy the command's inputs, and fill out all the required launch items (command-line string, mounts, mounted files, environment variables, etc.).

The existing code to do this can, I think, stay mostly intact.

## Response

The current REST API for launching a container just returns the container ID. I will continue doing that for now. But I would be open to changing that and returning whatever information the UI wants to display.