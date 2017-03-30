/*
 * web: commandConfigManagement.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Manage Site-wide Command Configs
 */

console.log('commandConfigManagement.js');

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

    var commandConfigManager,
        configDefinition,
        undefined,
        rootUrl = XNAT.url.rootUrl;

    XNAT.admin =
        getObject(XNAT.admin || {});

    XNAT.admin.commandConfigManager = commandConfigManager =
        getObject(XNAT.admin.commandConfigManager || {});

    XNAT.admin.configDefinition = configDefinition =
        getObject(XNAT.admin.configDefinition || {});

    function spacer(width){
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        })
    }

    function commandUrl(appended){
        appended = isDefined(appended) ? appended : '';
        return rootUrl('/xapi/commands' + appended);
    }

    function configUrl(command,wrapperName,appended){
        appended = isDefined(appended) ? '?' + appended : '';
        if (!command || !wrapperName) return false;
        return rootUrl('/xapi/commands/'+command+'/wrappers/'+wrapperName+'/config' + appended);
    }

    function configEnableUrl(command,wrapperName,flag){
        if (!command || !wrapperName || !flag) return false;
        return rootUrl('/xapi/commands/'+command+'/wrappers/'+wrapperName+'/' + flag);
    }

    commandConfigManager.getCommands = commandConfigManager.getAll = function(callback){
        
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

    configDefinition.getConfig = function(commandId,wrapperName,callback){
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

    configDefinition.table = function(config) {

        // initialize the table - we'll add to it below
        var chTable = XNAT.table({
            className: 'command-config-definition xnat-table '+config.type,
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        function basicConfigInput(name,value,required) {
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
            return '<input type="hidden" name="'+name+'" value="'+value+'" />'
        }


        // determine which type of table to build.
        if (config.type === 'inputs') {
            var inputs = config.inputs;

            // add table header row
            chTable.tr()
                .th({ addClass: 'left', html: '<b>Input</b>' })
                .th('<b>Default Value</b>')
                .th('<b>Matcher Value</b>')
                .th('<b>User-Settable?</b>')
                .th('<b>Advanced?</b>');

            for (i in inputs) {
                var input = inputs[i];
                chTable.tr({ data: { input: i }, className: 'input' })
                    .td( { data: { key: 'key' }, addClass: 'left'}, i )
                    .td( { data: { key: 'property', property: 'default-value' }}, basicConfigInput('defaultVal',input['default-value']) )
                    .td( { data: { key: 'property', property: 'matcher' }}, basicConfigInput('matcher',input['matcher']) )
                    .td( { data: { key: 'property', property: 'user-settable' }}, [['div', [configCheckbox('userSettable',input['user-settable']) ]]])
                    .td( { data: { key: 'property', property: 'advanced' }}, [['div', [configCheckbox('advanced',input['advanced']) ]]]);

            }

        } else if (config.type === 'outputs') {
            var outputs = config.outputs;

            // add table header row
            chTable.tr()
                .th({ addClass: 'left', html: '<b>Output</b>' })
                .th({ addClass: 'left', width: '75%', html: '<b>Label</b>' });

            for (o in outputs) {
                var output = outputs[o];
                chTable.tr({ data: { output: o }, className: 'output' })
                    .td( { data: { key: 'key' }, addClass: 'left'}, o )
                    .td( { data: { key: 'property', property: 'label' }}, basicConfigInput('label',output['label']) );
            }

        }

        configDefinition.$table = $(chTable.table);

        return chTable.table;
    };


    configDefinition.dialog = function(commandId,wrapperName){
        // get command definition
        configDefinition.getConfig(commandId,wrapperName)
            .success(function(data){
                var tmpl = $('div#command-config-template');
                var tmplBody = $(tmpl).find('.panel-body').html('');

                var inputs = data.inputs;
                var outputs = data.outputs;

                tmplBody.spawn('h3','Inputs');
                tmplBody.append(configDefinition.table({ type: 'inputs', inputs: inputs }));

                tmplBody.spawn('h3','Outputs');
                tmplBody.append(configDefinition.table({ type: 'outputs', outputs: outputs }));

                xmodal.open({
                    title: 'Set Config Values',
                    template: tmpl.clone(),
                    width: 850,
                    height: 500,
                    scroll: true,
                    beforeShow: function(obj){
                        var $panel = obj.$modal.find('#config-viewer-panel');
                        /*
                        $panel.find('input[type=text]').each(function(){
                            $(this).val($(this).data('value'));
                        });
                        */
                        $panel.find('input[type=checkbox]').each(function(){
                            $(this).prop('checked',$(this).data('checked'));
                        })
                    },
                    okClose: false,
                    okLabel: 'Save',
                    okAction: function(obj){
                        var $panel = obj.$modal.find('#config-viewer-panel');
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
                                console.log('"' + wrapperName + '" updated');
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
                console.log('Could not open config definition.', e);
            });

    };


    commandConfigManager.table = function(callback){

        // initialize the table - we'll add to it below
        var chTable = XNAT.table({
            className: 'sitewide-command-configs xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        chTable.tr()
            .th({ addClass: 'left', html: '<b>XNAT Command Label</b>' })
            .th('<b>Container</b>')
            .th('<b>Enabled</b>')
            .th('<b>Actions</b>');

        function viewLink(item, wrapper, text){
            return spawn('a.link|href=#!', {
                onclick: function(e){
                    e.preventDefault();
                    configDefinition.dialog(item.id, wrapper.name, false);
                    console.log('Open Config definition for '+ wrapper.name);
                }
            }, [['b', text]]);
        }

        function viewConfigButton(item,wrapper){
            return spawn('button.btn.sm', {
                onclick: function(e){
                    e.preventDefault();
                    configDefinition.dialog(item.id, wrapper.name, false);
                }
            }, 'View Command Configuration');
        }

        function enabledCheckbox(item,wrapper){
            enabled = !!item.enabled;
            var ckbox = spawn('input.config-enabled', {
                type: 'checkbox',
                checked: enabled,
                value: enabled,
                data: { name: item.name },
                onchange: function(){
                    // save the status when clicked
                    var checkbox = this;
                    enabled = checkbox.checked;
                    var enabledFlag = (enabled) ? 'enabled' : 'disabled';

                    XNAT.xhr.put({
                        url: configEnableUrl(item.id,wrapper.name,enabledFlag),
                        success: function(){
                            var status = (enabled ? ' enabled' : ' disabled');
                            checkbox.value = enabled;
                            XNAT.ui.banner.top(1000, '<b>' + wrapper.name+ '</b> ' + status, 'success');
                            console.log(wrapper.name + status)
                        }
                    });
                }
            });

            return spawn('div.center', [
                spawn('label.switchbox|title=' + item.name, [
                    ckbox,
                    ['span.switchbox-outer', [['span.switchbox-inner']]]
                ])
            ]);

        }

        function deleteConfigButton(item,wrapper){
            return spawn('button.btn.sm.delete', {
                onclick: function(){
                    xmodal.confirm({
                        height: 220,
                        scroll: false,
                        content: "" +
                        "<p>Are you sure you'd like to delete the <b>" + item.name + "</b> command configuration?</p>" +
                        "<p><b>This action cannot be undone. This action does not delete any project-specific configurations for this command.</b></p>",
                        okAction: function(){
                            console.log('delete id ' + item.id);
                            XNAT.xhr.delete({
                                url: configUrl(item.id,wrapper.name),
                                success: function(){
                                    console.log('"'+ wrapper.name + '" command deleted');
                                    XNAT.ui.banner.top(1000, '<b>"'+ wrapper.name + '"</b> configuration deleted.', 'success');
                                    refreshTable();
                                }
                            });
                        }
                    })
                }
            }, 'Delete');
        }

        commandConfigManager.getAll().done(function(data) {
            if (data) {
                for (var i = 0, j = data.length; i < j; i++) {
                    var xnatActions = '', item = data[i];
                    if (item.xnat) {
                        for (var k = 0, l = item.xnat.length; k < l; k++) {
                            var wrapper = item.xnat[k];

                            XNAT.xhr.get({
                                url: configEnableUrl(item.id,wrapper.name,'enabled'),
                                success: function(enabled){
                                    item.enabled = enabled;
                                    chTable.tr({title: wrapper.name, data: {id: item.id, name: wrapper.name, image: item.image}})
                                        .td([viewLink(item, wrapper, wrapper.description)]).addClass('name')
                                        .td(item.image)
                                        .td([['div.center', [enabledCheckbox(item,wrapper)]]])
                                        .td([['div.center', [viewConfigButton(item,wrapper), spacer(10), deleteConfigButton(item,wrapper)]]]);
                                }
                            });

                        }
                    }

                }
            } else {
                // create a handler when no command data is returned.
                chTable.tr({title: 'No command config data found'})
                    .td({colSpan: '5', html: 'No XNAT-enabled Commands Found'});
            }
        });

        commandConfigManager.$table = $(chTable.table);

        return chTable.table;
    };

    commandConfigManager.init = function(container){
        var $manager = $$(container||'div#command-config-list-container');

        /*
        var newCommand = spawn('button.new-command.btn.sm', {
            html: 'Add New Command',
            onclick: function(){
                commandDefinition.dialog(null,true); // opens the command code editor dialog with "new command" set to true
            }
        });
        */

        commandConfigManager.container = $manager;

        $manager.append(commandConfigManager.table());
    };

    commandConfigManager.init();

}));