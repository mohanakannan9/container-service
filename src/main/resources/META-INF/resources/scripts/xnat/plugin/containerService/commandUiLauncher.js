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
            content: '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p><p>' + e.responseText + '</p>',
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

    /* Temporary Functions */
    launcher.getConfig = function(commandId,wrapperId) {

        var config = {
            inputs: {
                scan: {
                    description: "Input scan",
                    type: "scan",
                    value: null,
                    'user-settable': true,
                    advanced: false,
                    required: true
                },
                'scan-dicoms': {
                    description: "The dicom resource on the scan",
                    type: "hidden",
                    value: null,
                    'user-settable': false,
                    advanced: false,
                    required: false
                },
                bids: {
                    description: "Create BIDS metadata file",
                    type: "boolean",
                    value: "true",
                    'user-settable': true,
                    advanced: false
                },
                'other-options': {
                    description: "Other command-line flags to pass to dcm2niix",
                    type: "string",
                    value: null,
                    'user-settable': true,
                    advanced: true
                }
            }

        };
        return config;
    };

    /*
     ** Launcher Options
     */

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
                var independentInputs = {},
                    childInputs = {},
                    advancedInputs = {};

                var inputList = Object.keys(inputs);
                if ( inputList.find(function(input){ return input === 'scan'; }) ) { // if the specified root element matches an input parameter, we can proceed

                    // Need to break out input object to separate advanced and child inputs, and flatten each input object
                    // Rule: No advanced input can also be a child input

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
                                childInputs[key].value = inputs[i].ui[k].values[0].value;
                            }

                        } else if (inputs[i].advanced) {
                            advancedInputs[i] = inputs[i];
                            advancedInputs[i].type = inputs[i].ui.default.type;
                            advancedInputs[i].value = inputs[i].ui.default.values[0].value;
                        } else {
                            independentInputs[i] = inputs[i];
                            independentInputs[i].type = inputs[i].ui.default.type;
                            independentInputs[i].value = inputs[i].ui.default.values[0].value;
                        }
                    }


                    XNAT.ui.dialog.open({
                        title: 'Set Container Launch Values for Scan: '+rootElementPath,
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
                                        rootElement = 'scan',
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

                                    console.log(dataToPost);

                                    XNAT.xhr.postJSON({
                                        url: containerLaunchUrl(wrapperId),
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
                            },
                            {
                                label: 'Cancel',
                                isDefault: false,
                                close: true
                            }
                        ]
                    });

                }
            }
        });



    };

    launcher.bulkLaunchDialog = function(commandId,wrapperId,targets,rootElement){
        // 'targets' should be formatted as an array of XNAT data objects that a container will run on in series.
        // the 'root element' should match one of the inputs in the command config object, and overwrite it with the values provided in the 'targets' array

        var launcherConfig = launcher.getConfig(commandId,wrapperId),
            inputs = launcherConfig.inputs,
            normalInputs = {},
            advancedInputs = {};

        var inputList = Object.keys(inputs);
        if ( inputList.find(function(input){ return input === rootElement; }) ) { // if the specified root element matches an input parameter, we can proceed

            switch (rootElement) {
                case 'scan':
                    targets.forEach(function(target,i) {
                        targets[i] = fullScanPath(target);
                    });
                    break;

                default:
                    break;
            }

            XNAT.ui.dialog.open({
                title: 'Set Container Launch Values for '+targets.length+' scans',
                content: spawn('div.panel'),
                width: 550,
                scroll: true,
                nuke: true,
                footerContent: '<label class="disabled"><input type="checkbox" id="applyBulkSettings" checked="checked" disabled /> Apply settings to all selected items</label>',
                beforeShow: function(obj){
                    var $panel = obj.$modal.find('.panel');
                    var bulkSettings = obj.$modal.find('#applyBulkSettings');
                    if (bulkSettings.prop('checked')) {
                        $panel.spawn('p','Select settings to be applied to each item in your list.');

                        // overwrite defaults for root element input in config object. Display a list of targets with no input. We will break this out later.
                        inputs[rootElement].type="staticList";
                        inputs[rootElement].value=targets.join(',');

                        // separate normal inputs from advanced inputs
                        for (var i in inputs) {
                            if (inputs[i].advanced) {
                                advancedInputs[i] = inputs[i];
                            } else {
                                normalInputs[i] = inputs[i];
                            }
                        }

                        // add normal and advanced form inputs
                        $panel.append(launcher.formInputs(normalInputs));

                        if (Object.keys(advancedInputs).length > 0) {
                            $panel.append(launcher.formInputs(advancedInputs));
                        }
                    } else {
                        errorHandler({
                            statusText: 'Bulk Launch Dialog',
                            responseText: 'Method not yet supported'
                        });
                    }
                },
                buttons: [
                    {
                        label: 'Run Container(s)',
                        close: false,
                        isDefault: true,
                        action: function(obj){
                            xmodal.loading.open({ title: "Attempting to launch containers..." });

                            var $panel = obj.$modal.find('.panel'),
                                bulkSettings = obj.$modal.find('#applyBulkSettings'),
                                dataToPost = [];

                            if (bulkSettings.prop('checked')) {
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
                            } else {
                                return false; // for now, don't handle unique param settings for multiple objects.
                            }

                            console.log(JSON.stringify(dataToPost));

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
                        rootElementName: item['external-input-name'],
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
        containerMenuItems[0].submenu.itemdata.push({
            text: item['wrapper-description'],
            url: 'javascript:openCommandLauncher({ commandid:"'+item['command-id']+'", wrapperid:"'+item['wrapper-id']+'", wrappername:"'+item['wrapper-name']+'", launcher: "select-scan" })'
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
        if (XNAT.data.context.isImageSession) {
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
                            scanActionTarget.removeClass('hidden').append(spawnedCommands);
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
            commandId = obj.commandid,
            wrapperId = obj.wrapperid;


        switch(launcher) {
            case 'select-scan':
                XNAT.plugin.containerService.launcher.bulkSelectDialog(commandId,wrapperId,'scan');
                break;
            case 'single-scan':
                var rootElementPath = obj.uri;
                XNAT.plugin.containerService.launcher.singleScanDialog(wrapperId,rootElementPath);
                break;
            case 'multiple-scans':
                var listOfScans = [];
                $('.selectable').find('tbody').find('input:checked').each(function(){
                    listOfScans.push($(this).val());
                });
                XNAT.plugin.containerService.launcher.bulkLaunchDialog(commandId,wrapperId,listOfScans,'scan');
                break;
            default:
                console.log('Sorry, I don\'t know what to do.');
        }
    };

    $(document).on('click','.commandLauncher',function(){
        var launcherObj = $(this).data();
        launcher.open(launcherObj);
    });

    launcher.refresh = function(){
        launcherMenu.html('');
        launcher.init();
    };

    $(document).ready(function(){
        launcher.init();
    });

}));