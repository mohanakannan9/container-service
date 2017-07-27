# Changelog

## 1.0-alpha.4

Not yet released

### Features

* Added Changelog

### Bugfixes


## 1.0-alpha.3-hotfix
[Released 2017-07-25](https://github.com/NrgXnat/container-service/releases/tag/v1.0-alpha.3-hotfix).

### Features

* Add REST API to support bulk container launch UI ([CS-242][])
* Add resiliency to launcher if no default value is provided for a child input

### Bugfixes
* Hide “Create Automation” button from project owners if they do not have admin privileges. Depends on [XNAT-5044](https://issues.xnat.org/browse/XNAT-5044) change. ([CS-257][])
* Fix: container -> container entity should use existing entity in db if it exists
* Fix: Initialize container entity when retrieving by container id

[CS-242]: https://issues.xnat.org/browse/CS-242
[CS-257]: https://issues.xnat.org/browse/CS-257

## 1.0-alpha.3

Let's consider this the first version for purposes of this changelog. Because we haven't kept track of what has changed from the beginning of the repo until now. You can always go dig through the commits if you are really curious.