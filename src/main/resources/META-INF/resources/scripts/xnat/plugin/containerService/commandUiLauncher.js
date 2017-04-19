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
        xsiType = XNAT.data.context.xsiType,
        sessionId = (XNAT.data.context.isImageSession) ? XNAT.data.context.ID : null;

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

    function scanSelector(scanArray){
        // build a scan object from the array of scans
        var scanObj = {};
        scanArray.forEach(function(item){
            scanObj['/experiments/'+sessionId+'/scans/'+item.id] = item.id + ' - ' +item.series_description;
        });
        var optionsObj = {
            label: 'Scan',
            options: scanObj,
            description: 'Required',
            name: 'scan'
        };
        return XNAT.ui.panel.select.menu(optionsObj).element;
    }

    launcher.getConfig = function(commandId,wrapperName,callback) {

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
                    value: "false",
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

        function configCheckbox(name,checked,description,onText,offText){
            checked = (checked === 'true') ? ' checked' : '';
            description = description || name;
            onText = onText || 'Yes';
            offText = offText || 'No';

            /*
            var enabled = !!checked;
            var ckbox = spawn('input.config-enabled', {
                type: 'checkbox',
                checked: enabled,
                value: 'true',
                data: { name: name, checked: enabled },
            });
            return XNAT.ui.panel.input.switchbox({
                name: name,
                label: name,
                value: 'true',
                checked: checked,
                onText: onText,
                offText: offText
            });
            */
            return '<div class="panel-element" data-name="'+name+'"><label class="element-label" for="'+name+'">'+name+'</label><div class="element-wrapper"><label><input type="checkbox" value="true" name="'+name+'" '+ checked +' />'+description+'</label></div><br class="clear" /></div>';
        }

        function hiddenConfigInput(name,value) {
            return XNAT.ui.input.hidden({
                name: name,
                value: value
            }).element;
        }


        // determine which type of table to build.
        if (config.type === 'inputs') {
            var inputs = config.inputs;

            for (var i in inputs) {
                var input = inputs[i];
                // create a panel.input for each input type
                switch (input.type){
                    case 'scan':
                        if (launcher.scanList.length > 0 && input['user-settable'] && !input.value) {
                            formPanelElements.push(scanSelector(launcher.scanList, input.value))
                        }
                        break;
                    case 'hidden':
                        formPanelElements.push(hiddenConfigInput(i,input.value));
                        break;
                    case 'boolean':
                        if (input['user-settable']) {
                            formPanelElements.push(configCheckbox(i,input.value,input.description))
                        } else {
                            formPanelElements.push(hiddenConfigInput(i,input.value));
                        }
                        break;
                    default:
                        if (input['user-settable']) {
                            formPanelElements.push(basicConfigInput(i,input.value,input.required))
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


    launcher.dialog = function(commandId,wrapperName,wrapperId,scanId){
        // get command definition
        var launcherConfig = launcher.getConfig(commandId,wrapperName);

        var tmpl = $('div#proj-command-config-template');
        var tmplBody = $(tmpl).find('.panel-body').html('');

        var inputs = launcherConfig.inputs;
        var outputs = launcherConfig.outputs;

        tmplBody.spawn('p','Please specify settings for this container.');
        XNAT.ui.panel
            .init({
                header: false,
                footer: false,
                body: launcher.formInputs({ type: 'inputs', inputs: inputs })
            })
            .render(tmplBody);

        XNAT.ui.panel
            .init({
                header: false,
                footer: false,
                body: launcher.formInputs({ type: 'outputs', outputs: outputs})
            })
            .render(tmplBody);

        xmodal.open({
            title: 'Set Config Values',
            template: tmpl.clone(),
            width: 550,
            height: 350,
            scroll: true,
            beforeShow: function (obj) {
                var $panel = obj.$modal.find('#proj-config-viewer-panel');
                $panel.find('input[type=checkbox]').each(function () {
                    $(this).prop('checked', $(this).data('checked'));
                })
            },
            okClose: false,
            okLabel: 'Run Container',
            okAction: function(obj){

                console.log(commandId,wrapperId);
                var $panel = obj.$modal.find('#proj-config-viewer-panel');
                var dataToPost = {};
                $panel.find('input[type=text]').not('[type=checkbox]').not('[type=radio]').each(function(){
                    // get the name and value from each text element and add it to our data to post
                    var key = $(this).prop('name');
                    dataToPost[key] = $(this).val();
                });

                $panel.find('input[type=checkbox]').each(function(){
                    var key = $(this).prop('name');
                    var val = ($(this).is(':checked')) ? $(this).val() : false;
                    dataToPost[key] = val;
                });

                $panel.find('select').each(function(){
                    var key = $(this).prop('name');
                    var val = $(this).find('option:selected').val();
                    dataToPost[key] = val;
                });

                console.log(dataToPost);

                XNAT.xhr.postJSON({
                    url: '/xapi/commands/'+commandId+'/wrappers/'+wrapperId+'/launch',
                    data: JSON.stringify(dataToPost),
                    success: function(containerId){
                        console.log(containerId, 'launched');
                        xmodal.alert({
                            title: 'Container Launch Success',
                            content: '<p>Successfully launched container with ID '+containerId+'.</p>',
                            okAction: function(){
                                xmodal.closeAll();
                            }
                        })
                    },
                    fail: function(e){
                        errorHandler(e);
                    }
                })
            }
        });

    };

    /*
    ** Launcher Options
     */

    launcher.openScanSelectModal = function(commandId,wrapperId){
        // if we need to select a scan, we need scan information.
        // first make sure we are in the right context
        console.log(commandId,wrapperId);

        if (!XNAT.data.context.isImageSession) {
            xmodal.alert('Sorry, something went wrong and I cannot launch.');
            return false;
        }

        XNAT.xhr.getJSON({
            url: rootUrl('/data/experiments/'+sessionId),
            success: function(data,commandId,wrapperId){
                var scans = data.items[0].children[0].items;
                var tmpl = $('#proj-command-config-template');
                var tmplBody = tmpl.find('.panel-body').html('');
                var selector = scanSelector(scans);
                tmplBody.append( selector );

                console.log(commandId,wrapperId);

                // open quick selector
                xmodal.open({
                    title: 'Select Scan to Run Command On',
                    height: 200,
                    width: 450,
                    template: tmpl.clone(),
                    beforeShow: function(obj){
                        var $panel = obj.$modal.find('#proj-config-viewer-panel');
                        $panel.find('select[name=scan]').prepend('<option value selected>Select Scan</option>');
                    },
                    okClose: false,
                    okAction: function(obj,commandId,wrapperId){

                        console.log(commandId,wrapperId);
                        var $panel = obj.$modal.find('#proj-config-viewer-panel');
                        var scan = $panel.find('select[name=scan]').find('option:selected').val();
                        var scanPath = '/experiments/'+sessionId+'/scans/'+scan;
                        var dataToPost = { scan: scanPath };
                        XNAT.xhr.postJSON({
                            url: '/xapi/commands/'+commandId+'/wrappers/'+wrapperId+'/launch',
                            data: JSON.stringify(dataToPost),
                            success: function(containerId){
                                console.log(containerId);
                            },
                            fail: function(e){
                                errorHandler(e);
                            }
                        })
                    }
                });
            },
            fail: function(e){
                errorHandler(e);
            }
        })

    };

    // cheat the logic and get a list of scans for this session
    launcher.scanList = [];
    if (XNAT.data.context.isImageSession === true) {
        XNAT.xhr.getJSON({
            url: rootUrl('/data/experiments/' + sessionId),
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

    launcher.addMenuItem = function(item){
        containerMenuItems[0].submenu.itemdata.push({
            text: item['wrapper-description'],
            url: 'javascript:openCommandLauncher({ commandid:"'+item['command-id']+'", wrapperid:"'+item['wrapper-id']+'", wrappername:"'+item['wrapper-name']+'", launcher: "select-scan" })'
        });
    };

    launcher.createMenu = function(target){
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
                    availableCommands.forEach(function (command) {
                        launcher.addMenuItem(command);
                    });
                    launcher.createMenu();
                }

            },
            fail: function (e) {
                errorHandler(e);
            }
        });
    };

    launcher.open = window.openCommandLauncher = function(obj){
        var launcher = obj.launcher,
            commandId = obj.commandid,
            wrapperId = obj.wrapperid,
            wrapperName = obj.wrappername;


        switch(launcher) {
            case 'select-scan':
                XNAT.plugin.containerService.launcher.dialog(commandId,wrapperName,wrapperId);
                break;
            default:
                xmodal.alert('Sorry, I don\'t know what to do.');
        }
    };

    launcher.refresh = function(){
        launcherMenu.html('');
        launcher.init();
    };

    launcher.init();


}));