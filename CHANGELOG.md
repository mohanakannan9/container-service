# Changelog

## 1.0-alpha.4

Not yet released

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