# REST API
This document details the various REST API functions used to interact with the ACE backend. This is the way to create, manage, and view objects like Actions, Commands, Docker Images, and Scripts.

In general, all data objects should be sent as json, and the Content-Type header should be set appropriately to `Content-Type: application/json`. Similarly, responses will generally be sent as json, and the Accept header should be set to `Accept: application/json`.

### Status Codes
Each REST endpoint detailed below will list the status codes with which it can respond. But there is a common set of status codes with which all endpoints could respond. They are listed here in lieu of repeating for each entry.

- **401** - Must be authenticated
- **403** - Forbidden. Typically means the user's role does not permit access to the requested resource.
- **415** - Unsupported Media Type. Typically means the `Content-Type` header on the request has not been set properly.
- **500** - Internal server error


## Administer Docker Server
### `GET /xapi/docker/server`
Returns information about the Docker server configuration.

#### Example Request

    GET /xapi/docker/server
    Accept: application/json

#### Query Parameters

- **None**

#### JSON Parameters

- **None**

#### Example Response

    HTTP/1.1 200 OK
    Content-Type: application/json

    {
        "host": "http://docker.server.url/",
        "cert-path": "/path/to/certificate/files"
    }

#### Status Codes

- **200** - OK
- **404** - Not found. Must configure the server first.

### `POST /xapi/docker/server`
Configure the Docker server.

#### Example Request

    POST /xapi/docker/server
    Content-Type: application/json

    {
        "host": "http://docker.server.url/",
        "cert-path": "/path/to/certificate/files"
    }

#### Query Parameters

- **None**

#### JSON Parameters

- **host** - (Required) URL to Docker server.
- **cert-path** - Path to Docker server certificate files on XNAT host. Required if Docker server is configured to use HTTPS.

#### Example Response

    HTTP/1.1 201 Created

#### Status Codes

- **201** - Created
- **400** - Bad request. The `host` property was not set in the request body.

### `GET /xapi/docker/server/ping`
Ping Docker Server to test configuration.

#### Example Request

    GET /xapi/docker/server/ping

#### Query Parameters

- **None**

#### JSON Parameters

- **None**

#### Example Response

    HTTP/1.1 200 OK
    OK

#### Status Codes

- **200** - OK
- **424** - Failed dependency. The docker server has not been configured.
- **500** - Internal server error. The ping did not go through, or produced an error.

## Administer Docker Hubs
### `GET /xapi/docker/hubs`
Returns the list of known Docker Hubs.

#### Example Request

    GET /xapi/docker/hubs
    Accept: application/json

#### Query Parameters

- **None**

#### JSON Parameters

- **None**

#### Example Response

    HTTP/1.1 200 OK
    Content-type: application/json

    [
        {
            "url": "https://index.docker.io/v1/",
            "name": "Docker Hub"
        },
        {
            "url": "https://some.other.url"
            "name": "My private hub"
            "username": "user"
            "password": "I really should not be returning this"
            "email": "login.email@domain.com"
        }
    ]

#### Status Codes

- **200** - OK

### `POST /xapi/docker/hubs`
Create a new Docker Hub

#### Example Request

    POST /xapi/docker/hubs
    Content-type: application/json

    {
        "url": "https://some.other.url"
        "name": "My private hub"
        "username": "user"
        "password": "password"
        "email": "login.email@domain.com"
    }


#### Query Parameters

- **None**

#### JSON Parameters

- **url** - (Required) The URL of the Hub. Must be unique among all Hub definitions.
- **name** - A human-readable name of the Hub
- **username** - The username to use for authentication, if required
- **password** - The password to use for authentication, if required
- **email** - I don't know if this is necessary, I just know you can send it in. ¯\\_(ツ)_/¯

#### Example Response

    HTTP/1.1 201 Created

#### Status Codes

- **201** - Created
- **500** - Internal server error

### `GET /xapi/docker/hubs/ping`
<!-- TODO -->
Ping Docker Hub to test configuration.

#### Example Request

    GET /xapi/docker/hubs/ping?url=https://index.docker.io/v1/

#### Query Parameters

