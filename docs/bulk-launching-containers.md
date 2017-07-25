<!-- id: 35029445 -->

# Summary

The purpose of this document is to discuss the following:

* What is meant by the phrase "bulk launching" of containers by the container service?
* What types of launching does the container service support or not support right now?
* For those types of launching not yet supported, how could they be supported? What questions are left open that would need to be answered before work on supporting them could begin?

# Background

## What is "bulk launching"
There are multiple ways to take one or more XNAT inputs and launch one or more containers, and multiple of those ways could be rightly called "bulk launching". At the input side, the user could send the contaner service N input sets; at the launching side, the container service could resolve a command with inputs and launch N containers; or both. We can make a 2x2 matrix of number of inputs vs number of launched containers.

|              | 1 launch | N launches |
|:-------------|:---------|:-----------|
| 1 input set  | 1 -> 1   | 1 -> N     |
| N input sets | N -> 1   | N -> N     |

Three of the four of these could be called "bulk launches"; only 1 -> 1 is excluded.

## A sketch of the flow from input to container
The flow of information inside the container service from launch request to launch is, very abstractly, this:

1. A launch request comes in through the API. It sepcifies the command+wrapper definition, and has some input parameters.
2. The command+wrapper are "resolved" using the input parameters. During the resolution process, the input parameters are used to instantiate XNAT objects whose properties and files are used as defined in the wrapper. The end result of command resolution is a "resolved command". Right now the number of resolved commands produced for each launch request is exactly one.
3. The resolved command is used to send a request to launch a container.

# Inputs -> Containers

Below, we will discuss all the (number of inputs) -> (number of container) pairs in the matrix above.

## 1 -> 1
One input is submitted, and the container service launches one container.

This is the simplest case and easiest to understand. The user requests a launch and gives some parameters, and the container service obliges.

This API has existed as a REST API for a long time. Work is currently ongoing to make a fully functional user interface for this launch mode.

## N -> N
`N` inputs are submitted, and the container service launches `N` containers.

This is a generalization of the 1 -> 1 case. Once we have a succifient 1 -> 1 API, we need only write a small additional API that takes in `N` inputs and calls the 1 -> 1 API for each of them.

This API exists as of recently in the form of a new REST API. Generalizing the API to a UI will (hopefully) not be much more complicated than the UI of the 1 -> 1 case.

## 1 -> N
One input is submitted, and the container service launches `N` containers.

The use case I can think of for this mode is looping over decendant XNAT objects. For example, I request a launch and give as input the ID of a session, with the expected result that one container will be launched on each of the session's `N` scans.

There are two ways this could be supported by the container service: in the UI, or in the back end.

### 1 -> N in the UI

In the user interface, this could essentially be faked. On the page for the "parent" object, say a session report page, an action could be included that actually submits a launch request for the `N` descendant objects, say the session's scans. In this way we could include some 1 -> N launching by using the existing N -> N API.

The drawback to this approach is that to have a general solution, the UI would have to allow users to launch containers for every combination of ancestor/descendant objects (session/scan, session/resource, scan/resource, subject/session, subject/scan, project/subject, project/session, project/scan, etc.). This would make the list of potential actions for projects and subjects very large, and the time spent making and processing those requests could increase page load time significantly.

Of course, we could also get away with a less-than-general solution by only coding the few cases that are most likely to be needed, perhaps adding more over time. For instance, launching on all scans in a session is likely to be used very often. (But, then again, the new scan table that allows launching containers on a selection of scans already captures the functionality of launching multiple scan containers from the session page. So maybe having a session-level action that launches on a subset of scans isn't necessary, since we can use the N -> N API to launch on that subset of scans directly.)

### 1 -> N in the back end

Supporting back-end 1 -> N launching would require changes to how commands are resolved, and probably in how the command wrappers are defined. The API cannot be made to handle this case, because at the level of the API the input parameters are just strings. It is only at the level of command resolution where the structure and relationships between the XNAT objects can be used.

I don't have a very clearly-formed picture of how this will look, so please allow me to ramble here for a bit. Hopefully over time these thoughts become sharper and the document becomes clearer to reflect that.

Is the wrapper changed so that I can write a wrapper that launches another wrapper? (Wrapper A takes a scan input, and feeds the command's inputs. Wrapper B takes a session input, and feeds its scans to wrapper A.) That means that only those few cases that are explicity defined would work. Not just any wrapper would be launchable on all scans in a session, only those with the wrapper that says they can be. That seems suboptimal.

Or is that structure allowed to be implicit, and taken care of during command resolution? (Wrapper A requires a scan input, but the user submits a session ID. Resolver tries to instantiate a scan from the ID and fails. Does it then try to instantiate a session, and get all of its scans? If that fails, does it try a subject? A project?) Instead, do we require the inputs to be in a special form for this to work? (`scan = "/experiments/E1/scans/S1"` will launch on a single scan, and `scan = "/experiments/E1/scans/*"` will launch on them all. How deep does that go? What about `scan = "/projects/P1/subjects/*/experiments/*/scans/*"`? The command resolution would have to parse out inputs that it suspects are REST-style URIs, breaking out those sections that are defined with IDs and those that are left with `*`s.) Do we allow matchers and searching embedded in this URI? `scan = "/projects/P1/subjects/*/experiments[?(@.xsiType="xnat:mrSessionData")]/*/scans[?(@.seriesDescription="MPRAGE")]/*"`? It seems like I am combining XPath and JSONPath syntax here, and that certainly isn't going to work.

Regardless of how we interpret the inputs, command resolution will have to be changed so that the final product is not a single resolved command, but a list of them. Does this mean that every 1 -> 1 launch is potentially a 1 -> N? Will accidental launches happen? How often? Would this introduce another phase to the launch UI flow, where the back end has to fully resolve everything, and the UI has to present a dialog "Are you sure you want to launch dicom to nifti on the entire project?"?

## N -> 1
`N` inputs are submitted, and the container service launches one container. This can represent a group analysis process. The user will select N items from XNAT, and launch some command wrapper than it written to accept those N items into an input and use them all together to feed the inputs of one command and launch from it one container.

Resolving a command of this form will require some changes to the resolution process, and will require new information to be written into the wrapper, but those changes should not be that substantial. As I see it right now, all that we would require to change in the wrapper is some indication than an input is "listable" (maybe just a boolean property with that name). I don't think that command resolution itself will have to change very much; with this N -> 1 use case in mind I recently refactored command resolution to add a "pre-resolution" part of the resolution process, in which the code allows all the inputs to take on multiple values and generates a tree of parent-child input value relationships.

The more substantial changes, changes of which I do not currently have a clear mental picture, are to the Resolved Command object and the code for launching a container. I don't know how I will send these unbounded numbers of mounts or command-line arguments to docker. I have a bunch of questions about how I can make this work. It would be nice to have some example applications that I could use for reference when trying to generalize this process, i.e. some containers that would use this N -> 1 launching mode. But for right now I do not have that, so here are some questions fro mthe top of my head:

* When I have N sets of input files (say, all the scans in a project) will I create N separate mounts into the container? (Does Docker have a limit on the number of mounts a single container can have?)
* Or will I copy all N sets of files into a single directory, which I can then send to the container as one mount? That could potentially mean each launch of a container like this would have to copy gigs and gigs of data around before launching. Less than ideal.
* Can I use the values of "listable" inputs in the various template strings, like the command line? What happens if input A is listable, and in the command line I need to use the value of A? Is that an error? Or do I just concatenate all N values of input A?
