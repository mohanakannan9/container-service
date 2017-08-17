# Changelog

## 1.2

Not yet released.

### Features

* Ping server on `GET /docker/server` and create
* [CS-111][], [CS-285][] Ping hub on /xapi/hubs operations and reflect that ping status in Admin UI
* [CS-62][] Special-case an error when someone wants to POST to /images/save but instead GETs /images/save.
* [CS-215][] POST /xapi/docker/pull will now return a 404 rather than a 500 when it cannot find the image you are attempting to pull
* [CS-318][] Containers returned from `GET /xapi/containers` and `GET /xapi/containers/{id}` now include top-level `status` property. This is mostly equal to the status in the most recent history item on the container, except for mapping the docker event statuses to more user-friendly statuses.
    * `created` -> `Created`
    * `started` -> `Running`
    * `die` -> `Done`
    * `kill` -> `Killed`
    * `oom` -> `Killed (Out of memory)`
* [CS-227][] Container inputs are no longer recorded with type `wrapper`. Now all wrapper inputs will be recorded as either `wrapper-external` or `wrapper-derived`. Containers launched and recorded before this version will still have inputs of type `wrapper`; we can't migrate them because information has been lost.
* [CS-145][], [CS-146][] Add convenience functions to the UI to enable / disable all commands in a site or in a project
* [CS-256][], [CS-307][] Force users to confirm delete request on an image that has had containers run in that XNAT
* [CS-205][], [CS-281][] Allow admins to view container logs directly in the container history table

[CS-111]: https://issues.xnat.org/browse/CS-111
[CS-285]: https://issues.xnat.org/browse/CS-285
[CS-62]: https://issues.xnat.org/browse/CS-62
[CS-215]: https://issues.xnat.org/browse/CS-215
[CS-318]: https://issues.xnat.org/browse/CS-318
[CS-227]: https://issues.xnat.org/browse/CS-227
[CS-145]: https://issues.xnat.org/browse/CS-145
[CS-146]: https://issues.xnat.org/browse/CS-146
[CS-256]: https://issues.xnat.org/browse/CS-256
[CS-307]: https://issues.xnat.org/browse/CS-307
[CS-205]: https://issues.xnat.org/browse/CS-205
[CS-281]: https://issues.xnat.org/browse/CS-281

### Bugfixes

