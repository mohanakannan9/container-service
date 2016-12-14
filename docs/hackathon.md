

I have little bits of documentation scattered about right now. Part of what I'm trying to do before Friday is gather those into a form that humans can read to gain understanding. It may help you if you know what Docker is and what it can be used for, so consult https://www.docker.com for general info, to the docs at https://docs.docker.com, and maybe their Get Started guide https://docs.docker.com/engine/getstarted/. You don't necessarily have to go through the steps there, but reading them can give you some sense of what this is all about.
At the Hackathon we'll be coming up with project ideas, making docker images that turn the idea into an executable, then writing Commands to tell XNAT how to run the docker images. The Command is a data object that I made up, and as such it is less documented than anything having to do with docker. But you can see what I do have here: https://wiki.xnat.org/display/CS/Command.
I can give you a high-level overview and some reading material. The pieces of this puzzle that all fit together in various ways are:
* XNAT 1.7 running the Container Service plugin – We'll provide this to you. Pretty self-explanatory.
* A server running Docker – Docker is an executable that you can use to build and run containers. See https://www.docker.com for general info; I'll refer heavily to the docs at https://docs.docker.com. This will just be running on the same server as XNAT, so consider it "provided" for purposes of our Hackathon. You will likely have a better time understanding what I say on Friday if you’re already somewhat familiar with Docker, what it can do, maybe even how to use it. Because a big part of what we're planning to do on Friday is for everyone to build their own...
* Docker image – This is the thing that runs whatever you want to run. A good analogy is: docker is to a docker image as bash is to a bash script.

* [Useful links](#useful-links)

# Useful links

* Hackathon VM: [https://containers-demo.xnat.org](https://containers-demo.xnat.org)
* Private key file, to SSH in to VM as user TK: [ftp://ftp.nrg.wustl.edu/pub/data/containers/hackathon/keys/hackathon](ftp://ftp.nrg.wustl.edu/pub/data/containers/hackathon/keys/hackathon)

