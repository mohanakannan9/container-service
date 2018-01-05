<!-- id: 51642428 -->

This document will describe how the container service can be extended to allow users to automatically upload / create assessors from the outputs of containers.

## Background and Purpose

Before the container service existed, the only option for processing in XNAT was to write a pipeline. Pipeline authors used a very common pattern when generating files from some image processing algorithm.

1. The main part of the processing would run to completion.
2. A small script would read through the files resulting from the processing and use those results to build an XML representation of an assessor.
3. That XML would be uploaded to XNAT, creating a new assessor object.
4. All the processing results files would be uploaded to one or more new resources underneath the assessor object.

When writing pipelines, this same pattern had to be re-built with every new pipeline. One of the design goals of the container service is to build in common patterns like these. In this way the pattern can be invoked with a few configurable options, and the container service performs the tasks laid out in the pattern.

The author of the docker image that performs the image processing should have no responsibility for any of these features. Nothing about this image or its command should be XNAT specific. In particular, they should not need to include an XNAT assessor creation script in their image. The author of the command wrapper, however, does have some responsibility for creating and uploading the assessor. They must indicate to the container service that they want to make an assessor and where the XML file can be found, and which files should be uploaded to a resource. However, the container service should enable them to perform this common task with a few simple settings.

To restate this in terms of the design goals of this feature, the author of the command wrapper should be able to easily:

1. Generate the assessor XML from the processed files,
2. Instruct the container service to create a new assessor from the XML file, and
3. Instruct the container service to upload the processed files as a resource to that assessor.

Currently the container service does not support this pattern. Each of the three points above will require new development. I will discuss each of these in turn.

## Feature: Generate Assessor XML from Processed Files

This feature is the most novel and difficult of the three. However, I have a path that I intend to take to implement this feature. It is related to an  idea that I am in the process of building into a new feature for a different purpose. The idea is that authors of the command wrappers can create auxiliary containers for specific purposes, and insert those into certain places in the command/container life cycle. When a launch request comes in for the "main" container, the container service will invoke these auxiliary containers as defined in the command wrapper.

### Setup Containers

The feature I am currently implementing based on the auxiliary container idea is the Setup Container ([CS-421](https://issues.xnat.org/browse/CS-421)). The setup container fits into the life cycle before the main container is launched. Its purpose is to stage the input files in whatever way the main container will expect to find them.

The use case for which the setup container feature is being designed is the BIDS Apps. These are a class of docker images that all expect their input data to be presented in BIDS format. The setup container will be responsible for reading the files out of the XNAT archive, copying them to another location, and organizing them according to the BIDS specification. When the setup container is finished, the container service will launch the desired BIDS App, mounting into it the BIDS data that was just prepared.

### "Teardown" Containers

The way I intend to allow command wrapper authors to generate the assessor XML is through adding a new auxiliary container stage in the command/container life cycle. This stage will be after the main container has been run, and before the outputs are uploaded back to XNAT. A container that is executed at this stage will be provided the processed files, with the intention that they can generate new XNAT-specific processed results from them, such as assessor XML files.

I have been calling them "teardown" containers. I think this is a bad name, but that is where my brain keeps going. It is in some sense the opposite of a "setup" container. I'm sure we could find something better.

Perhaps "prelaunch" and "postlaunch" would be a better pair of names.

## Feature: Instruct Container Service to Create New Assessor From XML File

This feature will, I expect, not be too difficult. For this feature, I need a way for the container service to take in a file from a specified path and use its contents to create a new XNAT object. This function is already performed in the XNAT XML Uploader code; users can upload an arbitrary XML file and whatever object it describes will be created. But that code is inaccessible to me from within the container service. So some internal service will have to be written that does this. I don't want to write and maintain a bunch of XNAT file uploading / object creation code in the container service. Instead, this seems to me like a general purpose feature that should be available through an internal XNAT API, which the container service could call.

I have a long-standing ticket open with XNAT for this feature: [XNAT-4556](https://issues.xnat.org/browse/XNAT-4556) XML import service.

## Feature: Instruct Container Service to Upload Processed Files as a Resource on Newly Created Assessor

Once the assessor has been created, we want to be able to upload all the processed files to it as one or more new resources. The container service already supports uploading container output files as a new resource, so we're most of the way there already. However, the feature will need to be extended to allow this use case.

Currently, the container service can only make new resources as children of *input* objects. In other words, if you send in a session as an input, we can upload output files as a new session resource. Or if you get one of that session's scans as a derived input, we can upload a new resource to that scan.

What we need to be able to do is to make a new resource as a child of an *output* object. We will be creating a new assessor. It doesn't exist when the container is launched, so it cannot be an input object. We will need to extend the `Command Wrapper Output Handler` syntax, and the container service code that processes these objects, to allow creating new resources on objects created by other output handlers. In that way, one output handler could instruct the container service to create an assessor from an XML file, then a second output handler could instruct the container service to take the processed files and upload them as a new resource on the assessor that was just created.
