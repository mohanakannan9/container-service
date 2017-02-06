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

console.log('containerHostManagement.js');

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

    var containerHostManager, undefined,
        rootUrl = XNAT.url.rootUrl;

    XNAT.admin =
        getObject(XNAT.admin || {});

    XNAT.admin.containerHostManager = containerHostManager =
        getObject(XNAT.admin.containerHostManager || {});

    containerHostManager.samples = [
        {
            "host": "unix:///var/run/docker.sock",
            "cert-path": ""
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

    function xapiUrl(appended){
        appended = isDefined(appended) ? '/' + appended : '';
        return rootUrl('/xapi/docker/server' + appended);
    }

    // get the list of hosts
    containerHostManager.getHosts = containerHostManager.getAll = function(callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: xapiUrl(),
            dataType: 'json',
            success: function(data){
                containerHostManager.hosts = data;
                callback.apply(this, arguments);
            }
        });
    };

    // dialog to create/edit hosts
    containerHostManager.dialog = function(item, isNew){
        var tmpl = $('#container-host-editor-template');
        var doWhat = !item ? 'New' : 'Edit';
        isNew = firstDefined(isNew, doWhat === 'New');
        console.log(isNew);
        // isNew = true;
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
                // the form panel is 'containerHostTemplate' in site-admin-element.yaml
                var $form = obj.$modal.find('form');
                var $host = $form.find('input[name=host]');
                console.log($host);
                $form.submitJSON({
                    method: isNew ? 'POST' : 'PUT',
                    url: isNew ? xapiUrl() : xapiUrl(item.id),
                    validate: function(){

                        $form.find(':input').removeClass('invalid');

                        var errors = 0;
                        var errorMsg = 'Errors were found with the following fields: <ul>';

                        [$host].forEach(function($el){
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

    // create table for Container Hosts
    containerHostManager.table = function(container, callback){

        // initialize the table - we'll add to it below
        var chTable = XNAT.table({
            className: 'container-hosts xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        chTable.tr()
            .th({ addClass: 'left', html: '<b>Host</b>' })
            .th('<b>Cert Path</b>')
            .th('<b>Enabled</b>')
            .th('<b>Actions</b>');

        function editLink(item, text){
            return spawn('a.link|href=#!', {
                onclick: function(e){
                    e.preventDefault();
                    containerHostManager.dialog(item, false);
                }
            }, [['b', text]]);
        }

        function editButton(item) {
            return spawn('button.btn.sm.edit', {
                onclick: function(e){
                    e.preventDefault();
                    containerHostManager.dialog(item, false);
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
                        "<p>Are you sure you'd like to delete the Container Host at <b>" + item.host + "</b>?</p>" +
                        "<p><b>This action cannot be undone.</b></p>",
                        okAction: function(){
                            console.log('delete id ' + item.id);
                            XNAT.xhr.delete({
                                url: xapiUrl(item.id),
                                success: function(){
                                    console.log('"'+ item.host + '" deleted');
                                    XNAT.ui.banner.top(1000, '<b>"'+ item.host + '"</b> deleted.', 'success');
                                    refreshTable();
                                }
                            });
                        }
                    })
                }
            }, 'Delete');
        }

        containerHostManager.getAll().done(function(data){
            data = [].concat(data);
            data.forEach(function(item){
                chTable.tr({ title: item.host, data: { id: item.id, host: item.host, certPath: item.certPath}})
                    .td([editLink(item, item.host)]).addClass('host')
                    .td(item.certPath)
                    .td([['div.center','enabled']])
                    .td([['div.center', [editButton(item), spacer(10), deleteButton(item)]]]);
            });

            if (container){
                $$(container).append(chTable.table);
            }

            if (isFunction(callback)) {
                callback(chTable.table);
            }

        });

        containerHostManager.$table = $(chTable.table);

        return chTable.table;
    };

    containerHostManager.init = function(container){

        var $manager = $$(container||'div#container-host-manager');

        containerHostManager.$container = $manager;

        $manager.append(containerHostManager.table());
        // containerHostManager.table($manager);

        var newReceiver = spawn('button.new-container-host.btn.btn-sm.submit', {
            html: 'New Container Host',
            onclick: function(){
                containerHostManager.dialog(null, true);
            }
        });



        // add the 'add new' button at the bottom
        $manager.append(spawn('div', [
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
        containerHostManager.$table.remove();
        containerHostManager.table(null, function(table){
            containerHostManager.$container.prepend(table);
        });
    }

    containerHostManager.refresh = refreshTable;

    containerHostManager.init();

    return XNAT.admin.containerHostManager = containerHostManager;

}));