* [CS-263][] When finalizing a container, check if user can edit parent before creating new resource. (Sorry, I thought I had already done this.)
* [CS-347][] Prevent project settings from overwritingn site-wide preferences for command configurations
* [XXX-60][], [XXX-61][] Prevent users from launching containers with input values containing strings that could be used for arbitrary code execution:

        ;
        `
        &&
        ||
        (
* [CS-349][] Better handle `/experiment/{assessor id}` URIs as inputs for assessor external inputs. (They will not be able to mount files, though. See [CS-354])

* [CS-337][] Fail more gracefully when user tries to use a docker host url with no scheme. An error message is logged that is not super helpful right now, but will become a little bit more helpful in a future version once [spotify/docker-client#870][] is merged and released.
* [CS-279][] Explicitly restrict access to container launch UI to project users with edit permissions (i.e. owners and members)
* [CS-293][], [CS-297][] Remove non-functional "Delete" buttons from instances in the UI where it was not supported
* [CS-341][] `GET /{some identifiers}/launch` returns `user-settable` boolean property on inputs
* [CS-339][], [CS-340][] Fix faulty display and handling of non-user-settable inputs in container launch UI
* [CS-274][] Improve functioning of Project opt-in settings.
* [CS-212][] Improve display and readability of error messages throughout
* [CS-317][], [CS-273][] Taxonomy cleanup in UI for consistency

[CS-263]: https://issues.xnat.org/browse/CS-263
[CS-347]: https://issues.xnat.org/browse/CS-347
[CS-337]: https://issues.xnat.org/browse/CS-337
[spotify/docker-client#870]: https://github.com/spotify/docker-client/pull/870
[XXX-60]: https://issues.xnat.org/browse/XXX-60
[XXX-61]: https://issues.xnat.org/browse/XXX-61
[CS-279]: https://issues.xnat.org/browse/CS-279
[CS-293]: https://issues.xnat.org/browse/CS-293
[CS-341]: https://issues.xnat.org/browse/CS-341
[CS-339]: https://issues.xnat.org/browse/CS-339
[CS-340]: https://issues.xnat.org/browse/CS-340
[CS-297]: https://issues.xnat.org/browse/CS-297
[CS-274]: https://issues.xnat.org/browse/CS-274
[CS-212]: https://issues.xnat.org/browse/CS-212
[CS-317]: https://issues.xnat.org/browse/CS-317
[CS-273]: https://issues.xnat.org/browse/CS-273
[CS-349]: https://issues.xnat.org/browse/CS-349
[CS-354]: https://issues.xnat.org/browse/CS-354

## 1.1

[Released 2017-08-04](https://github.com/NrgXnat/container-service/releases/tag/v1.1).

### Features

* Integrated UI container launcher controls with the [Selectable Table plugin](https://bitbucket.org/xnatdev/selectable-table-plugin) to allow users to run containers on individual scans or multiple selected scans in bulk.
* Added master controls in the Admin UI and Project Settings UI to enable and disable all available commands for individual projects and at the site-wide level.
* [CS-286][] Remove unused enable-all/disable-all APIs
* [CS-288][] Project enabled API now returns an object with project enabled boolean, project name, and command enabled boolean.
* Change launch report format. Now instead of showing either wrapper ID or command ID + wrapper name (depending on which API you used to launch the container), the launch report always shows wrapper ID and command ID. The IDs are now formatted as integers, not strings.

[CS-286]: https://issues.xnat.org/browse/CS-286
[CS-288]: https://issues.xnat.org/browse/CS-288

### Bugfixes

* [CS-282][] (Not actually fixed yet.) The existing (unused) `/container-service/settings` API has been removed. This clears the way for the API to be refactored in the next version.
* [CS-289][] Mounting a session no longer mounts the project
* Mark command and wrapper descriptions in LaunchUi as Nullable. This prevents a potential bug that I haven't yet seen.

[CS-282]: https://issues.xnat.org/browse/CS-282
[CS-289]: https://issues.xnat.org/browse/CS-289

## 1.0

[Released 2017-07-28](https://github.com/NrgXnat/container-service/releases/tag/v1.0).

### Features

* Added Changelog
* Check if command exists before saving, rather than catching exception when it fails to save.
* [CS-205][] Add `/containers/{id}/logs` and `/containers/{id}/logs/{stdout|stderr}` APIs to fetch container logs.
* Rename wrapper input properties to remove "xnat":
    * `derived-from-xnat-input` -> `derived-from-wrapper-input`
    * `as-a-child-of-xnat-input` -> `as-a-child-of-wrapper-input`
* "Create docker server" API returns 201 on success, not 202.
* Add "directory" property to Session and Assessor model objects

[CS-205]: https://issues.xnat.org/browse/CS-205

### Bugfixes

* Add some additional null checks to services
* [CS-177][] Fix for untagged images showing as ':' in UI
* [CS-276][] Correctly track command and wrapper identifiers in LaunchReport
* Could not read containers from db. Broken by 3385972b.
* Wrapper database tables were broken after 7b9a9a28.

[CS-177]: https://issues.xnat.org/browse/CS-177
[CS-276]: https://issues.xnat.org/browse/CS-276

## 1.0-alpha.3-hotfix
[Released 2017-07-25](https://github.com/NrgXnat/container-service/releases/tag/v1.0-alpha.3-hotfix).

### Features

* [CS-242][] Add REST API to support bulk container launch UI
* Add resiliency to launcher if no default value is provided for a child input

[CS-242]: https://issues.xnat.org/browse/CS-242

### Bugfixes
* [CS-257][] Hide “Create Automation” button from project owners if they do not have admin privileges. Depends on [XNAT-5044](https://issues.xnat.org/browse/XNAT-5044) change.
* Fix: container -> container entity should use existing entity in db if it exists
* Fix: Initialize container entity when retrieving by container id

[CS-257]: https://issues.xnat.org/browse/CS-257

## 1.0-alpha.3

Let's consider this the first version for purposes of this changelog. Because we haven't kept track of what has changed from the beginning of the repo until now. You can always go dig through the commits if you are really curious.