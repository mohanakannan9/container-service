<!-- id: 35029426 -->
A bunch of stuff that you may want to do with the container service, some of which is actually possible.

Key:

* BE = Back End
* UI = User Interface
* ✅ = Done
* ❌ = Not done
* 🔜 = Somewhat done or in progress
* ❔ = Not sure

# Docker
## Docker server
I want to configure XNAT to communicate with docker...

* on the same machine as XNAT
    * BE ✅
    * UI ✅
* on a machine remote from XNAT
    * BE ✅
    * UI ✅
* on multiple different servers
    * BE ❌ [CS-77](https://issues.xnat.org/browse/CS-77)
    * UI ❌
* swarm
    * BE ❌ [CS-1](https://issues.xnat.org/browse/CS-1)
    * UI ❌
* via Kubernetes
    * BE ❌
    * UI ❌
* via Apache Mesos
    * BE ❌
    * UI ❌

## Docker hub
I want XNAT to...

* allow the admin to manage which docker hubs are known
    * BE ✅
    * UI ✅
* pull images from the public docker hub by default
    * BE ✅
    * UI ✅
* pull images from my custom / private docker hub
    * BE ✅
    * UI ✅

## Docker images
I want to use XNAT to add a docker image...

* `docker pull`ed from Docker Hub
    * BE ✅
    * UI ✅
* `docker pull`ed from a different docker repository
    * BE ✅
    * UI ✅
* from a tgz file URL
    * BE ❌ [CS-4](https://issues.xnat.org/browse/CS-4)
    * UI ❌
* from a file path to a tgz file on my docker server
    * BE ❌ [CS-4](https://issues.xnat.org/browse/CS-4)
    * UI ❌
* from a file path to a tgz file on my XNAT server
    * BE ❌ [CS-4](https://issues.xnat.org/browse/CS-4)
    * UI ❌
* by uploading a tgz file on my local machine
    * BE ❌ [CS-4](https://issues.xnat.org/browse/CS-4)
    * UI ❌

# Commands
Note: I am using "command" as a shorthand for the command + any wrappers that define how to launch from XNAT.

## Adding commands
I have a command I want to add to XNAT...

* and its definition JSON lives in my image's labels
    * BE ✅
    * UI ✅ (auto-saved when XNAT pulls the image, and can be manually added later)
* and its definition JSON lives in a file inside my image
    * BE ❌ [CS-11](https://issues.xnat.org/browse/CS-11)
    * UI 🔜 (not clear if there would be a UI for this)
* and its definition JSON lives at a URL
    * BE ❌ [CS-6](https://issues.xnat.org/browse/CS-6)
    * UI ❌
* by submitting the definition in JSON
    * BE ✅
    * UI ✅
* in [Boutiques syntax](https://github.com/boutiques/boutiques)
    * BE ❌ [CS-3](https://issues.xnat.org/browse/CS-3)
    * UI ❌
* in [Common Workflow Language syntax](https://github.com/common-workflow-language/common-workflow-language)
    * BE ❌ [CS-9](https://issues.xnat.org/browse/CS-9)
    * UI ❌

## Writing/defining commands
I have a docker image, and I want to define a command for it...

* with pretty much no help at all other than the code (✅)
* by asking [one of the container-service developers](https://github.com/johnflavin) for help (✅)
* by reading some documentation of the format (🔜 We do have some documentation [on the XNAT wiki](https://wiki.xnat.org/display/CS/Command), source maintained [in the repo](https://github.com/NrgXnat/container-service/blob/master/docs/command.md). But some details are missing or sparse.)
* by following a written tutorial or guide (❌)
* by interactively defining it inside a UI (❌)

After I write a command, but before it is ready, I want to...

* validate it has correct syntax (❌)
* run it in XNAT on test data (❌ [CS-55](https://issues.xnat.org/browse/CS-55))

## Other command management
I want to...

* delete a command
    * BE ✅
    * UI ✅
* update a few fields of an existing command
    * BE ✅
    * UI ✅
* update a docker image, but keep the command the same (a subset of 👆🏼)
    * BE ✅
    * UI ✅
* configure a few fields of the command at the site level
    * BE ✅
    * UI ✅
* add a command to a project, i.e. configure a few fields of the command at the project level
    * BE ✅
    * UI ✅

## Stuff you can do with a command
I want to...

* launch a container (see [Containers: Launching](#launching))
* use properties of XNAT objects (subject/session labels, scan ids, etc.) as inputs (🔜 You can do this, but the implementation is limited right now to a few properties. It can be fleshed out more as is. But full support for every property in every data type may require refactoring in container service or XNAT or both.)
* use files from XNAT as inputs (✅)
* upload files back to XNAT as a new...
    * resource (✅)
    * assessor (❌ [XNAT-4556](https://issues.xnat.org/browse/XNAT-4556) and  [CS-29](https://issues.xnat.org/browse/CS-29))
    * scan (❌ [CS-8](https://issues.xnat.org/browse/CS-8), may be solved by the XNAT ticket 👆🏼)
    * type of thing altogether, that just aggregates results of all the stuff I ran (❌)

# Containers

## Launching
I want to launch a container...

* when archiving a session (✅ Done, but very limited.)
* when "archiving" a scan (✅ Done, but very limited.)
* using other XNAT events (❌ waiting on the revamped XNAT event service)
* On a single XNAT object
    * BE ✅ and 🔜 A direct REST API exists to do this, but the back end needs more work to support the UI. [CS-180](https://issues.xnat.org/browse/CS-180)
    * UI 🔜 [CS-170](https://issues.xnat.org/browse/CS-170)
* in bulk - N objects -> N containers
    * BE ✅
    * UI 🔜 This is possible from the scan table. There are other places in the UI where the scan table approach will work, but some places (like search results) will require a different UI approach and/or a lot more XNAT work.
* in bulk - 1 object -> N containers, i.e. launch on all scans from a session
    * BE ❌
    * UI ❌ But this task can be accomplished as an N->N launch.
* in bulk - N objects -> 1 container, i.e. group analysis
    * BE ❌
    * UI ❌ But this task can (probably) be accomplished using the same UI as an N->N launch.
* "synchronously", where the user requests a launch and keeps the connection open while the container runs, and that same connection delivers the results back
    * BE ❌ [CS-51](https://issues.xnat.org/browse/CS-51)
    * UI ❌

## Monitoring / reporting
I want to see...

* what containers are running right now, on anything
    * BE ❌
    * UI ❌
* what containers are running right now, on a specific XNAT object, i.e. "workflow status alert"
    * BE ❌
    * UI ❌
* what containers have run in the past, on anything
    * BE ✅
    * UI ✅
* what containers have run in the past, on a specific thing
    * BE ❔
    * UI ❌
* the status of a particular container execution
    * BE 🔜 This is possible through a REST API but could be better.
    * UI ❌
* what files/resources/assessors/etc. were generated by the specific container execution I am looking at
    * BE 🔜 This information is stored, but there is no easy API to get it
    * UI ❌
* what container execution produced the specific file/resource/assessor/etc. I am looking at
    * BE ❌
    * UI ❌ Would probably require XNAT changes
* the logs from a completed container execution
    * BE ❌
    * UI ❌
* notifications on container status changes (success/failure/other)
    * BE ❌
    * UI ❌

I want to...

* stop a running container
    * BE ❌ [CS-13](https://issues.xnat.org/browse/CS-13)
    * UI ❌
* restart a failed/finished container, either as-is or with new input values
    * BE ❌ [CS-13](https://issues.xnat.org/browse/CS-13)
    * UI ❌

# Users / permissions / security
Some of the container service functions need to be locked down to only certain XNAT users.

* Functions that are available to admins
    * Manage docker server(s)
    * Manage docker hub(s)
    * Manage docker images (pull onto server, delete from server)
    * Manage commands (create, update, delete, configure site-wide defaults, enable/disable on site)
    * View all launched containers
* Same as 👆🏼 but for a new "container admin" user role
* Functions that are available to project owners
    * Manage commands (configure project-level defaults, enable/disable on project)
    * View containers launched from a project
* Functions that are available to all users
    * Launch commands within a project
    * GET site-wide command definitions
    * GET configured project commands

Currently this whole section is either ❌ or ❔. Almost all API functions are fully open. Ticket: [CS-34](https://issues.xnat.org/browse/CS-34).

# Other stuff
Big-ticket items that are currently poorly defined. If any of these become priorities, they will have to be defined more sharly and will spawn a huge number of additional tickets and roadmap items.

* I want to define a workflow to chain together my containers / commands / wrappers.
    * BE ❌
    * UI ❌
* I want to configure XNAT to communicate with Singularity.
    * BE ❌ [CS-76](https://issues.xnat.org/browse/CS-76)
    * UI ❌
