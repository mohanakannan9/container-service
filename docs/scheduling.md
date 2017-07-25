<!-- id: 36372493 -->

This document will contain a discussion on container job scheduling as it pertains to the XNAT container service.

Scheduling is a necessary part of executing container jobs on a cluster or in a high-performance computing enviornment. The container service can currently communicate with a single docker server, and all containers run on the same machine as docker. In this scenario, the container service is acting as a scheduler, scheduling all jobs to a single worker node. When the number or size of the jobs grows, a single worker node will no longer provide adequate resources to service all the jobs simultaneously. Additional worker nodes can be added to a cluster and jobs assigned to nodes as appropriate, or the jobs can be held in a queue until resources are available, or both. These solutions are part of the job of a scheduler.

> **scheduling** is the method by which work specified by some means is assigned to resources that complete the work. - [Wikipedia: Scheduling (Computing)](https://en.wikipedia.org/wiki/Scheduling_(computing))

The requirements of the scheduler will quickly grow beyond what the container service can do. We need to look at integrating with some scheduling system.

# What have we done before?

## Pipeline engine

The pipeline engine delegates job scheduling to a customizable script. When XNAT prepares to launch a pipeline execution, it builds the command-line string to be executed and passes that to a script called `schedule`. This script, as it exists when distributed with the pipeline engine, does nothing but executes whatever arguments it was given; it is a simple pass-through. But the `schedule` script can be overridden by particular XNAT installations, which allows customizable job scheduling. On the CNDA and other NRG-managed XNAT installations, `schedule` passes its arguments (which, remember, are the command-line string of a pipeline to be executed) to another tool called `PipelineJobSubmitter`. The latter tool was written against the Distributed Resource Management Application API (DRMAA), which allows it to communicate with the NRG's Sun Grid Engine (and later Open Grid Engine) for job scheduling.

* Advantages
    * Customizable behavior per installation, through overridable script
    * Ships with an implementation that targets a generic scheduling API
* Disadvantages
    * Configuration is difficult.
    * Each pipeline is an atomic unit of work, rather than each pipeline step. This caused several problems.
        * If a pipeline gave resource requirements, it had to ask for the maximum amount of resources that any of its steps might use, even if the other steps used far less. This could cause inefficient use of node resources.
        * Some pipeline steps used very little resources but needed network access. Other pipeline steps needed a lot of resources but no network. If the steps could be assigned to different nodes, some nodes would be able to specialize into weak-but-network-available and beefy-but-network-unavailable, for instance. Since all the steps had to be done on one node, all the nodes had to generalize.

## On-demand (Onde) processing
Kevin Archie designed and partially built the Onde service in 2013 to serve as a next-generation XNAT processing system. (Some links to old wiki docs: [Onde requirements](https://wiki.xnat.org/display/XNATDev/XNAT+Onde+requirements), [Onde design](https://wiki.xnat.org/display/XNATDev/XNAT+Onde+design), [Example "CIFTI Average" job request](https://wiki.xnat.org/display/XNATDev/Job+request+-+CIFTI+average))

At its core, Onde was designed as a job scheduler. It was made to manage compute nodes and job queues. the structure of the jobs themselves was not a primary concern.

It was never finished, so we can't really evaluate it. I just wanted to include some of the history of this project.

# What approaches could we take now?

Some constraints to keep in mind as we consider various schedulers:

* The container service is built around running jobs as containers. As such, we would do well to chose a scheduling system that is also built around running containers.
* We have already built a service that can run individual docker containers directly with a docker server. There will be some changes and additions necessary to support interacting with a job scheduler instead. But we should strive to choose to support a scheduler that can minimize the cost/benefit ratio of any changes we would have to make to the existing container service. Minimal code/API changes are good. Minimal changes to the command spec are good. But we can accommodate larger changes if the scheduler gives some additional benefit in exchange.
* As much as possible, we should consider our users. If some users are considering using the container service and are already using containers in an HPC environment or on a cluster, we should at least learn from them and possibly try to support their workflow. And if other users already have the container service and are considering scaling up from a single docker instance to some larger infrastructure, we should not put pressure on them through our actions to set up a very difficult scheduling system (*cough* SGE *cough*).

## Docker Swarm

SUMMARY: Docker swarm would work ok for our purposes, but its strongest features are things we will never use.

Swarm is more of an "orchestration" tool rather than a "scheduling" tool. Its core purpose is to maintain replicas of "services" across all your nodes. Services are, roughly, a spec for how to run a container with some resource requirements and a desired state, like how many replicas of the service you want to be up at a given time. The job of the swarm is to maintain the service in the desired state.

For instance, a service can specify how many replicas should exist, and the manager will make sure that number of replicas is maintained. If some worker goes down, or a task becomes unresponsive, the manager will start identical replica tasks on other worker nodes. Replication is a key selling point for swarm services, but it seems useless for our purposes. Every job we run is doing something unique, either because it was given unique command-line arguments or it has mounted different files.

We can use the swarm job-scheduling features, but that's it. Which is not to say that this is bad. Being able to submit a service spec to a manager node and have that service be sent out to a worker node is still very useful to us, even if we will never replicate any services.

* Things we would need:
    * Sys admin sets up swarm. We aren't going to expose the whole API for that.
    * To make sure the bind mounts will work...
        * Every node in the swarm will need to have the full archive mounted, or
        * The transporter is going to have to become functional. But even that is tricky. We would have to either know which node is going to receive the task in order to send the files to one place (and that means we have to predict the scheduler's behavior), or we transport the files to all nodes even if they aren't going to use them.
    * XNAT will have some checkbox for "swarm mode" which sets a pref value.
    * Some of the docker APIs we use will be slightly different, but not that much.
         * "service create" instead of "container create"
         * Monitoring API will be different.
         * What about events? Need to see if we can just check for container events on the manager, or if we will need to check for swarm events or something like that. I hope we don't need to gather events from every node in the swarm... 😕
    * We will need to add fields to the container objects (pojo and entity) to note which node it is running on, and maybe additional IDs if we need them.

## Kubernetes

My entire understanding comes from browsing their documentation and from this article: [Docker Swarm vs. Kubernetes: Comparison of the Two Giants in Container Orchestration](https://www.upcloud.com/blog/docker-swarm-vs-kubernetes/). I think Kubernetes will not work for us right now because...

* Kubernetes "Takes some work to get up and running"
* We would have to support a very different API.
  > Kubernetes uses its own client, API and YAML definitions which each differ from that of the standard Docker equivalents. In other words, you cannot use Docker CLI nor Docker Compose to define containers. When switch platforms, commands and YAML definitions will need to be rewritten.

Like Docker Swarm, Kubernetes is more of an orchestration tool that can also do job scheduling, and not a dedicated scheduler.