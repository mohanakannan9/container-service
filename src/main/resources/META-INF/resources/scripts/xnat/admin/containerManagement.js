/*
 * web: dicomScpManager.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Manage DICOM SCP Receivers
 */

console.log('dicomScpManager.js');

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

    var containerManager, undefined,
        rootUrl = XNAT.url.rootUrl;

    XNAT.admin =
        getObject(XNAT.admin || {});

    XNAT.admin.containerManager = containerManager =
        getObject(XNAT.admin.containerManager || {});

    containerManager.samples = [
        {
            "aeTitle": "Bogus",
            "enabled": true,
            "fileNamer": "string",
            "identifier": "string",
            "port": 0,
            "scpId": "BOGUS"
        },
        {
            "enabled": true,
            "fileNamer": "string",
            "identifier": "string",
            "port": 8104,
            "scpId": "XNAT",
            "aeTitle": "XNAT"
        }
    ];

    function spacer(width){
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        })
    }

    function scpUrl(appended){
        appended = isDefined(appended) ? '/' + appended : '';
        return rootUrl('/xapi/dicomscp' + appended);
    }

    containerManager.getReceiver = containerManager.getOne = function(id, callback){
        if (!id) return null;
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: scpUrl(id),
            dataType: 'json',
            success: callback
        });
    };

    containerManager.get = function(id){
        if (!id) {
            return containerManager.getAll();
        }
        return containerManager.getOne(id);
    };

    // dialog to create/edit receivers
    containerManager.dialog = function(item, isNew){
        var tmpl = $('#dicom-scp-editor-template');
        var doWhat = !item ? 'New' : 'Edit';
        var oldPort = item && item.port ? item.port : null;
        isNew = firstDefined(isNew, doWhat === 'New');
        console.log(isNew);
        item = item || {};
        xmodal.open({
            title: doWhat + ' Container Server Host',
            template: tmpl.clone(),
            width: 350,
            height: 300,
            scroll: false,
            padding: '0',
            beforeShow: function(obj){
                var $form = obj.$modal.find('#container-host-editor-panel');
                if (item && isDefined(item.id)) {
                    $form.setValues(item);
                }
            },
            okClose: false,
            okLabel: 'Save',
            okAction: function(obj){
                // the form panel is 'dicomScpEditorTemplate' in site-admin-element.yaml
                var $form = obj.$modal.find('#dicomScpEditorTemplate');
                var $title = $form.find('#scp-title');
                var $port = $form.find('#scp-port');
                console.log(item.id);
                $form.submitJSON({
                    method: isNew ? 'POST' : 'PUT',
                    url: isNew ? scpUrl() : scpUrl(item.id),
                    validate: function(){

                        $form.find(':input').removeClass('invalid');

                        var errors = 0;
                        var errorMsg = 'Errors were found with the following fields: <ul>';

                        [$port, $title].forEach(function($el){
                            var el = $el[0];
                            if (!el.value) {
                                errors++;
                                errorMsg += '<li><b>' + el.title + '</b> is required.</li>';
                                $el.addClass('invalid');
                            }
                        });

                        errorMsg += '</ul>';

                        if (errors > 0) {
                            xmodal.message('Errors Found', errorMsg, { height: 300 });
                        }

                        return errors === 0;

                    },
                    success: function(){
                        refreshTable();
                        xmodal.close(obj.$modal);
                        XNAT.ui.banner.top(2000, 'Saved.', 'success')
                    }
                });
            }
        });
    };

    // create table for DICOM SCP receivers
    containerManager.table = function(container, callback){

        // initialize the table - we'll add to it below
        var scpTable = XNAT.table({
            className: 'dicom-scp-receivers xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        scpTable.tr()
            .th({ addClass: 'left', html: '<b>AE Title</b>' })
            .th('<b>Port</b>')
            .th('<b>Enabled</b>')
            .th('<b>Actions</b>');

        // TODO: move event listeners to parent elements - events will bubble up
        // ^-- this will reduce the number of event listeners
        function enabledCheckbox(item){
            var enabled = !!item.enabled;
            var ckbox = spawn('input.dicom-scp-enabled', {
                type: 'checkbox',
                checked: enabled,
                value: enabled,
                data: { id: item.id, name: item.aeTitle },
                onchange: function(){
                    // save the status when clicked
                    var checkbox = this;
                    enabled = checkbox.checked;
                    XNAT.xhr.put({
                        url: scpUrl(item.id + '/enabled/' + enabled),
                        success: function(){
                            var status = (enabled ? ' enabled' : ' disabled');
                            checkbox.value = enabled;
                            XNAT.ui.banner.top(1000, '<b>' + item.aeTitle + '</b> ' + status, 'success');
                            console.log(item.aeTitle + status)
                        }
                    });
                }
            });
            return spawn('div.center', [
                spawn('label.switchbox|title=' + item.aeTitle, [
                    ckbox,
                    ['span.switchbox-outer', [['span.switchbox-inner']]]
                ])
            ]);
        }

        function editLink(item, text){
            return spawn('a.link|href=#!', {
                onclick: function(e){
                    e.preventDefault();
                    containerManager.dialog(item, false);
                }
            }, [['b', text]]);
        }

        function editButton(item) {
            return spawn('button.btn.sm.edit', {
                onclick: function(e){
                    e.preventDefault();
                    containerManager.dialog(item, false);
                }
            }, 'Edit');
        }

        function deleteButton(item){
            return spawn('button.btn.sm.delete', {
                onclick: function(){
                    xmodal.confirm({
                        height: 220,
                        scroll: false,
                        content: "" +
                        "<p>Are you sure you'd like to delete the '<b>" + item.aeTitle + "</b>' DICOM Receiver?</p>" +
                        "<p><b>This action cannot be undone.</b></p>",
                        okAction: function(){
                            console.log('delete id ' + item.id);
                            XNAT.xhr.delete({
                                url: scpUrl(item.id),
                                success: function(){
                                    console.log('"'+ item.aeTitle + '" deleted');
                                    XNAT.ui.banner.top(1000, '<b>"'+ item.aeTitle + '"</b> deleted.', 'success');
                                    refreshTable();
                                }
                            });
                        }
                    })
                }
            }, 'Delete');
        }

        containerManager.getAll().done(function(data){
            data.forEach(function(item){
                scpTable.tr({ title: item.aeTitle, data: { id: item.id, port: item.port }})
                    .td([editLink(item, item.aeTitle)]).addClass('aeTitle')
                    .td([['div.mono.center', item.port]]).addClass('port')
                    .td([enabledCheckbox(item)]).addClass('status')
                    .td([['div.center', [editButton(item), spacer(10), deleteButton(item)]]]);
            });

            if (container){
                $$(container).append(scpTable.table);
            }

            if (isFunction(callback)) {
                callback(scpTable.table);
            }

        });

        containerManager.$table = $(scpTable.table);

        return scpTable.table;
    };

    containerManager.init = function(container){

        var $manager = $$(container||'div#dicom-scp-manager');

        containerManager.$container = $manager;

        $manager.append(containerManager.table());
        // containerManager.table($manager);

        var newReceiver = spawn('button.new-dicomscp-receiver.btn.btn-sm.submit', {
            html: 'New DICOM SCP Receiver',
            onclick: function(){
                containerManager.dialog(null, true);
            }
        });

        var startAll = spawn('button.start-receivers.btn.btn-sm', {
            html: 'Start All',
            onclick: function(){
                XNAT.xhr.put({
                    url: scpUrl('start'),
                    success: function(){
                        console.log('DICOM SCP Receivers started')
                    }
                })
            }
        });

        var stopAll = spawn('button.stop-receivers.btn.btn-sm', {
            html: 'Stop All',
            onclick: function(){
                XNAT.xhr.put({
                    url: scpUrl('stop'),
                    success: function(){
                        console.log('DICOM SCP Receivers stopped')
                    }
                })
            }
        });

        // add the start, stop, and 'add new' buttons at the bottom
        $manager.append(spawn('div', [
            // startAll,
            // spacer(10),
            // stopAll,
            newReceiver,
            ['div.clear.clearfix']
        ]));

        return {
            element: $manager[0],
            spawned: $manager[0],
            get: function(){
                return $manager[0]
            }
        };
    };

    function refreshTable(){
        containerManager.$table.remove();
        containerManager.table(null, function(table){
            containerManager.$container.prepend(table);
        });
    }

    containerManager.refresh = refreshTable;

    containerManager.init();

    return XNAT.admin.containerManager = containerManager;

}));
