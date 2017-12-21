<!-- id: 53674279 -->

You can launch containers through the ordinary XNAT UI.

First, a command must be added to your XNAT, and its wrapper must be enabled in your project. Then when you are on a page where a command and wrapper are available, a Run Containers menu will appear in the Actions box.

SCREENSHOT Page with Run containers menu

Inside the Run Containers menu are all the different command wrappers that can be used to launch a container with the object you are viewing.

SCREENSHOT Open Run containers menu, show some wrappers inside

Click on one of those command wrappers, and you will be presented with a Launch Container dialog. Here you can provide or customize the values of various arguments that will be used to launch the container.

SCREENSHOT Launch container dialog

After you submit, the container will be launched.

## A note on scan-level launching

Some command wrappers are configured to launch on a particular scan. There is no User Interface in any existing XNAT version to launch containers from these command wrappers, because there are no buttons on the scan listings that can be customized in this way.

However, there is an XNAT plugin that can provide this function: the [selectable table plugin](https://bitbucket.org/xnatdev/selectable-table-plugin). You can download this plugin jar (from the [downloads page](https://bitbucket.org/xnatdev/selectable-table-plugin/downloads/) in the repo) and install it into your XNAT. It will customize the scan tables and enable you to launch any command wrappers that are defined to launch on scans. You can even launch in bulk on multiple scans at once.

SCREENSHOT Scan table with selectable table installed
