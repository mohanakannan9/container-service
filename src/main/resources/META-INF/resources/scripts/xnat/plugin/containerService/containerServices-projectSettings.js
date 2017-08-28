/*
 * web: containerServices-projectSettings.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Manage Site-wide Command Configs
 */

console.log('containerServices-projectSettings.js');

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

    var projCommandConfigManager,
        projConfigDefinition,
        undefined,
        rootUrl = XNAT.url.rootUrl,
        csrfUrl = XNAT.url.csrfUrl;
    
    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.containerService = 
        getObject(XNAT.plugin.containerService || {});

    XNAT.plugin.containerService.projCommandConfigManager = projCommandConfigManager =
        getObject(XNAT.plugin.containerService.projCommandConfigManager || {});

    XNAT.plugin.containerService.projConfigDefinition = projConfigDefinition =
        getObject(XNAT.plugin.containerService.projConfigDefinition || {});


    function spacer(width){
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        });
    }

    function errorHandler(e, title){
        console.log(e);
        title = (title) ? 'Error Found: '+ title : 'Error';
        var errormsg = (e.statusText) ? '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p><p>' + e.responseText + '</p>' : e;
        xmodal.alert({
            title: title,
            content: errormsg,
            okAction: function () {
                xmodal.closeAll();
            }
        });
    }

    function getUrlParams(){
        var paramObj = {};

        // get the querystring param, redacting the '?', then convert to an array separating on '&'
        var urlParams = window.location.search.substr(1,window.location.search.length);
        urlParams = urlParams.split('&');

        urlParams.forEach(function(param){
            // iterate over every key=value pair, and add to the param object
            param = param.split('=');
            paramObj[param[0]] = param[1];
        });

        return paramObj;
    }

    function getProjectId(){
        return getUrlParams().id;
    }

    function commandUrl(appended){
        appended = isDefined(appended) ? appended : '';
        return rootUrl('/xapi/commands' + appended);
    }

    function configUrl(commandId,wrapperName,appended){
        appended = isDefined(appended) ? '?' + appended : '';
        if (!commandId || !wrapperName) return false;
        return csrfUrl('/xapi/projects/'+getProjectId()+'/commands/'+commandId+'/wrappers/'+wrapperName+'/config' + appended);
    }

    function sitewideConfigEnableUrl(commandObj,wrapperObj,flag){
        var command = commandObj.id,
            wrapperName = wrapperObj.name;
        return csrfUrl('/xapi/commands/'+command+'/wrappers/'+wrapperName+'/' + flag);
    }

    function projConfigEnableUrl(commandId,wrapperName,flag){
        if (!commandId || !wrapperName || !flag) return false;
        var projectId = getProjectId();
        return csrfUrl('/xapi/projects/'+projectId+'/commands/'+commandId+'/wrappers/'+wrapperName+'/' + flag);
    }

    function projectPrefUrl(action){
        var projectId = getProjectId();
        action = action||''; 
        var flag = (action.toUpperCase() === 'PUT') ? '?inbody=true' : '';
        return csrfUrl('/data/projects/'+projectId+'/config/container-service/general' + flag);
    }

    projCommandConfigManager.getCommands = projCommandConfigManager.getAll = function(callback){

        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: commandUrl(),
            dataType: 'json',
            success: function(data){
                if (data) {
                    return data;
                }
                callback.apply(this, arguments);
            }
        });
    };

    projConfigDefinition.getConfig = function(commandId,wrapperName,callback){
        if (!commandId || !wrapperName) return false;
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: configUrl(commandId,wrapperName),
            dataType: 'json',
            success: function(data){
                if (data) {
                    return data;
                }
                callback.apply(this, arguments);
            }
        });
    };

    projCommandConfigManager.getEnabledStatus = function(command,wrapper,callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: projConfigEnableUrl(command.id,wrapper.name,'enabled'),
            success: function(data){
                if (data) {
                    return data;
                }
                callback.apply(this, arguments);
            },
            fail: function(e){
                errorHandler(e);
            }
        });
    };

    // auto-save to project config on click of the opt-in switchbox.
    $('#optIntoSitewideCommands').on('change',function(){
        var optIn = $(this).prop('checked');
        var paramToPut = JSON.stringify({ optIntoSitewideCommands: optIn });
        XNAT.xhr.putJSON({
            url: projectPrefUrl('PUT'),
            data: paramToPut,
            dataType: 'json',
            success: function(){
                XNAT.ui.banner.top(1000, 'Site-wide Command Opt-in Setting set to <b>' + optIn + '</b>.', 'success');
                if (optIn) projCommandConfigManager.importSiteWideEnabledStatus();
            },
            fail: function(e){
                xmodal.alert({title: 'Error', content: e.statusText });
            }
        });
    });


    projConfigDefinition.table = function(config) {

        // initialize the table - we'll add to it below
        var pcdTable = XNAT.table({
            className: 'command-config-definition xnat-table '+config.type,
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        function basicConfigInput(name,value) {
            value = (value === undefined || value === null || value == 'null') ? '' : value;
            return '<input type="text" name="'+name+'" value="'+value+'" />';
        }

        function configCheckbox(name,checked,onText,offText){
            onText = onText || 'Yes';
            offText = offText || 'No';
            var enabled = !!checked;
            var ckbox = spawn('input.config-enabled', {
                type: 'checkbox',
                checked: enabled,
                value: 'true',
                data: { name: name, checked: enabled },
            });

            return spawn('div.left', [
                spawn('label.switchbox', [
                    ckbox,
                    ['span.switchbox-outer', [['span.switchbox-inner']]],
                    ['span.switchbox-on',[onText]],
                    ['span.switchbox-off',[offText]]
                ])
            ]);
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

            // add table header row
            pcdTable.tr()
                .th({ addClass: 'left', html: '<b>Input</b>' })
                .th('<b>Default Value</b>')
                .th('<b>Matcher Value</b>')
                .th('<b>User-Settable?</b>')
                .th('<b>Advanced?</b>');

            for (i in inputs) {
                var input = inputs[i];
                pcdTable.tr({ data: { input: i }, className: 'input' })
                    .td( { data: { key: 'key' }, addClass: 'left'}, i )
                    .td( { data: { key: 'property', property: 'default-value' }}, basicConfigInput('defaultVal',input['default-value']) )
                    .td( { data: { key: 'property', property: 'matcher' }}, basicConfigInput('matcher',input['matcher']) )
                    .td( { data: { key: 'property', property: 'user-settable' }}, [['div', [configCheckbox('userSettable',input['user-settable']) ]]])
                    .td( { data: { key: 'property', property: 'advanced' }}, [['div', [configCheckbox('advanced',input['advanced']) ]]]);
            }

        } else if (config.type === 'outputs') {
            var outputs = config.outputs;

            // add table header row
            pcdTable.tr()
                .th({ addClass: 'left', html: '<b>Output</b>' })
                .th({ addClass: 'left', width: '75%', html: '<b>Label</b>' });

            for (o in outputs) {
                var output = outputs[o];
                pcdTable.tr({ data: { output: o }, className: 'output' })
                    .td( { data: { key: 'key' }, addClass: 'left'}, o )
                    .td( { data: { key: 'property', property: 'label' }}, basicConfigInput('label',output['label']) );
            }

        }

        projConfigDefinition.$table = $(pcdTable.table);

        return pcdTable.table;
    };

    projConfigDefinition.dialog = function(commandId,wrapperName){
        // get command definition
        projConfigDefinition.getConfig(commandId,wrapperName)
            .success(function(data){
                var tmpl = $('div#proj-command-config-template');
                var tmplBody = $(tmpl).find('.panel-body').html('');

                var inputs = data.inputs;
                var outputs = data.outputs;

                tmplBody.spawn('h3','Inputs');
                tmplBody.append(projConfigDefinition.table({ type: 'inputs', inputs: inputs }));

                if (outputs.length) {
                    tmplBody.spawn('h3','Outputs');
                    tmplBody.append(projConfigDefinition.table({ type: 'outputs', outputs: outputs }));
                }

                xmodal.open({
                    title: 'Set Config Values',
                    template: tmpl.clone(),
                    width: 850,
                    height: 500,
                    scroll: true,
                    beforeShow: function(obj){
                        var $panel = obj.$modal.find('#proj-config-viewer-panel');
                        $panel.find('input[type=checkbox]').each(function(){
                            $(this).prop('checked',$(this).data('checked'));
                        })
                    },
                    okClose: false,
                    okLabel: 'Save',
                    okAction: function(obj){
                        var $panel = obj.$modal.find('#proj-config-viewer-panel');
                        var configObj = { inputs: {}, outputs: {} };

                        // gather input items from table
                        var inputRows = $panel.find('table.inputs').find('tr.input');
                        $(inputRows).each(function(){
                            var row = $(this);
                            // each row contains multiple cells, each of which defines a property.
                            var key = $(row).find("[data-key='key']").html();
                            configObj.inputs[key] = {};

                            $(row).find("[data-key='property']").each(function(){
                                var propKey = $(this).data('property');
                                var formInput = $(this).find('input');
                                if ($(formInput).is('input[type=checkbox]')) {
                                    var checkboxVal = ($(formInput).is(':checked')) ? $(formInput).val() : 'false';
                                    configObj.inputs[key][propKey] = checkboxVal;
                                } else {
                                    configObj.inputs[key][propKey] = $(this).find('input').val();
                                }
                            });

                        });

                        // gather output items from table
                        var outputRows = $panel.find('table.outputs').find('tr.output');
                        $(outputRows).each(function(){
                            var row = $(this);
                            // each row contains multiple cells, each of which defines a property.
                            var key = $(row).find("[data-key='key']").html();
                            configObj.outputs[key] = {};

                            $(row).find("[data-key='property']").each(function(){
                                var propKey = $(this).data('property');
                                configObj.outputs[key][propKey] = $(this).find('input').val();
                            });

                        });

                        // POST the updated command config
                        XNAT.xhr.postJSON({
                            url: configUrl(commandId,wrapperName,'enabled=true'),
                            dataType: 'json',
                            data: JSON.stringify(configObj),
                            success: function() {
                                XNAT.ui.banner.top(1000, '<b>"' + wrapperName + '"</b> updated.', 'success');
                                xmodal.closeAll();
                            },
                            fail: function(e) {
                                xmodal.alert({
                                    title: 'Error',
                                    content: '<p><strong>Error '+e.status+'</strong></p><p>'+e.statusText+'</p>',
                                    okAction: function(){
                                        xmodal.closeAll();
                                    }
                                });
                            }
                        })
                    }
                });

            })
            .fail(function(e){
                errorHandler(e);
            });

    };

    projCommandConfigManager.table = function(config){

        // initialize the table - we'll add to it below
        var pccmTable = XNAT.table({
            className: 'xnat-table '+config.className,
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            },
            id: config.id
        });

        // add table header row
        pccmTable.tr()
            .th({ addClass: 'left', html: '<b>XNAT Command Label</b>' })
            .th('<b>Container</b>')
            .th('<b>Enabled</b>')
            .th('<b>Actions</b>');

        // add master switch
        pccmTable.tr({ 'style': { 'background-color': '#f3f3f3' }})
            .td( {className: 'name', colSpan: 2 }, 'Enable / Disable All Commands' )
            .td([ spawn('div',[ masterCommandCheckbox() ]) ])
            .td();

        function viewLink(command, wrapper, text){
            return spawn(
                'a.link|href=#!',
                {
                    onclick: function(e){
                        e.preventDefault();
                        projConfigDefinition.dialog(command.id, wrapper.name, false);
                    }
                },
                spawn('b', text)
            );
        }

        function editConfigButton(command,wrapper){
            return spawn('button.btn.sm', {
                onclick: function(e){
                    e.preventDefault();
                    projConfigDefinition.dialog(command.id, wrapper.name, false);
                }
            }, 'Set Defaults');
        }

        function enabledCheckbox(command,wrapper){
            projCommandConfigManager.getEnabledStatus(command,wrapper).done(function(data){
                var enabled = data['enabled-for-site'] && data['enabled-for-project'];
                $('#wrapper-'+wrapper.id+'-enable').prop('checked',enabled);

                if (data['enabled-for-site'] === false) {
                    // if a command has been disabled at the site-wide level, don't allow user to toggle it.
                    // disable the input, and add a 'disabled' class to the input controller
                    // or, remove the input and controller entirely.  
                    $('#wrapper-'+wrapper.id+'-enable').prop('disabled','disabled')
                        .parents('.switchbox').addClass('disabled').hide()
                        .parent('div').html(spawn('span',{ 'style': { 'color': '#808080' }},'disabled'));
                }
                projCommandConfigManager.setMasterEnableSwitch();
            });

            var ckbox = spawn('input.config-enabled.wrapper-enable', {
                type: 'checkbox',
                checked: false,
                value: 'true',
                id: 'wrapper-'+wrapper.id+'-enable',
                data: { wrappername: wrapper.name, commandid: command.id },
                onchange: function(){
                    // save the status when clicked
                    var checkbox = this;
                    var enabled = checkbox.checked;
                    var enabledFlag = (enabled) ? 'enabled' : 'disabled';

                    XNAT.xhr.put({
                        url: projConfigEnableUrl(command.id,wrapper.name,enabledFlag),
                        success: function(){
                            var status = (enabled ? ' enabled' : ' disabled');
                            checkbox.value = enabled;
                            XNAT.ui.banner.top(1000, '<b>' + wrapper.name+ '</b> ' + status, 'success');
                        }
                    });

                    projCommandConfigManager.setMasterEnableSwitch();
                }
            });

            return spawn('div.center', [
                spawn('label.switchbox|title=' + wrapper.name, [
                    ckbox,
                    ['span.switchbox-outer', [['span.switchbox-inner']]]
                ])
            ]);
        }

        function masterCommandCheckbox(){

            var ckbox = spawn('input.config-enabled', {
                type: 'checkbox',
                checked: false,
                value: 'true',
                id: 'wrapper-all-enable',
                onchange: function(){
                    // save the status when clicked
                    var checkbox = this;
                    enabled = checkbox.checked;
                    var enabledFlag = (enabled) ? 'enabled' : 'disabled';

                    // iterate through each command toggle and set it to 'enabled' or 'disabled' depending on the user's click
                    $('.wrapper-enable').each(function(){
                        var status = ($(this).is(':checked')) ? 'enabled' : 'disabled';
                        if (status !== enabledFlag) $(this).click();
                    });
                    XNAT.ui.banner.top(2000, 'All commands <b>'+enabledFlag+'</b>.', 'success');
                }
            });

            return spawn('div.center', [
                spawn('label.switchbox|title=enable-all', [
                    ckbox,
                    ['span.switchbox-outer', [['span.switchbox-inner']]]
                ])
            ]);
        }

        projCommandConfigManager.getAll().done(function(data) {
            if (data) {
                for (var i = 0, j = data.length; i < j; i++) {
                    var command = data[i];
                    if (command.xnat) { // if an xnat wrapper has been defined for this command...
                        for (var k = 0, l = command.xnat.length; k < l; k++) {
                            var wrapper = command.xnat[k];

                            pccmTable.tr({title: wrapper.name, data: {id: wrapper.id, name: wrapper.name, image: command.image}})
                                .td([viewLink(command, wrapper, wrapper.description)]).addClass('name')
                                .td(command.image)
                                .td([['div.center', [enabledCheckbox(command,wrapper)]]])
                                .td([['div.center', [editConfigButton(command,wrapper)]]]);
                        }
                    }

                }
            } else {
                // create a handler when no command data is returned.
                pccmTable.tr({title: 'No command config data found'})
                    .td({colSpan: '5', html: 'No XNAT-enabled Commands Found'});
            }
        });

        projCommandConfigManager.$table = $(pccmTable.table);

        return pccmTable.table;
    };

    // examine all command toggles and set master switch to "ON" if all are checked
    projCommandConfigManager.setMasterEnableSwitch = function(){
        var allEnabled = true;
        $('.wrapper-enable').each(function(){
            if (!$(this).is(':checked')) {
                allEnabled = false;
                return false;
            }
        });

        if (allEnabled) {
            $('#wrapper-all-enable').prop('checked','checked');
        } else {
            $('#wrapper-all-enable').prop('checked',false);
        }
    };

    projCommandConfigManager.importSiteWideEnabledStatus = function(){
        $('.wrapper-enable').each(function(){
            // check current status and site-wide setting, then reconcile differences.
            // For now, do not disable a command in the project if it is not enabled in the site

            var $toggle = $(this),
                projStatus = ($toggle.is(':checked')) ? 'enabled' : 'disabled';
            XNAT.xhr.getJSON({
                url: sitewideConfigEnableUrl({ id: $toggle.data('commandid')} , { name: $toggle.data('wrappername') }, 'enabled'),
                success: function(status){
                    if (projStatus === 'disabled' && status) {
                        $toggle.click();
                        projCommandConfigManager.setMasterEnableSwitch();
                    }
                },
                fail: function(e){
                    errorHandler(e, 'Could not import site-wide setting for '+$(this).data('wrappername'));
                }
            })
        });
    };

    projCommandConfigManager.init = function(container){
        var $manager = $$(container||'div#proj-command-config-list-container');

        projCommandConfigManager.container = $manager;

        $manager.append(projCommandConfigManager.table({id: 'sitewide-commands', className: '', type: 'sitewide' }));
        $manager.append(projCommandConfigManager.table({id: 'project-commands', className: 'hidden', type: 'project' }));

        // set value of opt-in controller based on project config
        $('#optIntoSitewideCommands').prop('checked',false);

        XNAT.xhr.getJSON({
            url: projectPrefUrl(),
            success: function(data){
                var configParams = JSON.parse(data.ResultSet.Result[0].contents);
                if (configParams.optIntoSitewideCommands === true) {
                    $('#optIntoSitewideCommands').prop('checked','checked');
                }
            },
            fail: function(){
                // if no project preference was found, set one based on the site-wide opt-in preference.
                XNAT.xhr.getJSON('/xapi/siteConfig/optIntoSitewideCommands')
                    .done(function(data){
                        var optIn = data || false;
                        var paramToPut = JSON.stringify({ optIntoSitewideCommands: optIn });
                        XNAT.xhr.putJSON({
                            url: projectPrefUrl('PUT'),
                            dataType: 'json',
                            data: paramToPut
                        });
                        if (optIn) {
                            $('#optIntoSitewideCommands').prop('checked','checked');
                        }
                    })
            }
        });
    };

    projCommandConfigManager.init();

    /* ================== *
     * Command Automation *
     * ================== */

    console.log('commandAutomation.js');

    var commandAutomation;

    XNAT.plugin.containerService.commandAutomation = commandAutomation =
        getObject(XNAT.plugin.containerService.commandAutomation || {});

    function getCommandAutomationUrl(appended){
        appended = (appended) ? '?'+appended : '';
        return rootUrl('/xapi/commandeventmapping' + appended);
    }
    function postCommandAutomationUrl(flag){
        flag = (flag) ? '/'+flag : ''; // can be used to set 'enabled' or 'disabled' flag
        return csrfUrl('/xapi/commandeventmapping' + flag);
    }
    function commandAutomationIdUrl(id){
        return csrfUrl('/xapi/commandeventmapping/' + id );
    }

    commandAutomation.deleteAutomation = function(id){
        if (!id) return false;
        XNAT.xhr.delete({
            url: commandAutomationIdUrl(id),
            success: function(){

                XNAT.ui.dialog.open({
                    title: 'Success',
                    width: 400,
                    content: 'Successfully deleted command event mapping.',
                    buttons: [
                        {
                            label: 'OK',
                            isDefault: true,
                            close: true,
                            action: function(){
                                XNAT.plugin.containerService.commandAutomation.init('refresh');
                            }
                        }
                    ]
                })
            },
            fail: function(e){
                errorHandler(e);
            }
        })
    };

    $(document).on('change','#assignCommandIdToWrapper',function(){
        var commandId = $(this).find('option:selected').data('command-id');
        $('#event-command-identifier').val(commandId);
    });

    $(document).on('click','.deleteAutomationButton',function(){
        var automationID = $(this).data('id');
        if (automationID) {
            XNAT.xhr.delete({
                url: commandAutomationIdUrl(automationID),
                success: function(){
                    XNAT.ui.banner.top(2000,'Successfully removed command automation from project.','success');
                    XNAT.plugin.containerService.commandAutomation.init('refresh');
                },
                fail: function(e){
                    errorHandler(e, 'Could not delete command automation');
                }
            })
        }
    });

    commandAutomation.addDialog = function(){
        // get all commands and wrappers that are known to this project, then open a dialog to allow user to configure an automation.
        var projectId = getProjectId();

        function eventCommandSelector(name,options,label,description){
            // receive an array of objects as our list of options
            if (options.length > 0) {
                description = (description) ? description : '';

                // build formatted options list to stick into the generated select menu
                var formattedOptions = [
                    spawn('option',{ selected: true })
                ];
                options.forEach(function(option){
                    formattedOptions.push(
                        spawn('option',{
                            value: option.value,
                            data: { commandId: option['command-id'] },
                            html: option.label
                        } ));
                });

                var select = spawn('div.panel-element',[
                    spawn('label.element-label',label),
                    spawn('div.element-wrapper',[
                        spawn('label',[
                            spawn ('select', {
                                name: name,
                                id: 'assignCommandIdToWrapper'
                            }, formattedOptions )
                        ]),
                        spawn('div.description',description)
                    ])
                ]);

                return select;
            }
        }

        projCommandConfigManager.getAll().done(function(data) {
            if (data.length) {

                // build array of commands that can be selected
                var projectCommandOptions = [];
                data.forEach(function(command){
                    command.xnat.forEach(function(wrapper){
                        projectCommandOptions.push({
                            label: wrapper.name,
                            value: wrapper.name,
                            'command-id': command.id
                        });
                    });
                });

                var eventOptions = {
                    'SessionArchived': 'On Session Archive',
                    'ScanArchived': 'On Scan Archive'
                };

                if (Object.keys(projectCommandOptions).length > 0) {
                    XNAT.ui.dialog.open({
                        title: 'Create Command Automation',
                        width: 500,
                        content: '<div class="panel pad20"></div>',
                        beforeShow: function(obj){
                            // populate form elements
                            var panel = obj.$modal.find('.panel');
                            panel.append( spawn('p','Please enter values for each field.') );
                            panel.append( XNAT.ui.panel.select.single({
                                name: 'event-type',
                                label: 'On Event',
                                options: eventOptions
                            }));
                            panel.append( eventCommandSelector(
                                'xnat-command-wrapper',
                                projectCommandOptions,
                                'Run Command')
                            );
                            panel.append( XNAT.ui.panel.input.hidden({
                                name: 'project',
                                value: projectId
                            }));
                            panel.append( XNAT.ui.panel.input.hidden({
                                name: 'command-id',
                                id: 'event-command-identifier'
                            })); // this will remain without a value until a command wrapper has been selected
                        },
                        buttons: [
                            {
                                label: 'Create Automation',
                                isDefault: true,
                                close: false,
                                action: function(obj){
                                    // collect input values, validate them, and post them to the command-event-mapping URI
                                    var panel = obj.$modal.find('.panel'),
                                        project = panel.find('input[name=project]').val(),
                                        command = panel.find('input[name=command-id]').val(),
                                        wrapper = panel.find('select[name=xnat-command-wrapper]').find('option:selected').val(),
                                        event = panel.find('select[name=event-type]').find('option:selected').val();

                                    if (project && command && wrapper && event){
                                        var data = {
                                            'project': project,
                                            'command-id': command,
                                            'xnat-command-wrapper': wrapper,
                                            'event-type': event
                                        };
                                        XNAT.xhr.postJSON({
                                            url: csrfUrl('/xapi/commandeventmapping'),
                                            data: JSON.stringify(data),
                                            success: function(){
                                                XNAT.ui.banner.top(2000, '<b>Success!</b> Command automation has been added', 'success');
                                                XNAT.ui.dialog.closeAll();
                                                XNAT.plugin.containerService.commandAutomation.init('refresh');
                                            },
                                            fail: function(e){
                                                errorHandler(e,'Could not create command automation');
                                            }
                                        });
                                    } else {
                                        xmodal.alert('Please enter a value for each field');
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
                    // if no wrappers are identified, fail to launch
                }

            } else {
                // if no commands are found, fail to launch

            }
        });
    };

    commandAutomation.table = function(isAdmin){
        // if the user has admin privileges, then display additional controls.
        isAdmin = isAdmin || false;

        // initialize the table - we'll add to it below
        var caTable = XNAT.table({
            className: 'xnat-table compact',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        caTable.tr()
            .th({ addClass: 'left', html: '<b>ID</b>' })
            .th('<b>Event</b>')
            .th('<b>Command</b>')
            .th('<b>Created By</b>')
            .th('<b>Date Created</b>')
            .th('<b>Enabled</b>')
            .th('<b>Action</b>');

        function displayDate(timestamp){
            var d = new Date(timestamp);
            return d.toISOString().replace('T',' ').replace('Z',' ');
        }

        function deleteAutomationButton(id,isAdmin){
            if (isAdmin) return spawn('button.deleteAutomationButton', {
                data: {id: id},
                title: 'Delete Automation'
            }, [ spawn ('i.fa.fa-trash') ]);
        }

        XNAT.xhr.getJSON({
            url: getCommandAutomationUrl(),
            fail: function(e){
                errorHandler(e);
            },
            success: function(data){
                // data returns an array of known command event mappings
                if (data.length){
                    var projectAutomations = false;
                    data.forEach(function(mapping){
                        if (mapping['project'] === getProjectId()) {
                            projectAutomations = true;
                            caTable.tr()
                                .td( '<b>'+mapping['id']+'</b>' )
                                .td( mapping['event-type'] )
                                .td( mapping['xnat-command-wrapper'] )
                                .td( mapping['subscription-user-name'] )
                                .td( displayDate(mapping['timestamp']) )
                                .td( mapping['enabled'] )
                                .td([ deleteAutomationButton(mapping['id'],isAdmin) ])
                        }
                    });

                    if (!projectAutomations) {
                        caTable.tr()
                            .td({ colSpan: '7', html: 'No command automations exist for this project.' });
                    }
                } else {
                    caTable.tr()
                        .td({ colSpan: '7', html: 'No command event mappings exist for this project.' });
                }
            }
        });

        commandAutomation.$table = $(caTable.table);

        return caTable.table;
    };

    commandAutomation.init = function(refresh,isAdmin){
        // initialize the list of command automations
        var manager = $('#command-automation-list');
        var $footer = manager.parents('.panel').find('.panel-footer');

        var isAdmin; // check current user's admin status by checking the JSP page variable PAGE.username

        XNAT.xhr.getJSON({
            url: '/xapi/users/' + PAGE.username + '/roles',
            success: function (userRoles) {
                isAdmin = userRoles.find(function(role){ return role ==='Administrator' });

                manager.html('');
                manager.append(commandAutomation.table(isAdmin));

                if (!refresh && isAdmin !== undefined) {
                    var newAutomation = spawn('button.new-command-automation.btn.btn-sm.submit', {
                        html: 'Add New Command Automation',
                        onclick: function(){
                            commandAutomation.addDialog();
                        }
                    });

                    // add the 'add new' button to the panel footer
                    $footer.append(spawn('div.pull-right', [
                        newAutomation
                    ]));
                    $footer.append(spawn('div.clear.clearFix'));
                }
            },
            fail: function (e) {
                errorHandler(e);
            }
        });


    };

    commandAutomation.init();

}));