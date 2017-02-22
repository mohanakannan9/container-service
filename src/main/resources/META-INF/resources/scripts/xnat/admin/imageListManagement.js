/*
 * web: imageListManagement.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Manage DICOM SCP Receivers
 */

console.log('imageListManagement.js');

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

    var imageListManager, 
        imageFilterManager, 
        addImage,
        commandListManager,
        commandDefinition,
        undefined,
        rootUrl = XNAT.url.rootUrl;
    
    XNAT.admin =
        getObject(XNAT.admin || {});

    XNAT.admin.imageListManager = imageListManager =
        getObject(XNAT.admin.imageListManager || {});

    XNAT.admin.imageFilterManager = imageFilterManager =
        getObject(XNAT.admin.imageFilterManager || {});

    XNAT.admin.addImage = addImage =
        getObject(XNAT.admin.addImage || {});

    XNAT.admin.commandListManager = commandListManager =
        getObject(XNAT.admin.commandListManager || {});
    
    XNAT.admin.commandDefinition = commandDefinition = 
        getObject(XNAT.admin.commandDefinition || {});

    imageListManager.samples = [
        {
            "label": "Docker Hub",
            "image_id": "https://hub.docker.com",
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

    function imageUrl(appended){
        appended = isDefined(appended) ? '/' + appended : '';
        return rootUrl('/xapi/docker/images' + appended);
    }

    function commandUrl(appended){
        appended = isDefined(appended) ? '/' + appended : '';
        return rootUrl('/xapi/commands' + appended);
    }

    // get the list of images
    imageListManager.getImages = imageListManager.getAll = function(callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: imageUrl(),
            dataType: 'json',
            success: function(data){
                imageListManager.hosts = data;
                callback.apply(this, arguments);
            }
        });
    };

    commandListManager.getCommands = commandListManager.getAll = function(callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: commandUrl(),
            dataType: 'json',
            success: function(data){
                commandListManager.commands = data;
                callback.apply(this,arguments);
            }
        });
    };

    // dialog to add new images
    addImage.dialog = function(item){
        var tmpl = $('#add-image-template');
        item = item || {};
        xmodal.open({
            title: 'Pull New Image',
            template: tmpl.clone(),
            width: 400,
            height: 420,
            scroll: false,
            padding: '0',
            okClose: false,
            okLabel: 'Pull Image',
            okAction: function(obj){
                // the form panel is 'imageListTemplate' in site-admin-element.yaml
                var $form = obj.$modal.find('form');
                var $image = $form.find('input[name=image]');
                var $tag = $form.find('input[name=tag]');
                $form.submitJSON({
                    method: 'POST',
                    url: imageUrl('pull'),
                    validate: function(){

                        $form.find(':input').removeClass('invalid');

                        var errors = 0;
                        var errorMsg = 'Errors were found with the following fields: <ul>';

                        [$image,$tag].forEach(function($el){
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
                    always: function(){
                        xmodal.loading.open({title: 'Submitting Pull Request',height: '110'});
                    },
                    success: function(){
                        refreshTable();
                        xmodal.close(obj.$modal);
                        XNAT.ui.banner.top(2000, 'Pull request complete.', 'success');
                    }
                });
            }
        });
    };

    // create a read-only code editor dialog to view a command definition
    commandDefinition.dialog = function(data){
        data = data || {};

        var _source = spawn('textarea', JSON.stringify(data, null, 4));

        var _editor = XNAT.app.codeEditor.init(_source, {
            language: 'json'
        });

        _editor.openEditor({
            title: data.name,
            classes: 'plugin-json',
            footerContent: '(read-only)',
            buttons: {
                // json: {
                //     label: 'View JSON',
                //     link: true,
                //     action: function(){
                //         xmodal.iframe({
                //             title: data.id,
                //             url: _url,
                //             width: 720, height: 480,
                //             buttons: { close: { label: 'Close' } }
                //         })
                //     }
                // },
                close: { label: 'Close' },
                info: {
                    label: 'View Command Info',
                    action: function(){
                        window.open(data['info-url'],'infoUrl');
                    }
                }
            },
            afterShow: function(dialog, obj){
                obj.aceEditor.setReadOnly(true);
            }
        });
    };


    // create table for listing comands
    commandListManager.table = function(callback){

        // initialize the table - we'll add to it below
        var chTable = XNAT.table({
            className: 'enabled-commands xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        chTable.tr()
            .th({ addClass: 'left', html: '<b>Command</b>' })
            .th('<b>XNAT Contexts</b>')
            .th('<b>Version</b>')
            .th('<b>Image</b>')
            // .th('<b>Enabled</b>')
            .th('<b>Actions</b>');

        function viewLink(item, text){
            return spawn('a.link|href=#!', {
                onclick: function(e){
                    e.preventDefault();
                    commandDefinition.dialog(item, false);
                }
            }, [['b', text]]);
        }

        function enabledCheckbox(item){
            var enabled = !!item.enabled;
            var ckbox = spawn('input.command-enabled', {
                type: 'checkbox',
                checked: enabled,
                value: enabled,
                data: { name: item.name },
                onchange: function(){
                /*    // save the status when clicked
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
                    */
                }
            });

            return spawn('div.center', [
                spawn('label.switchbox|title=' + item.name, [
                    ckbox,
                    ['span.switchbox-outer', [['span.switchbox-inner']]]
                ])
            ]);
        }

        function deleteCommandButton(item){
            return spawn('button.btn.sm.delete', {
                onclick: function(){
                    xmodal.confirm({
                        height: 220,
                        scroll: false,
                        content: "" +
                        "<p>Are you sure you'd like to delete the <b>" + item.name + "</b> command definition?</p>" +
                        "<p><b>This action cannot be undone.</b></p>",
                        okAction: function(){
                            console.log('delete id ' + item.id);
                            XNAT.xhr.delete({
                                url: commandUrl(item.id),
                                success: function(){
                                    console.log('"'+ item.name + '" command deleted');
                                    XNAT.ui.banner.top(1000, '<b>"'+ item.name + '"</b> deleted.', 'success');
                                    refreshTable();
                                }
                            });
                        }
                    })
                }
            }, 'Delete');
        }

        commandListManager.getAll().done(function(data){
            data = [].concat(data);
            if (data.length > 0) {
                data.forEach(function(item){
                    var xnatContexts = '';
                    item.xnat = [].concat(item.xnat);
                    [item.xnat].forEach(function(innerItem,i){
                        if (xnatContexts.length > 0) xnatContexts += '<br>';
                        xnatContexts += innerItem[i].description;
                    });
                    chTable.tr({ title: item.name, data: { id: item.id, name: item.name, image: item.image}})
                        .td([viewLink(item, item.name)]).addClass('name')
                        // .td(item.name).addClass('name')
                        .td(xnatContexts)
                        .td(item.version)
                        .td(item.image)
                        // .td([enabledCheckbox(item)]).addClass('status')
                        .td([['div.center', [deleteCommandButton(item)]]]);
                });
            } else {
                chTable.tr({ title: 'no commands found'})
                    .td('No commands found').attr('colspan:5');
            }


            if (isFunction(callback)) {
                callback(chTable.table);
            }

        });

        commandListManager.$table = $(chTable.table);

        return chTable.table;
    };

    imageFilterManager.init = function(container){

        var $manager = $$(container||'div#image-filter-bar');

        imageFilterManager.container = $manager;

//        $manager.append(imageListManager.table());
        // imageListManager.table($manager);

        var newImage = spawn('button.new-image.btn.btn-sm.submit', {
            html: 'Add New Image',
            onclick: function(){
                addImage.dialog(null);
            }
        });



        // add the 'add new' button at the bottom
        $manager.append(spawn('div.pull-right', [
            newImage
        ]));
        $manager.append(spawn('div.clear.clearfix'));

        return {
            element: $manager[0],
            spawned: $manager[0],
            get: function(){
                return $manager[0]
            }
        };
    };

    imageListManager.init = function(container){
        var $manager = $$(container||'div#image-list-container');

        imageListManager.container = $manager;

        imageListManager.getAll().done(function(data){
            data = [].concat(data);
            [data].forEach(function(item){
                var imageInfo = item[0];
//                var imageContainer = "<div><h3>"+ item.tags[0] +"</h3></div>";
                $manager.append(spawn('div.imageContainer',[
                    ['h3.imageTitle',imageInfo.tags[0]],
                    ['div.imageCommandList',[commandListManager.table()]]
                ]));
            });
        });
    };


    function refreshTable(){
        imageListManager.container.remove();
        imageListManager.init();
    }

    // imageListManager.refresh = refreshTable;

    imageFilterManager.init();
    imageListManager.init();

//    return XNAT.admin.imageListManager = imageListManager;

}));
