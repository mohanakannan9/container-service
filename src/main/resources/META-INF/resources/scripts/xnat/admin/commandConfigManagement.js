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

    configDefinition.dialog = function(commandId,wrapperName){
        // get command definition
        configDefinition.getConfig(commandId,wrapperName)
            .success(function(data){

                var _source = spawn('textarea', JSON.stringify(data, null, 4));

                var _editor = XNAT.app.codeEditor.init(_source, {
                    language: 'json'
                });

                _editor.openEditor({
                    title: 'Config Definition for '+wrapperName,
                    classes: 'plugin-json',
                    footerContent: '(read-only)',
                    buttons: {
                        close: { label: 'Close' },
                    },
                    afterShow: function(dialog, obj){
                        obj.aceEditor.setReadOnly(true);
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
            var enabled = !!item.enabled;
            var ckbox = spawn('input.config-enabled', {
                type: 'checkbox',
                checked: enabled,
                value: enabled,
                data: { name: item.name },
                onchange: function(){
                     // save the status when clicked
                     var checkbox = this;
                     enabled = checkbox.checked;
                     XNAT.xhr.post({
                         url: configUrl(item.id,wrapper.name,'enable='+enabled),
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

                            chTable.tr({title: wrapper.name, data: {id: item.id, name: wrapper.name, image: item.image}})
                                .td([viewLink(item, wrapper, wrapper.description)]).addClass('name')
                                .td(item.image)
                                .td([['div.center', [enabledCheckbox(item,wrapper)]]])
                                .td([['div.center', [viewConfigButton(item,wrapper), spacer(10), deleteConfigButton(item,wrapper)]]]);
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