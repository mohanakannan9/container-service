# Changelog

## 1.2

Not yet released.

### Features

* Ping server on `GET /docker/server` and create
* [CS-111][] Ping hub on /xapi/hubs operations
* [CS-62][] Special-case an error when someone wants to POST to /images/save but instead GETs /images/save.

[CS-111]: https://issues.xnat.org/browse/CS-111
[CS-62]: https://issues.xnat.org/browse/CS-62

### Bugfixes

*

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