- **None**

#### JSON Parameters

- **None**

#### Example Response

    HTTP/1.1 200 OK
    OK

#### Status Codes

- **200** - OK
- **424** - Failed dependency. The docker server has not been configured.
- **500** - Internal server error. The ping did not go through, or produced an error.

## Administer Docker Images

### `GET /xapi/docker/images`
Get a list of all Docker images

#### Example Request

    GET /xapi/docker/images
    Accept: application/json

#### Query Parameters

- **from-db=true|false** - Default true. Fetch Image records from the XNAT database.
- **from-docker-server=true|false** - Default true. Fetch Image records from Docker Server.

#### JSON Parameters

- **none**

#### Example Response

    HTTP/1.1 200 OK
    Content-type: application/json

    [
        {
            "id": 1,
            "enabled": true,
            "created":,
            "updated":,
            "disabled",
            "name": "foo/foo",
            "imageId": "abc123efg",
            "repoTags": ["foo/foo:v1.0", "foo/foo:latest"],
            "labels": {
                "label0": "value0",
                "label1": "value1"
            },
            "in-database": true,
            "on-docker-server": true
        },
        {
            "id": 2,
            "enabled": true,
            "created":,
            "updated":,
            "disabled",
            "name": "The bar/bar image",
            "imageId": "123abc456",
            "repoTags": ["bar/bar:v99.99"],
            "labels": null,
            "in-database": true,
            "on-docker-server": true
        },
        {
            "imageId": "123456789",
            "repoTags": ["some/image"],
            "in-database": false,
            "on-docker-server": true
        }
    ]

#### Status Codes

- **200** - OK

### `GET /xapi/docker/images/{id}`
Get a specific Docker images

#### Example Request

    GET /xapi/docker/images/1
    Accept: application/json

#### Query Parameters

- **from-docker-server=true|false** - Default true. Check if image still exists on Docker Server.

#### JSON Parameters

- **none**

#### Example Response

    HTTP/1.1 200 OK
    Content-type: application/json

    {
        "id": 1,
        "enabled": true,
        "created":,
        "updated":,
        "disabled",
        "name": "foo/foo",
        "imageId": "abc123efg",
        "repoTags": ["foo/foo:v1.0", "foo/foo:latest"],
        "labels": {
            "label0": "value0",
            "label1": "value1"
        },
        "in-database": true,
        "on-docker-server": true
    }

#### Status Codes

- **200** - OK
- **404** - Not found. No image exists in the database with the given image ID.

### `POST /xapi/docker/images`
Save a new entry for a Docker image that is already on the Docker server

#### Example Request

    POST /xapi/docker/images
    Content-type: application/json

    {
        "name": "Arbitrary Name",
        "imageId": "0123456789abcdef",
    }

#### Query Parameters

- **None**

#### JSON Parameters

- **imageId** - (Required) The ID of the docker image, as reported by the Docker server. For more information, see Docker's guide on [working with images](https://docs.docker.com/engine/userguide/containers/dockerimages/).
- **name** - A user-friendly name. This will be displayed by XNAT anywhere users can view or select this image. If no name is provided, one of the image's repo tags will be randomly selected and used as the name.

#### Example Response

    HTTP/1.1 201 Created

#### Status Codes

- **201** - Created
- **400** - Bad request. No imageId set on request body.
- **404** - Not found. No image exists on the Docker server with the given image ID.

### pull new image from hub, which automagically saves to db
<!--TODO -->

### `DELETE /xapi/docker/images/{id}`
Delete image record from db (optionally also from docker server)

#### Example Request

    DELETE /xapi/docker/images/1?from-docker-server=false

#### Query Parameters

- **from-docker-server=true|false** - Default false. Also attempt to remove image from Docker Server.

#### JSON Parameters

- **None**

#### Example Response

    HTTP/1.1 200 OK

#### Status Codes

- **200** - OK
- **404** - Not found. No image exists in the database with the given image ID.
- **500** - Internal server error. Image deletion should be rolled back in this case.

## Administer Script Environments

## Commands

## Actions

## Executing Actions (ACEs)
