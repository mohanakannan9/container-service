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
                    advanced: false
                }
            },
            outputs: {
                "noti-resource": {
                    "type": "hidden",
                    "label": "NIFTI"
                }
            }

        };

        return config;
    };

    /*
     * Panel form elements for launcher
     */

    function scanSelector(scanArray){
        // build a scan object from the array of scans
        var scanObj = {};
        scanArray.forEach(function(item){
            var scanPath = fullScanPath(item.id);
            scanObj[scanPath] = item.id + ' - ' +item.series_description;
        });
        var optionsObj = {
            label: 'Scan',
            options: scanObj,
            description: 'Required',
            name: 'scan'
        };
        return XNAT.ui.panel.select.menu(optionsObj).element;
    }

    launcher.formInputs = function(config) {
        var formPanelElements = [];

        function basicConfigInput(name,value,required) {
            value = (value === undefined || value === null || value == 'null') ? '' : value;
            var description = (required) ? 'Required' : '';
            return XNAT.ui.panel.input.text({
                name: name,
                value: value,
                description: description,
                label: name
            }).element;
        }

        function configCheckbox(name,value,outerLabel,innerLabel,condensed){

            var vertSpace = function(condensed) {
                return (condensed) ? '' : spawn('br.clear');
            };

            return spawn('div.panel-element', { data: { name: name }}, [
                spawn('label.element-label', outerLabel),
                spawn('div.element-wrapper', [
                    spawn('label', [
                        spawn('input', { type: 'checkbox', name: name, value: value }),
                        innerLabel
                    ])
                ]),
                vertSpace(condensed)
            ]);
        }

        function booleanCheckbox(name,checked,description,onText,offText){
            checked = (checked === 'true') ? ' checked' : '';
            description = description || name;

            return '<div class="panel-element" data-name="'+name+'"><label class="element-label" for="'+name+'">'+name+'</label><div class="element-wrapper"><label><input type="checkbox" value="true" name="'+name+'" '+ checked +' />'+description+'</label></div><br class="clear" /></div>';
        }

        function hiddenConfigInput(name,value) {
            return XNAT.ui.input.hidden({
                name: name,
                value: value
            }).element;
        }

        function staticConfigInput(name,value,text) {
            return spawn(
                'div.panel-element', { data: { name: name } }, [
                    spawn('label.element-label', name),
                    spawn('div.element-wrapper', text),
                    XNAT.ui.panel.input.hidden({
                        name: name,
                        value: value
                    }).element,
                    spawn('br.clear')
                ]
            );
        }

        function staticConfigList(name,list) {
            var listArray = list.split(',');
            listArray.forEach(function(item,i){
                listArray[i] = '<li>'+item+'</li>'
            });
            return spawn(
                'div.panel-element', { data: { name: name } }, [
                    spawn('label.element-label', name),
                    spawn('div.element-wrapper', [
                        spawn('ul',{ style: {
                            'list-style-type': 'none',
                            margin: 0,
                            padding: 0
                        }},listArray.join(''))
                    ]),
                    spawn('br.clear')
                ]
            )
        }

        // determine which type of table to build.
        if (config.type === 'inputs') {
            var inputs = config.inputs;

            for (var i in inputs) {
                var input = inputs[i];
                // create a panel.input for each input type
                switch (input.type){
                    case 'scanSelectOne':
                        formPanelElements.push(scanSelector(launcher.scanList, input.value));
                        break;
                    case 'scanSelectMany':
                        launcher.scanList.forEach(function(scan,i){
                            scan.url = fullScanPath(scan.id);
                            scan.label = scan.id + ' - ' + scan.series_description;
                            if (i === 0) {
                                formPanelElements.push(configCheckbox('scan', scan.url, 'scans', scan.label, 'condensed'));
                            } else if (i < launcher.scanList.length-1){
                                formPanelElements.push(configCheckbox('scan', scan.url, '', scan.label, 'condensed'));
                            } else {
                                formPanelElements.push(configCheckbox('scan', scan.url, '', scan.label ));
                            }
                        });
                        break;
                    case 'hidden':
                        formPanelElements.push(hiddenConfigInput(i,input.value));
                        break;
                    case 'static':
                        formPanelElements.push(staticConfigInput(i,input.value,input.value));
                        break;
                    case 'staticList':
                        formPanelElements.push(staticConfigList(i,input.value));
                        break;
                    case 'boolean':
                        if (input['user-settable']) {
                            formPanelElements.push(booleanCheckbox(i,input.value,input.description));
                        } else {
                            formPanelElements.push(hiddenConfigInput(i,input.value));
                        }
                        break;
                    default:
                        if (input['user-settable']) {
                            formPanelElements.push(basicConfigInput(i,input.value,input.required));
                        } else {
                            formPanelElements.push(hiddenConfigInput(i,input.value));
                        }
                }
            }

        } else if (config.type === 'outputs') {
            var outputs = config.outputs;

            for (var o in outputs) {
                var output = outputs[o];
                formPanelElements.push(hiddenConfigInput(o,output.value));
            }

        }

        // return spawned panel.
        return formPanelElements;
    };



    /*
     ** Launcher Options
     */

    launcher.bulkSelectDialog = function(commandId,wrapperId,rootElement){
        // get command definition
        var launcherConfig = launcher.getConfig(commandId,wrapperId),
            inputs = launcherConfig.inputs,
            outputs = launcherConfig.outputs;

        var inputList = Object.keys(inputs);
        if ( inputList.find(function(input){ return input === rootElement; }) ) { // if the specified root element matches an input parameter, we can proceed

            inputs[rootElement].type = 'scanSelectMany';

            // open the template form in a modal
            XNAT.ui.dialog.open({
                title: 'Set Config Values',
                content: '<div class="panel"></div>',
                width: 550,
                scroll: true,
                nuke: true,
                beforeShow: function (obj) {
                    var $panel = obj.$modal.find('.panel');
                    $panel.spawn('p', 'Please specify settings for this container.');
                    // place input and output form elements into the template
                    $panel.append(launcher.formInputs({type: 'inputs', inputs: inputs}));
                    $panel.append(launcher.formInputs({type: 'outputs', outputs: outputs}));
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

    launcher.singleScanDialog = function(commandId,wrapperId,target,rootElement){
        if (!target) return false;

        var launcherConfig = launcher.getConfig(commandId,wrapperId);

        var inputs = launcherConfig.inputs;
        var outputs = launcherConfig.outputs;

        var inputList = Object.keys(inputs);
        if ( inputList.find(function(input){ return input === rootElement; }) ) { // if the specified root element matches an input parameter, we can proceed

            XNAT.ui.dialog.open({
                title: 'Set Container Launch Values for Scan: '+target,
                content: '<div class="panel"></div>',
                width: 550,
                scroll: true,
                beforeShow: function(obj){
                    var $panel = obj.$modal.find('.panel');
                    $panel.spawn('p','Please specify settings for this container.');

                    // overwrite defaults for root element input in config object
                    inputs[rootElement].type="static";
                    inputs[rootElement].value=target;

                    $panel.append(launcher.formInputs({ type: 'inputs', inputs: inputs }));
                    $panel.append(launcher.formInputs({ type: 'outputs', outputs: outputs }));

                }
            });

        }

    };

    launcher.bulkLaunchDialog = function(commandId,wrapperId,targets,rootElement){
        // 'targets' should be formatted as an array of XNAT data objects that a container will run on in series.
        // the 'root element' should match one of the inputs in the command config object, and overwrite it with the values provided in the 'targets' array

        var launcherConfig = launcher.getConfig(commandId,wrapperId),
            inputs = launcherConfig.inputs,
            outputs = launcherConfig.outputs;

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
                content: '<div class="panel"></div>',
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

                        $panel.append(launcher.formInputs({ type: 'inputs', inputs: inputs }));
                        $panel.append(launcher.formInputs({ type: 'outputs', outputs: outputs }));
                    } else {
                        xmodal.alert('What, are you trying to kill me?');
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
                        commandid: item['command-id'],
                        wrapperid: item['wrapper-id'],
                        wrappername: item['wrapper-name'],
                        launcher: item['launcher']
                    }
                })
            ]));
        return itemSet;
    };

    launcher.createMenu = function(target,itemSet){
        var containerMenu = spawn('li.has-submenu',[
            spawn(['a',{ href: '#!', html: 'Run' }]),
            spawn(['ul.dropdown-submenu', itemSet ])
        ]);


        target.append(containerMenu);
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

        // if this is a session, run a second context check for scans
        if (XNAT.data.context.isImageSession) {
            var xsiScanType = xsiType.replace('Session','Scan');
            XNAT.xhr.getJSON({
                url: rootUrl('/xapi/commands/available?project=' + projectId + '&xsiType=' + xsiScanType),
                success: function (data) {
                    var availableCommands = data;
                    if (!availableCommands.length) {
                        return false;
                    } else {
                        var spawnedCommands = [];
                        availableCommands.forEach(function (command) {
                            command.launcher = 'multiple-scans';
                            launcher.addMenuItem(command,spawnedCommands);
                        });
                        menuTarget = $('#scanActionsMenu');
                        launcher.createMenu(menuTarget,spawnedCommands);
                        $('.scan-actions-controls').show();
                    }
                },
                fail: function(e) {
                    errorHandler(e);
                }
            });

            // also, generate a list of scans
            launcher.scanList = [];
            XNAT.xhr.getJSON({
                url: sessionUrl(),
                success: function (data) {
                    var scans = [], children = data.items[0].children;
                    children.forEach(function(child){
                        if (child.field === "scans/scan") scans = child.items;
                    });
                    scans.forEach(function(scan){
                        launcher.scanList.push({
                            id: scan.data_fields.ID,
                            series_description: scan.data_fields.series_description
                        });
                    });
                }
            });
        }
        
    };

    launcher.open = window.openCommandLauncher = function(obj){
        var launcher = obj.launcher,
            commandId = obj.commandid,
            wrapperId = obj.wrapperid,
            wrapperName = obj.wrappername;


        switch(launcher) {
            case 'select-scan':
                XNAT.plugin.containerService.launcher.bulkSelectDialog(commandId,wrapperId,'scan');
                break;
            case 'single-scan':
                XNAT.plugin.containerService.launcher.singleScanDialog(commandId,wrapperName,scanId);
                break;
            case 'multiple-scans':
                var listOfScans = [];
                $('.selectable').find('tbody').find('input:checked').each(function(){
                    listOfScans.push($(this).val());
                });
                XNAT.plugin.containerService.launcher.bulkLaunchDialog(commandId,wrapperId,listOfScans,'scan');
                break;
            default:
                xmodal.alert('Sorry, I don\'t know what to do.');
        }
    };

    $(document).on('click','.commandLauncher',function(){
        var launcherObj = {};
        launcherObj.commandid = $(this).data('commandid');
        launcherObj.wrapperid = $(this).data('wrapperid');
        launcherObj.wrappername = $(this).data('wrappername');
        launcherObj.launcher = $(this).data('launcher');

        launcher.open(launcherObj);
    });

    launcher.refresh = function(){
        launcherMenu.html('');
        launcher.init();
    };

    launcher.init();

}));