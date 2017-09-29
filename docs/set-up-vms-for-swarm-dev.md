<!-- id: 43483466 -->

* Make local directories
    * One to serve as the XNAT archive, which I will call "archive"
    * Another for the build directory, which I will call "build"
* Set up XNAT VM (use XNAT Vagrant project)
    * Start from xnat-release config
    * Edit local.yaml
    * Pointing to 1.7.3 war
    * Add shares
        * archive to `/data/xnat/archive`
        * build to `/data/xnat/build`
    * Run vagrant setup
    * If you are on the Ubuntu 14.04 VM, update docker. If you are using 16.04 or later, you can skip this.
        * Fix bad puppet PGP key
            * `sudo gpg --keyserver pgp.mit.edu --recv-key 7F438280EF8D349F`
            * `sudo gpg -a --export EF8D349F | sudo apt-key add -`
        * `sudo apt-get update`
        * `sudo apt-get upgrade`
        * Say "No" to everything
        * Find version with `docker version`. (Right now it is 17.05.0-ce)
    * If you are using the Ubuntu 16.04 VM, find the docker version
        * SSH onto machine
        * `docker version`
        * Note docker version for later.
    * set up swarm
        * `docker swarm init --advertise-addr ${YOUR_VM_IP}`
        * Note the swarm join command that gets spit out. We will use it later.
        * `docker node update --availability drain xnat`
* Set up Worker node(s)
    * Go to [boot2docker releases page](https://github.com/boot2docker/boot2docker/releases)
    * Find boot2docker release with the same version of docker as you noted earlier
    * `docker-machine create --boot2docker-url ${above url} worker${n}` (where n is an index, recommended if you are making more than one worker)
    * ssh to node just created
    * Use swarm join command that got spit out when we made the swarm
    * Share local XNAT archive (do this for each worker node)
        * In virtualbox, change VM settings. Add shares to archive and build (use the local paths on your machine)
        * ssh to VM
        * `sudo mkdir -p /data/xnat/archive`
        * `sudo mkdir /data/xnat/build`
        * `sudo chown -R docker /data`
        * Share archive to worker node: `sudo mount -t vboxsf -o uid=1000,gid=100 archive /data/xnat/archive`
        * Share build to worker node: `sudo mount -t vboxsf -o uid=1000,gid=100 build /data/xnat/build`