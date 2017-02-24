/*
 * web: imageHostManagement.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Manage DICOM SCP Receivers
 */

console.log('imageHostManagement.js');

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

    var imageHostManager, undefined,
        rootUrl = XNAT.url.rootUrl;

    XNAT.admin =
        getObject(XNAT.admin || {});

    XNAT.admin.imageHostManager = imageHostManager =
        getObject(XNAT.admin.imageHostManager || {});

    imageHostManager.samples = [
        {
            "name": "Docker Hub",
            "url": "https://hub.docker.com",
            "enabled": true
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

    function xapiUrl(isDefault,appended){
        appended = isDefined(appended) ? '/' + appended : '';
        if (isDefault) {
            return rootUrl('/xapi/docker/hubs' + appended + '?default='+isDefault)
        } else {
            return rootUrl('/xapi/docker/hubs' + appended);
        }
    }

    // get the list of hosts
    imageHostManager.getHosts = imageHostManager.getAll = function(callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: xapiUrl(),
            dataType: 'json',
            success: function(data){
                imageHostManager.hosts = data;
                callback.apply(this, arguments);
            }
        });
    };

    // dialog to create/edit hosts
    imageHostManager.dialog = function(item, isNew){
        var tmpl = $('#image-host-editor-template');
        var doWhat = !item ? 'New' : 'Edit';
        isNew = firstDefined(isNew, doWhat === 'New');
        console.log(isNew);
        // isNew = true;
        item = item || {};
        xmodal.open({
            title: doWhat + ' Image Hub',
            template: tmpl.clone(),
            width: 400,
            height: 460,
            scroll: false,
            padding: '0',
            beforeShow: function(obj){
                var $form = obj.$modal.find('form');
                if (item && isDefined(item.url)) {
                    $form.setValues(item);
                }
            },
            okClose: false,
            okLabel: 'Save',
            okAction: function(obj){
                // the form panel is 'imageHostTemplate' in containers-elements.yaml
                var $form = obj.$modal.find('form');
                var $url = $form.find('input[name=url]');
                var $name = $form.find('input[name=name]');
                var isDefault = $form.find('input[name=default]').val();
                $form.submitJSON({
                    method: 'POST',
                    url: (isNew) ? xapiUrl(isDefault) : xapiUrl(isDefault,item.id),
                    validate: function(){

                        $form.find(':input').removeClass('invalid');

                        var errors = 0;
                        var errorMsg = 'Errors were found with the following fields: <ul>';

                        [$name,$url].forEach(function($el){
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
                    },
                    fail: function(e){
                        xmodal.close(obj.$modal);
                        xmodal.alert({title: 'Error', content: '<p>Could not save changes. Error code: '+e.status+'</p><p>'+e.statusText+'</p>'});
                    }
                });
            }
        });
    };

    // create table for Image Hosts
    imageHostManager.table = function(imageHost, callback){

        // initialize the table - we'll add to it below
        var chTable = XNAT.table({
            className: 'image-hosts xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        chTable.tr()
            .th({ addClass: 'left', html: '<b>ID</b>' })
            .th('<b>Name</b>')
            .th('<b>URL</b>')
            .th('<b>Default</b>')
            .th('<b>Actions</b>');

        function editLink(item, text){
            return spawn('a.link|href=#!', {
                onclick: function(e){
                    e.preventDefault();
                    imageHostManager.dialog(item, false);
                }
            }, [['b', text]]);
        }

        function editButton(item) {
            return spawn('button.btn.sm.edit', {
                onclick: function(e){
                    e.preventDefault();
                    imageHostManager.dialog(item, false);
                }
            }, 'Edit');
        }

        function defaultToggle(item){
            var defaultVal = !!item.default;
            var rdo = spawn('input.image-host-enabled', {
                type: 'radio',
                name: 'defaultHub',
                checked: defaultVal,
                value: 'default',
                data: { id: item.id, name: item.name },
                onchange: function(){
                    // save the status when clicked
                    var radio = this;
                    defaultVal = radio.checked;
                    XNAT.xhr.post({
                        url: xapiUrl(true,item.id),
                        success: function(){
                            radio.value = defaultVal;
                            radio.checked = 'checked';
                            refreshTable();
                            XNAT.ui.banner.top(1000, '<b>' + item.name + '</b> set as default', 'success');
                        },
                        fail: function(e){
                            radio.checked = false;
                            refreshTable()
                            xmodal.alert({title: 'Error', content: '<p>Could not set hub as default. Error code: '+e.status+'</p><p>'+ e.statusText +'</p>'})
                        }
                    });
                }
            });
            return spawn('div.center', [rdo]);
        }

        function isDefault(status,valIfTrue) {
            return (status) ? valIfTrue : false;
        }

        function deleteButton(item){
            return spawn('button.btn.sm.delete', {
                onclick: function(){
                    xmodal.confirm({
                        height: 220,
                        scroll: false,
                        content: "" +
                        "<p>Are you sure you'd like to delete the Container Host at <b>" + item.url + "</b>?</p>" +
                        "<p><b>This action cannot be undone.</b></p>",
                        okAction: function(){
                            console.log('delete id ' + item.id);
                            XNAT.xhr.delete({
                                url: xapiUrl(false,item.id),
                                success: function(){
                                    console.log('"'+ item.url + '" deleted');
                                    XNAT.ui.banner.top(1000, '<b>"'+ item.url + '"</b> deleted.', 'success');
                                    refreshTable();
                                }
                            });
                        }
                    })
                },
                disabled: isDefault(item.default,"disabled"),
                title: isDefault(item.default,"Cannot delete the default hub")
            }, 'Delete');
        }

        imageHostManager.getAll().done(function(data){
            data = [].concat(data);
            data.forEach(function(item){
                chTable.tr({ title: item.name, data: { id: item.id, name: item.name, url: item.url}})
                    .td(['div.center'],item.id)
                    .td([editLink(item, item.name)]).addClass('name')
                    .td(item.url)
                    .td([defaultToggle(item)]).addClass('status')
                    .td([['div.center', [editButton(item), spacer(10), deleteButton(item)]]]);
            });

            if (imageHost){
                $$(imageHost).append(chTable.table);
            }

            if (isFunction(callback)) {
                callback(chTable.table);
            }

        });

        imageHostManager.$table = $(chTable.table);

        return chTable.table;
    };

    imageHostManager.init = function(container){

        var $manager = $$(container||'div#image-host-manager');

        imageHostManager.container = $manager;

        $manager.append(imageHostManager.table());
        // imageHostManager.table($manager);

        var newReceiver = spawn('button.new-image-host.btn.btn-sm.submit', {
            html: 'New Image Hub',
            onclick: function(){
                imageHostManager.dialog(null, true);
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
        imageHostManager.$table.remove();
        imageHostManager.table(null, function(table){
            imageHostManager.container.prepend(table);
        });
    }

    imageHostManager.refresh = refreshTable;

    imageHostManager.init();

    return XNAT.admin.imageHostManager = imageHostManager;

}));
