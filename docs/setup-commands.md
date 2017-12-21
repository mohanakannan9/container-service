<!-- id: 53674441 -->
## Background and Problem

There are several use cases where it would be nice to run a little bit of code before launching a container.

One such case is when running the BIDS Apps ([CS-237](https://issues.xnat.org/browse/CS-237)). The _raison d'être_ of the BIDS Apps is that they run on BIDS-formatted data. The user launches them by sending in a path, and it is implied that the data found at that path will be formatted in the BIDS structure. However, this is incompatible with how the container service typically works, which is by mounting data directly from the archive into a container and kicking off a process. When our data is in the archive it isn't stored in the BIDS structure, it is stored in the XNAT archive structure.

We do have a container, xnat/dcm2bids-session ([github](https://github.com/NrgXnat/docker-images/tree/master/dcm2bids-session)) ([docker hub](https://hub.docker.com/r/xnat/dcm2bids-session/)), that ostensibly converts DICOM data in XNAT to BIDS format. That is correct up to a point. It generates the NIFTI files and BIDS JSON metadata, but it keeps all that data in the XNAT archive format rather than storing it all at rest in BIDS format. Even after running this container on our data we still can't launch any BIDS Apps directly; because the data are stored in the XNAT archive format the BIDS Apps would not be able to read them. The dicom2bids container gets us most of the way to launching BIDS Apps. We need an extra push to get all the way.

## Solution: Setup Commands
The way we solve this problem is by introducing a new type of command called a "setup command". The job of a setup command will be to take data that is in the XNAT archive format and stage it / move it / convert it into whatever format is required for the "main" command. The author of a main command (for instance, a command that describes a BIDS App) who has a need for a setup command will have to first create a setup docker image, write a setup command to describe it, and reference that setup command in their main command. At launch time, the Container Service will launch a container from the setup command before starting the main container from the main command, thus giving the setup container a chance to stage the files in the right way for the main container.

## How a Setup Command Changes the Container Launch Flow

When the Container Service goes to launch a container from a command, if it sees that the command makes reference to a setup command, it will create and start a setup container before starting the main container. To see where and how that happens, let us examine the launch sequence with and without a setup command.

### Without Setup Command

First we will examine the Command Resolution and Container Launch sequence for a command that does not reference a setup command. Here is a (very abbreviated) version of finding the file paths for a mount.

1. The Command has a mount named `input`. The mount defines the path inside the container, which we will call `p`.
2. The Command Wrapper has an input which provides files for mount `input`.
3. That Command Wrapper input has a runtime value which we can resolve into some XNAT object. We find the path to that object's files in the archive, which we will call `A`.
4. We will mount the path `A` on the host to the path `p` in the container.

Once the rest of the Command Resolution process is complete, the container will be created and started with a mount which takes path `A` outside the container to path `p` inside the container.

### With Setup Command

Now we will examine the same process, only this time the Command Wrapper input references a Setup Command.

1. The Command has a mount named `input`. The mount defines the path inside the container, which we will call `p`.
2. The Command Wrapper has an input which provides files for mount `input`.
3. That Command Wrapper input has a runtime value which we can resolve into some XNAT object. We find the path to that object's files in the archive, which we will call `A`.
4. The Command Wrapper input has a value for its `"via-setup-command"` property. We must now resolve that Setup Command.
5. A Setup Command is created with two mounts, which have inside-the-container paths `/input` and `/output`, repsectively.
6. We will mount the path `A` on the host to the the path `/input` in the setup container.
7. We make a new writable directory `B`
8. We will mount the path `B` on the host to the the path `/output` in the setup container.
8. We will mount the path `B` on the host to the the path `p` in the main container.

Once the rest of the Command Resolution process is complete, we will create two containers:

1. The setup container. This will have two mounts:
    1. The archive path `A` to `/input`
    2. A writable directory `B` to `/output`
2. The main container, which has one mount: `B` to `p`.

In this way, whatever the setup container writes to its output mount is given to the main container through its input mount.

<ac:structured-macro ac:name="tip" ac:schema-version="1" ac:macro-id="9374bcc1-9eb9-457e-a5ee-8786bd50f97a"><ac:rich-text-body>
Though we _create_ the setup and main containers at the same time, we only _launch_ one: the setup container. Once the setup container is complete, the Container Service will launch the main container. If the setup container fails, the main container will not be launched, but will receive a status "Failed Setup".
</ac:rich-text-body></ac:structured-macro>

## Creating a Setup Docker Image

The purpose of the setup image (and the setup containers created from it) is to take files in the XNAT archive format and copy / transform / convert them to whatever format they need to be. To fulfill this purpose, setup containers will always be given mounts at the same places: `/input` and `/output`. Inside the setup image, you can run whatever scripts you need to accomplish your task.

As an example, the setup image `xnat/xnat2bids` runs a script that takes files with BIDS metadata, but stored in the XNAT archive format, and moves them into the BIDS format. To do this it runs a python script, `xnat2bids.py`. Here is this script (as of 2017-12-20; the [current version](https://github.com/NrgXnat/docker-images/blob/master/setup-commands/xnat2bids/xnat2bids.py) may be different).

    #!/usr/bin/env python

    """xnat2bids
    Turn files in XNAT archive format into BIDS format.

    Usage:
        xnat2bids.py <inputDir> <outputDir>
        xnat2bids.py (-h | --help)
        xnat2bids.py --version

    Options:
        -h --help           Show the usage
        --version           Show the version
        <inputDir>          Directory with XNAT-archive-formatted files.
                            There should be scan directories, each having a NIFTI resource with NIFTI files, and
                            BIDS resources with BIDS sidecar JSON files.
        <outputDir>         Directory in which BIDS formatted files should be written.
    """

    import os
    import sys
    import json
    import shutil
    from glob import glob
    from docopt import docopt

    bidsAnatModalities = ['T1w', 'T2w', 'T1rho', 'T1map', 'T2map', 'T2star', 'FLAIR', 'FLASH', 'PD', 'PDmap', 'PDT2', 'inplaneT1', 'inplaneT2', 'angio', 'defacemask', 'SWImagandphase']
    bidsFuncModalities = ['bold', 'physio', 'stim', 'sbref']
    bidsDwiModalities = ['dwi', 'dti']
    bidsBehavioralModalities = ['beh']
    bidsFieldmapModalities = ['phasemap', 'magnitude1']

    class BidsScan(object):
        def __init__(self, scanId, bidsNameMap, *args):
            self.scanId = scanId
            self.bidsNameMap = bidsNameMap
            self.subject = bidsNameMap.get('sub')
            self.modality = bidsNameMap.get('modality')
            self.subDir = 'anat' if self.modality in bidsAnatModalities else \
                          'func' if self.modality in bidsFuncModalities else \
                          'dwi' if self.modality in bidsDwiModalities else \
                          'beh' if self.modality in bidsBehavioralModalities else \
                          'fmap' if self.modality in bidsFieldmapModalities else \
                          None
            self.sourceFiles = list(args)

    class BidsSession(object):
        def __init__(self, sessionLabel, bidsScans=[]):
            self.sessionLabel = sessionLabel
            self.bidsScans = bidsScans

    class BidsSubject(object):
        def __init__(self, subjectLabel, bidsSession=None, bidsScans=[]):
            self.subjectLabel = subjectLabel
            if bidsSession:
                self.bidsSessions = [bidsSession]
                self.bidsScans = None
            if bidsScans:
                self.bidsScans = bidsScans
                self.bidsSessions = None

        def addBidsSession(self, bidsSession):
            if self.bidsScans:
                raise ValueError("Cannot add a BidsSession when the subject already has a list of BidsScans.")
            if not self.bidsSessions:
                self.bidsSessions = []
            self.bidsSessions.append(bidsSession)

        def hasSessions(self):
            return bool(self.bidsSessions is not None and self.bidsSessions is not [])

        def hasScans(self):
            return bool(self.bidsScans is not None and self.bidsScans is not [])

    def generateBidsNameMap(bidsFileName):

        # The BIDS file names will look like
        # sub-<participant_label>[_ses-<session_label>][_acq-<label>][_ce-<label>][_rec-<label>][_run-<index>][_mod-<label>]_<modality_label>
        # (that example is for anat. There may be other fields and labels in the other file types.)
        # So we split by underscores to get the individual field values.
        # However, some of the values may contain underscores themselves, so we have to check that each entry (save the last)
        #   contains a -.
        underscoreSplitListRaw = bidsFileName.split('_')
        underscoreSplitList = []

        for splitListEntryRaw in underscoreSplitListRaw[:-1]:
            if '-' not in splitListEntryRaw:
                underscoreSplitList[-1] = underscoreSplitList[-1] + splitListEntryRaw
            else:
                underscoreSplitList.append(splitListEntryRaw)

        bidsNameMap = dict(splitListEntry.split('-') for splitListEntry in underscoreSplitList)
        bidsNameMap['modality'] = underscoreSplitListRaw[-1]

        return bidsNameMap

    def bidsifySession(sessionDir):
        print("Checking for session structure in " + sessionDir)

        sessionBidsJsonPath = os.path.join(sessionDir, 'RESOURCES', 'BIDS', 'dataset_description.json')

        scansDir = os.path.join(sessionDir, 'SCANS')
        if not os.path.exists(scansDir):
            # I guess we don't have any scans with BIDS data in this session
            print("STOPPING. Could not find SCANS directory.")
            return

        print("Found SCANS directory. Checking scans for BIDS data.")

        bidsScans = []
        for scanId in os.listdir(scansDir):
            print("")
            print("Checking scan {}.".format(scanId))

            scanDir = os.path.join(scansDir, scanId)
            scanBidsDir = os.path.join(scanDir, 'BIDS')
            scanNiftiDir = os.path.join(scanDir, 'NIFTI')

            if not os.path.exists(scanBidsDir):
                # This scan does not have BIDS data
                print("SKIPPING. Scan {} does not have a BIDS directory.".format(scanId))
                continue

            scanBidsJsonGlobList = glob(scanBidsDir + '/*.json')
            if len(scanBidsJsonGlobList) != 1:
                # Something went wrong here. We should only have one JSON file in this directory.
                print("SKIPPING. Scan {} has {} JSON files in its BIDS directory. I expected to see one.".format(scanId, len(scanBidsJsonGlobList)))
                for jsonFile in scanBidsJsonGlobList:
                    print(jsonFile)
                continue
            scanBidsJsonFilePath = scanBidsJsonGlobList[0]
            scanBidsJsonFileName = os.path.basename(scanBidsJsonFilePath)
            scanBidsFileName = scanBidsJsonFileName.rstrip('.json')
            scanBidsNameMap = generateBidsNameMap(scanBidsFileName)

            print("BIDS JSON file name: {}".format(scanBidsJsonFileName))
            print("Name map: {}".format(scanBidsNameMap))

            if not scanBidsNameMap.get('sub') or not scanBidsNameMap.get('modality'):
                # Either 'sub' or 'modality' or both weren't found. Something is wrong. Let's find out what.
                if not scanBidsNameMap.get('sub') and not scanBidsNameMap.get('modality'):
                    print("SKIPPING. Neither 'sub' nor 'modality' could be parsed from the BIDS JSON file name.")
                elif not scanBidsNameMap.get('sub'):
                    print("SKIPPING. Could not parse 'sub' from the BIDS JSON file name.")
                else:
                    print("SKIPPING. Could not parse 'modality' from the BIDS JSON file name.")
                continue

            scanBidsDirFilePaths = glob(os.path.join(scanBidsDir, scanBidsFileName) + '.*')
            scanNiftiDirFilePaths = glob(os.path.join(scanNiftiDir, scanBidsFileName) + '.*')
            allFilePaths = scanBidsDirFilePaths + scanNiftiDirFilePaths

            bidsScan = BidsScan(scanId, scanBidsNameMap, *allFilePaths)
            if not bidsScan.subDir:
                print("SKIPPING. Could not determine subdirectory for modality {}.".format(bidsScan.modality))
                continue

            bidsScans.append(bidsScan)
            print("Done checking scan {}.".format(scanId))

        print("")
        print("Done checking all scans.")
        return bidsScans

    def getSubjectForBidsScans(bidsScanList):
        print("")
        print("Finding subject for list of BIDS scans.")
        subjects = list({bidsScan.subject for bidsScan in bidsScanList if bidsScan.subject})

        if len(subjects) == 1:
            print("Found subject {}.".format(subjects[0]))
            return subjects[0]
        elif len(subjects) > 1:
            print("ERROR: Found more than one subject: {}.".format(", ".join(subjects)))
        else:
            print("ERROR: Found no subjects.")

        return None

    def copyScanBidsFiles(destDirBase, bidsScanList):
        # First make all the "anat", "func", etc. subdirectories that we will need
        for subDir in {scan.subDir for scan in bidsScanList}:
            os.mkdir(os.path.join(destDirBase, subDir))

        # Now go through all the scans and copy their files into the correct subdirectory
        for scan in bidsScanList:
            destDir = os.path.join(destDirBase, scan.subDir)
            for f in scan.sourceFiles:
                shutil.copy(f, destDir)

    version = "1.0"
    args = docopt(__doc__, version=version)

    inputDir = args['<inputDir>']
    outputDir = args['<outputDir>']

    print("Input dir: {}".format(inputDir))
    print("Output dir: {}".format(outputDir))

    # First check if the input directory is a session directory
    sessionBidsScans = bidsifySession(inputDir)

    bidsSubjectMap = {}
    if sessionBidsScans:
        subject = getSubjectForBidsScans(sessionBidsScans)
        if not subject:
            # We would have already printed an error message, so no need to print anything here
            sys.exit(1)
        bidsSubjectMap = {subject: BidsSubject(subject, bidsScans=sessionBidsScans)}
    else:
        # Ok, we didn't find any BIDS scan directories in inputDir. We may be looking at a collection of session directories.
        print("")
        print("Checking subdirectories of {}.".format(inputDir))

        for subSessionDir in os.listdir(inputDir):
            subSessionBidsScans = bidsifySession(os.path.join(inputDir, subSessionDir))
            if subSessionBidsScans:
                subject = getSubjectForBidsScans(subSessionBidsScans)
                if not subject:
                    print("SKIPPING. Could not determine subject for session {}.".format(subSessionDir))
                    continue

                print("Adding BIDS session {} to list for subject {}.".format(subSessionDir, subject))
                bidsSession = BidsSession(subSessionDir, subSessionBidsScans)
                if subject not in bidsSubjectMap:
                    bidsSubjectMap[subject] = BidsSubject(subject, bidsSession=bidsSession)
                else:
                    bidsSubjectMap[subject].addBidsSession(bidsSession)

            else:
                print("No BIDS data found in session {}.".format(subSessionDir))

    print("")

    if not bidsSubjectMap:
        print("No BIDS data found anywhere in inputDir {}.".format(inputDir))
        sys.exit(1)

    print("")
    allHaveSessions = True
    allHaveScans = True
    for bidsSubject in bidsSubjectMap.itervalues():
        allHaveSessions = allHaveSessions and bidsSubject.hasSessions()
        allHaveScans = allHaveScans and bidsSubject.hasScans()

    if not (allHaveSessions ^ allHaveScans):
        print("ERROR: Somehow we have a mix of subjects with explicit sessions and subjects without explicit sessions. We must have either all subjects with sessions, or all subjects without. They cannot be mixed.")
        sys.exit(1)

    print("Copying BIDS data.")
    for bidsSubject in bidsSubjectMap.itervalues():
        subjectDir = os.path.join(outputDir, "sub-" + bidsSubject.subjectLabel)
        os.mkdir(subjectDir)

        if allHaveSessions:
            for bidsSession in bidsSubject.bidsSessions:
                sessionDir = os.path.join(subjectDir, "ses-" + bidsSession.sessionLabel)
                os.mkdir(sessionDir)
                copyScanBidsFiles(sessionDir, bidsSession.bidsScans)
        else:
            copyScanBidsFiles(subjectDir, bidsSubject.bidsScans)

    print("Done.")

## Writing a Setup Command

The setup command is a command, and as such it must follow the [command definition](https://wiki.xnat.org/display/CS/Command). However, setup commands cannot use the full set of features that most commands can. Setup commands cannot define any inputs, outputs, mounts, or wrappers. The mounts are always the same, `/input` and `/output`, so they need not be specified.

For a command to be recognized as a setup command, it **must** have the property

    "type": "docker-setup"

This is in contrast to standard commands, which have `"type": "docker"` (usually implicitly, since that is the default value).

The only properties that can be set in a setup command are

* `name` (**required**)
* `description` (optional, but recommended)
* `version` (optional, but recommended)
* `type` (**required** - must always have the value `"docker-setup"`)
* `command-line` (**required**)
* `working-directory` (optional)

Here is the command JSON for the `xnat/xnat2bids` setup command (as of 2017-12-20; the [current version](https://github.com/NrgXnat/docker-images/blob/master/setup-commands/xnat2bids/command.json) may be different).

    {
        "name": "xnat2bids",
        "description": "xnat2bids setup command. Transforms an XNAT session with BIDS and NIFTI resources into BIDS format.",
        "version": "1.0",
        "type": "docker-setup",
        "command-line": "xnat2bids.py /input /output"
    }

## Referencing a Setup Command in a Main Command

This is a simple matter of setting one property. On any Command Wrapper Input (external or derived) that has a value for the property `"provides-files-for-command-mount"`, you can set a value for the additional property `"via-setup-command"`. This will tell the container service to take the files from the input, run them through the indicated setup command, and then give the resulting files to the container.

The value for the `"via-setup-command"` property should be in the docker image format: `repo/image:version`. So to reference the `xnat2bids` setup command, we could use the value `xnat/xnat2bids:1.0` or `xnat/xnat2bids:latest`.

Additionally, we support one additional property in this value: the command name. This allows you to create one setup image which can have multiple commands. The full format of the `"via-setup-command"` property is `repo/image:version:commandname`.

For example, here is a snippet from the `xnat/bids-mriqc` command (as of 2017-12-20; the [current version](https://github.com/NrgXnat/docker-images/blob/master/bids-mriqc/command.json) may be different). Here we can see how the command references the `xnat2bids` setup command in the `xnat/xnat2bids-setup` image.

    {
        "name": "bids-mriqc",
        ...,
        "mounts": [
            {
                "name": "in",
                "writable": "false",
                "path": "/input"
            },
            ...
        ],
        ...,
        "xnat": [
            {
                "name": "bids-mriqc-session",
                "description": "Run the MRIQC BIDS App with a session mounted",
                "contexts": ["xnat:imageSessionData"],
                "external-inputs": [
                    {
                        "name": "session",
                        "description": "Input session",
                        "type": "Session",
                        "required": true,
                        "provides-files-for-command-mount": "in",
                        "via-setup-command": "xnat/xnat2bids-setup:1.0:xnat2bids"
                    }
                ],
                ...
            }
        ]
    }
