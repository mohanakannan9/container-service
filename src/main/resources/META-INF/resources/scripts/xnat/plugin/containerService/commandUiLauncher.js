/*
 * web: commandUiLauncher.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Flexible script to be used in the UI to launch 
 */

console.log('commandUiLauncher.js');

var XNAT = getObject(XNAT || {});

(function(factory){
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function(){

    var launcher,
        undefined,
        launcherMenu = $('#container-launch-menu'),
        rootUrl = XNAT.url.rootUrl,
        csrfUrl = XNAT.url.csrfUrl,
        projectId = XNAT.data.context.projectID,
        xsiType = XNAT.data.context.xsiType;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.containerService = 
        getObject(XNAT.plugin.containerService || {});

    XNAT.plugin.containerService.launcher = launcher =
        getObject(XNAT.plugin.containerService.launcher || {});


    function errorHandler(e){
        console.log(e);
        xmodal.alert({
            title: 'Error',
            content: '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p>',
            okAction: function () {
                xmodal.closeAll();
            }
        });
    }
    function getEnabledCommandsUrl(appended){
        appended = isDefined(appended) ? appended : '';
        return rootUrl('/xapi/commands' + appended);
    }

    function getCommandUrl(commandId,wrapperName,appended){
        if (!commandId || !wrapperId) return false;
        appended = (appended) ? '/' + appended : '';
        return rootUrl('/xapi/commands/'+commandId+'/wrappers/'+wrapperName+appended);
    }

    function getProjectCommandConfigUrl(commandId,wrapperName){
        if (!projectId || !commandId || !wrapperName) return false;
        return rootUrl('/xapi/projects/'+projectId+'/commands/'+commandId+'/wrapper/'+wrapperName+'/config');
    }
    function getLauncherUI(wrapperId,rootElementName,rootElementValue){
        return rootUrl('/xapi/wrappers/'+wrapperId+'/launch?'+rootElementName+'='+rootElementValue);
    }
    function containerLaunchUrl(wrapperId){
        return csrfUrl('/xapi/wrappers/'+wrapperId+'/launch');
    }
    function bulkLaunchUrl(wrapperId){
        return csrfUrl('/xapi/wrappers/'+wrapperId+'/bulklaunch');
    }
    function sessionUrl(){
        var sessionId = (XNAT.data.context.isImageSession) ? XNAT.data.context.ID : null;
        if (!sessionId) return false;
        return rootUrl('/REST/experiments/'+sessionId);
    }
    function fullScanPath(scanId){
        var sessionId = (XNAT.data.context.isImageSession) ? XNAT.data.context.ID : null;
        if (!sessionId) return false;
        return '/experiments/'+sessionId+'/scans/'+scanId;
    }

    /*
     ** Launcher Options
     */

    function launchOneContainer(inputs,rootElement,wrapperId){
        var independentInputs = {},
            childInputs = {},
            advancedInputs = {};

        var inputList = Object.keys(inputs);

        if (inputList.find(function(input) { return input === rootElement; }) ) {
            // if the root element is specified in the list of inputs ...

            for (var i in inputs){
                if (inputs[i].parent) {
                    // iterate through each possible value and create a new hidden, disabled input
                    // when a parent input value is detected, the appropriate child input will be enabled
                    var parentInput = inputs[inputs[i].parent];

                    for (var k in inputs[i].ui) {
                        var key = i+'-'+k;
                        childInputs[key] = inputs[i];
                        if (parentInput.ui.default.type !== 'static' && parentInput.ui.default.type !== 'hidden') {
                            childInputs[key].disabled = true;
                            childInputs[key].hidden = true;
                        }
                        childInputs[key].name = i;
                        childInputs[key].type = inputs[i].ui[k].type;
                        childInputs[key].value = inputs[i].ui[k].values[0].value || '';
                    }

                } else if (inputs[i].advanced) {
                    advancedInputs[i] = inputs[i];
                    advancedInputs[i].type = inputs[i].ui.default.type;
                    advancedInputs[i].value = inputs[i].ui.default.values[0].value || '';
                } else {
                    independentInputs[i] = inputs[i];
                    independentInputs[i].type = inputs[i].ui.default.type;
                    independentInputs[i].value = inputs[i].ui.default.values[0].value || '';
                }
            }


            XNAT.ui.dialog.open({
                title: 'Set Container Launch Values',
                content: '<div class="panel"></div>',
                width: 550,
                scroll: true,
                beforeShow: function(obj){
                    var $panel = obj.$modal.find('.panel');
                    $panel.spawn('p','Please specify settings for this container.');

                    $panel.append(launcher.formInputs(independentInputs));
                    $panel.append(launcher.formInputs(childInputs));
                    $panel.append(launcher.formInputs(advancedInputs,true));
                },
                buttons: [
                    {
                        label: 'Run Container',
                        isDefault: true,
                        close: false,
                        action: function(obj){
                            var $panel = obj.$modal.find('.panel'),
                                targetData = {};

                            // gather form input values
                            targetData[rootElement] = $panel.find('input[name='+rootElement+']').val();

                            $panel.find('input').not(':disabled').not('[type=checkbox]').not('[type=radio]').not('[name='+rootElement+']').each(function(){
                                // get the name and value from each text element and add it to our data to post
                                var key = $(this).prop('name');
                                targetData[key] = $(this).val();
                            });

                            $panel.find('input[type=checkbox]').not(':disabled').each(function(){
                                var key = $(this).prop('name');
                                var val = ($(this).is(':checked')) ? $(this).val() : false;
                                targetData[key] = val;
                            });

                            $panel.find('select').not(':disabled').each(function(){
                                var key = $(this).prop('name');
                                var val = $(this).find('option:selected').val();
                                targetData[key] = val;
                            });

                            var dataToPost = targetData;

                            xmodal.loading.open({ title: 'Launching Container...' });

                            XNAT.xhr.postJSON({
                                url: containerLaunchUrl(wrapperId),
                                data: JSON.stringify(dataToPost),
                                success: function(data){
                                    xmodal.loading.close();

                                    XNAT.ui.dialog.open({
                                        title: 'Container Launch Success',
                                        content: spawn('p',{ css: { 'word-wrap': 'break-word'}}, data ),
                                        buttons: [
                                            {
                                                label: 'OK',
                                                isDefault: true,
                                                close: true,
                                                action: XNAT.ui.dialog.closeAll()
                                            }
                                        ]
                                    })
                                },
                                fail: function (e) {
                                    errorHandler(e);
                                }
                            });

                        }
                    },
                    {
                        label: 'Cancel',
                        isDefault: false,
                        close: true
                    }
                ]
            });

        } else {
            errorHandler({
                responseText: 'Could not launch command. Root element "'+rootElement+'" not found in the list of inputs provided.'
            });
        }
    }

    function launchManyContainers(inputs,rootElement,wrapperId,targets){
        var independentInputs = {},
            childInputs = {},
            advancedInputs = {};

        var inputList = Object.keys(inputs);

        if ( inputList.find(function(input){ return input === rootElement; }) ) { // if the specified root element matches an input parameter, we can proceed

            for (var i in inputs){
                if (inputs[i].parent) {
                    // iterate through each possible value and create a new hidden, disabled input
                    // when a parent input value is detected, the appropriate child input will be enabled
                    var parentInput = inputs[inputs[i].parent];

                    for (var k in inputs[i].ui) {
                        var key = i+'-'+k;
                        childInputs[key] = inputs[i];
                        if (parentInput.ui.default.type !== 'static' && parentInput.ui.default.type !== 'hidden') {
                            // if parent inputs can be set by the user, disable each possible child input until the user sets a value for that parent input.
                            childInputs[key].disabled = true;
                            childInputs[key].hidden = true;
                        }
                        childInputs[key].name = i;
                        childInputs[key].type = inputs[i].ui[k].type;
                        childInputs[key].value = inputs[i].ui[k].values[0].value || '';
                    }

                } else if (inputs[i].advanced) {
                    advancedInputs[i] = inputs[i];
                    advancedInputs[i].type = inputs[i].ui.default.type;
                    advancedInputs[i].value = inputs[i].ui.default.values[0].value || '';
                } else {
                    independentInputs[i] = inputs[i];
                    independentInputs[i].type = inputs[i].ui.default.type;
                    independentInputs[i].value = inputs[i].ui.default.values[0].value || '';
                }
            }

            XNAT.ui.dialog.open({
                title: 'Set Container Launch Values for '+targets.length+' '+rootElement+'(s)',
                content: spawn('div.panel'),
                width: 550,
                scroll: true,
                nuke: true,
                beforeShow: function(obj){
                    var $panel = obj.$modal.find('.panel');
                    $panel.spawn('p','Select settings to be applied to each item in your list.');

                    // overwrite defaults for root element input in config object. Display a list of targets with no input. We will break this out later.
                    inputs[rootElement].type="staticList";
                    inputs[rootElement].value=targets.join(',');

                    // add inputs
                    $panel.append(launcher.formInputs(independentInputs));
                    $panel.append(launcher.formInputs(childInputs));
                    $panel.append(launcher.formInputs(advancedInputs,true));
                },
                buttons: [
                    {
                        label: 'Run Container(s)',
                        close: false,
                        isDefault: true,
                        action: function(obj){
                            xmodal.loading.open({ title: "Attempting to launch containers..." });

                            var $panel = obj.$modal.find('.panel'),
                                dataToPost = [];

                            targets.forEach(function(target){ // iterate over each item in the list of selected items to run container on
                                var targetData = {};
                                targetData[rootElement] = target;

                                $panel.find('input').not('[type=checkbox]').not('[type=radio]').not('[name='+rootElement+']').each(function(){
                                    // get the name and value from each text element and add it to our data to post
                                    var key = $(this).prop('name');
                                    targetData[key] = $(this).val();
                                });

                                $panel.find('input[type=checkbox]').each(function(){
                                    var key = $(this).prop('name');
                                    var val = ($(this).is(':checked')) ? $(this).val() : false;
                                    targetData[key] = val;
                                });

                                $panel.find('select').each(function(){
                                    var key = $(this).prop('name');
                                    var val = $(this).find('option:selected').val();
                                    targetData[key] = val;
                                });

                                dataToPost.push(targetData);
                            });

                            XNAT.xhr.postJSON({
                                url: '/xapi/wrappers/'+wrapperId+'/bulklaunch',
                                data: JSON.stringify(dataToPost),
                                success: function(data){
                                    xmodal.loading.close();

                                    var successRecord = '',
                                        failureRecord = '';

                                    if (data.successes.length > 0) {
                                        successRecord += '<p>Successfully launched containers on: </p><ul>';
                                        data.successes.forEach(function(success){
                                            successRecord += '<li>'+success.params[rootElement]+'</li>';
                                        });
                                        successRecord += '</ul>';
                                    }

                                    if (data.failures.length > 0) {
                                        failureRecord += '<p>Failed to launch containers on: </p><ul>';
                                        data.failures.forEach(function(failure){
                                            failureRecord += '<li>'+failure.params[rootElement]+'</li>';
                                        });
                                        failureRecord += '</ul>';
                                    }

                                    XNAT.ui.dialog.open({
                                        title: 'Container Launch Record',
                                        content: successRecord + failureRecord,
                                        buttons: [
                                            {
                                                label: 'OK',
                                                isDefault: true,
                                                close: true,
                                                action: XNAT.ui.dialog.closeAll()
                                            }
                                        ]
                                    })
                                },
                                fail: function(e){
                                    errorHandler(e);
                                }
                            })
                        }
                    },
                    {
                        label: 'Cancel',
                        isDefault: false,
                        close: true
                    }
                ]
            });

        } else {
            errorHandler({
                statusText: 'Root element mismatch',
                responseText: 'No instance of '+rootElement+' was found in the list of inputs for this command'
            });
        }
    }

    launcher.defaultLauncher = function(wrapperId,rootElement,rootElementValue){
        rootElementValue = rootElementValue || XNAT.data.context.ID; // if no value is provided, assume that the current page context provides the value.

        if (!rootElementValue) {
            errorHandler({ responseText: 'Could not launch UI. No value provided for '+rootElement+'.' });
            return false;
        }

        xmodal.loading.open({ title: 'Configuring Container Launcher' });

        XNAT.xhr.getJSON({
            url: getLauncherUI(wrapperId,rootElement,rootElementValue),
            fail: function(e){
                xmodal.loading.close();
                errorHandler({
                    statusText: e.statusText,
                    responseText: 'Could not launch UI with value: "'+rootElementValue+'" for root element: "'+rootElement+'".'
                });
            },
            success: function(data){
                xmodal.loading.close();
                var inputs = data.inputs;
                launchOneContainer(inputs,rootElement,wrapperId);
            }
        })
    };

    launcher.bulkSelectDialog = function(commandId,wrapperId,rootElement){
        // get command definition
        var launcherConfig = launcher.getConfig(commandId,wrapperId),
            advancedInputs = {},
            normalInputs = {};

        var inputs = launcherConfig.inputs;

        var inputList = Object.keys(inputs);
        if ( inputList.find(function(input){ return input === rootElement; }) ) { // if the specified root element matches an input parameter, we can proceed

            inputs[rootElement].type = 'scanSelectMany';

            // separate normal inputs from advanced inputs
            for (var i in inputs) {
                if (paramObj[i].advanced) {
                    advancedInputs[i] = paramObj[i];
                } else {
                    normalInputs[i] = paramObj[i];
                }
            }

            // open the template form in a modal
            XNAT.ui.dialog.open({
                title: 'Set Config Values',
                content: spawn('div.panel'),
                width: 550,
                scroll: true,
                nuke: true,
                beforeShow: function (obj) {
                    var $panel = obj.$modal.find('.panel');
                    $panel.spawn('p', 'Please specify settings for this container.');

                    // place normal and advanced input form elements into the template
                    $panel.append(launcher.formInputs(normalInputs));

                    if (Object.keys(advancedInputs).length > 0) {
                        $panel.spawn('p','Advanced Inputs');
                        $panel.append(launcher.formInputs(advancedInputs,true));
                    }
                },
                buttons: [
                    {
                        label: 'Run Container',
                        close: false,
                        isDefault: true,
                        action: function (obj) {
                            var $panel = obj.$modal.find('.panel');
                            var selectedItems = $panel.find('input[name='+rootElement+']:checked');
                            if (selectedItems.length === 0) {
                                XNAT.ui.banner.top(2000,'<b>Error:</b> You must select at least one '+rootElement, 'error');
                                return false;
                            } else {
                                var dataToPost = [];
                                $(selectedItems).each(function(){
                                    var itemsToPost = {};
                                    itemsToPost[rootElement] = $(this).val();

                                    $panel.find('input').not('[type=checkbox]').not('[type=radio]').each(function () {
                                        // get the name and value from each text element and add it to our data to post
                                        var key = $(this).prop('name');
                                        itemsToPost[key] = $(this).val();
                                    });

                                    $panel.find('input[type=checkbox]').not('input[name='+rootElement+']').each(function () {
                                        var key = $(this).prop('name');
                                        var val = ($(this).is(':checked')) ? $(this).val() : false;
                                        itemsToPost[key] = val;
                                    });

                                    $panel.find('select').each(function () {
                                        var key = $(this).prop('name');
                                        var val = $(this).find('option:selected').val();
                                        itemsToPost[key] = val;
                                    });

                                    dataToPost.push(itemsToPost);
                                });

                                console.log(dataToPost);

                                XNAT.xhr.postJSON({
                                    url: bulkLaunchUrl(wrapperId),
                                    data: JSON.stringify(dataToPost),
                                    success: function(data){
                                        xmodal.loading.close();

                                        var successRecord = '',
                                            failureRecord = '';

                                        if (data.successes.length > 0) {
                                            successRecord += '<p>Successfully launched containers on: </p><ul>';
                                            data.successes.forEach(function(success){
                                                successRecord += '<li>'+success.params[rootElement]+'</li>';
                                            });
                                            successRecord += '</ul>';
                                        }

                                        if (data.failures.length > 0) {
                                            failureRecord += '<p>Failed to launch containers on: </p><ul>';
                                            data.failures.forEach(function(failure){
                                                failureRecord += '<li>'+failure.params[rootElement]+'</li>';
                                            });
                                            failureRecord += '</ul>';
                                        }

                                        XNAT.ui.dialog.open({
                                            title: 'Container Launch Record',
                                            content: successRecord + failureRecord,
                                            buttons: [
                                                {
                                                    label: 'OK',
                                                    isDefault: true,
                                                    close: true,
                                                    action: XNAT.ui.dialog.closeAll()
                                                }
                                            ]
                                        })
                                    },
                                    fail: function (e) {
                                        errorHandler(e);
                                    }
                                });
                            }

                        }
                    },
                    {
                        label: 'Cancel',
                        isDefault: false,
                        close: true
                    }
                ]
            });
        } else {
            errorHandler({
                statusText: 'Root element mismatch',
                responseText: 'No instance of ' + rootElement + ' was found in the list of inputs for this command'
            });
        }

    };

    launcher.singleScanDialog = function(wrapperId,rootElementPath){
        // end goal is submitting to /xapi/commands/launch/
        // need to build UI with input values from /xapi/wrappers/{id}/launchui, specifying the root element name and path

        XNAT.xhr.getJSON({
            url: getLauncherUI(wrapperId,'scan',rootElementPath),
            fail: function(e){
                errorHandler(e);
            },
            success: function(data){
                var inputs = data.inputs;
                var rootElement = 'scan';
                launchOneContainer(inputs,rootElement,wrapperId);
            }
        });
    };

    launcher.bulkLaunchDialog = function(wrapperId,rootElement,targets){
        // 'targets' should be formatted as a one-dimensional array of XNAT data values (i.e. scan IDs) that a container will run on in series.
        // the 'root element' should match one of the inputs in the command config object, and overwrite it with the values provided in the 'targets' array

        if (!targets || targets.length === 0) return false;

        var rootElementValue = targets[0]; // assume that the first target value passed will be representative of all target values for the purposes of generating a UI.

        xmodal.loading.open({ title: 'Configuring Container Launcher' });
        XNAT.xhr.getJSON({
            url: getLauncherUI(wrapperId,rootElement,rootElementValue),
            fail: function(e){
                xmodal.loading.close();
                errorHandler({
                    statusText: e.statusText,
                    responseText: 'Could not launch UI with value: "'+rootElementValue+'" for root element: "'+rootElement+'".'
                });
            },
            success: function(data){
                xmodal.loading.close();
                var inputs = data.inputs;
                launchManyContainers(inputs,rootElement,wrapperId,targets);
            }
        });


    };

    /*
     * Build UI for menu selection
     */

    var containerMenuItems = [
        {
            text: 'Run Containers',
            url: '#run',
            submenu: {
                id: 'containerSubmenuItems',
                itemdata: [
                ]
            }
        }
    ];

    launcher.addMenuItem = function(item,itemSet){
        itemSet = itemSet || [];
        itemSet.push(
            spawn('li', [
                spawn('a', {
                    html: item['wrapper-description'],
                    href: '#!',
                    className: 'commandLauncher',
                    data: {
                        wrapperid: item['wrapper-id'],
                        rootElementName: item['root-element-name'],
                        uri: item['uri'],
                        launcher: item['launcher']
                    }
                })
            ]));
        return itemSet;
    };

    launcher.createMenu = function(target,itemSet){
        /*
        var containerMenu = spawn('li.has-submenu',[
            spawn(['a',{ href: '#!', html: 'Run' }]),
            spawn(['ul.dropdown-submenu', itemSet ])
        ]);
        */

        target.append(itemSet);
    };

    /* to be replaced when we kill YUI */
    launcher.addYUIMenuItem = function(item){
        var launcher = item.launcher || "default";
        containerMenuItems[0].submenu.itemdata.push({
            text: item['wrapper-description'],
            url: 'javascript:openCommandLauncher({ wrapperid:"'+item['wrapper-id']+'", launcher: "'+launcher+'", rootElement: "'+ item['root-element-name'] + '" })'
        });
    };

    launcher.createYUIMenu = function(target){
        target = target || 'actionsMenu';
        var containerMenu = new YAHOO.widget.Menu('containerMenu', { autosubmenudisplay:true, scrollincrement:5, position:'static' });
        containerMenu.addItems(containerMenuItems);
        containerMenu.render(target);
    };

    launcher.init = function() {
        // populate or hide the command launcher based on what's in context
        XNAT.xhr.getJSON({
            url: rootUrl('/xapi/commands/available?project=' + projectId + '&xsiType=' + xsiType),
            success: function (data) {
                var availableCommands = data;
                if (!availableCommands.length) {
                    return false;
                } else {
                    var spawnedCommands = [];
                    availableCommands.forEach(function (command) {
                        launcher.addYUIMenuItem(command,spawnedCommands);
                    });
                    launcher.createYUIMenu('actionsMenu',spawnedCommands);
                }

            },
            fail: function (e) {
                errorHandler(e);
            }
        });

        // Special case: If this is a session, run a second context check for scans
        // only support scan-level actions if the new scan table is found. 
        if (XNAT.data.context.isImageSession && document.getElementById('selectable-table-scans')) {
            var xsiScanType = xsiType.replace('Session','Scan');

            XNAT.xhr.getJSON({
                url: rootUrl('/xapi/commands/available?project=' + projectId + '&xsiType=' + xsiScanType),
                success: function (data) {
                    var availableCommands = data;
                    if (!availableCommands.length) {
                        return false;
                    } else {
                        // build menu of commands
                        var spawnedCommands = [];
                        availableCommands.forEach(function (command) {
                            command.launcher = 'multiple-scans';
                            command.uri = '';
                            launcher.addMenuItem(command,spawnedCommands);
                        });

                        // add action menu to each scan listing
                        launcher.scanList = XNAT.data.context.scans || [];
                        launcher.scanList.forEach(function(scan){
                            var spawnedCommands = [];
                            availableCommands.forEach(function (command) {
                                command.launcher = 'single-scan';
                                command.uri = fullScanPath(scan['id']);
                                launcher.addMenuItem(command,spawnedCommands);
                            });

                            var scanActionTarget = $('tr#scan-'+scan['id']).find('.single-scan-actions-menu');
                            scanActionTarget.append(spawnedCommands).parents('td').find('.inline-actions-menu-toggle').removeClass('hidden');
                        });

                        // add commands to Bulk Run action menu at the top of the scan table
                        var menuTarget = $('#scanActionsMenu');
                        launcher.createMenu(menuTarget,spawnedCommands);
                        $('.scan-actions-controls').show();
                    }
                },
                fail: function(e) {
                    errorHandler(e);
                }
            });
        }
        
    };

    launcher.open = window.openCommandLauncher = function(obj){
        var launcher = obj.launcher,
            wrapperId = obj.wrapperid,
            rootElement = obj.rootElement,
            rootElementValue = obj.rootElementValue || undefined;

        switch(launcher) {
            case 'select-scan':
                XNAT.plugin.containerService.launcher.bulkSelectDialog(wrapperId,'scan');
                break;
            case 'single-scan':
                var rootElementPath = obj.uri;
                XNAT.plugin.containerService.launcher.singleScanDialog(wrapperId,rootElementPath);
                break;
            case 'multiple-scans':
                var listOfScanIds = [];
                $('.selectable').find('tbody').find('input:checked').each(function(){
                    var scanId = $(this).val();
                    var scanPath = fullScanPath(scanId);
                    listOfScanIds.push(scanPath);
                });
                XNAT.plugin.containerService.launcher.bulkLaunchDialog(wrapperId,'scan',listOfScanIds);
                break;
            default:
                XNAT.plugin.containerService.launcher.defaultLauncher(wrapperId,rootElement,rootElementValue);
        }
    };

    $(document).on('click','.commandLauncher',function(){
        var launcherObj = $(this).data();
        launcher.open(launcherObj);
    });

    $(document).on('click','.advancedSettingsToggle',function(){
        var advancedPanel = $(this).parent('.advancedSettings');
        advancedPanel.toggle();
    });

    launcher.refresh = function(){
        launcherMenu.html('');
        launcher.init();
    };

    $(document).ready(function(){
        launcher.init();
    });

}));