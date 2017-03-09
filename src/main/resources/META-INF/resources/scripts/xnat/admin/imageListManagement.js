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
        imageHubs,
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

    XNAT.admin.imageHubs = imageHubs =
        getObject(XNAT.admin.imageHubs || {});

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
        appended = isDefined(appended) ? appended : '';
        return rootUrl('/xapi/commands' + appended);
    }

    function imagePullUrl(appended,hubId){
        if (isDefined(hubId)) {
            return rootUrl('/xapi/docker/hubs/'+ hubId +'pull?' + appended);
        } else {
            return rootUrl('/xapi/docker/pull?' + appended);
        }
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

    // get the list of image hubs
    imageHubs.getHubs = imageHubs.getAll = function(callback){
        callback = isFunction(callback)? callback : function(){};
        return XNAT.xhr.get({
            url: '/xapi/docker/hubs',
            dataType: 'json',
            success: function(data){
                imageHubs.hubs = data;
                callback.apply(this, arguments);
            }
        });
    };

    commandListManager.getCommands = commandListManager.getAll = function(imageName,callback){
        /*
        if (imageName) {
            imageName = imageName.split(':')[0]; // remove any tag definition (i.e. ':latest') in the image name
            imageName = imageName.replace("/","%2F"); // convert slashes in image names to URL-ASCII equivalent
        }
        */
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: (imageName) ? commandUrl('?image='+imageName) : commandUrl(),
            dataType: 'json',
            success: function(data){
                if (data) {
                    return data;
                }
                callback.apply(this, arguments);
            }
        });
    };

    commandDefinition.getCommand = function(id,callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: commandUrl('/'+id),
            dataType: 'json',
            success: function(data){
                if (data) {
                    return data;
                }
                callback.apply(this, arguments);
            }
        })
    };

    // dialog to add new images
    addImage.dialog = function(item){
        var tmpl = $('#add-image-template');
        var pullUrl;
        item = item || {};
        xmodal.open({
            title: 'Pull New Image',
            template: tmpl.clone(),
            width: 400,
            height: 420,
            scroll: false,
            padding: '0',
            beforeShow: function(obj){
                var $form = obj.$modal.find('form');
                if (item && isDefined(item.image)) {
                    $form.setValues(item);
                }
                var $hubSelect = $form.find('#hub-id');
                // get list of image hubs and select the default hub
                imageHubs.getAll().done(function(hubs){
                    if (hubs.length > 1) {
                        hubs.forEach(function(item){
                            var option = '<option value="'+item.id+'"';
                            if (item.default) option += ' selected';
                            option += '>'+item.name+'</option>';
                            $hubSelect.prop('disabled',false).append(option);
                        });
                    } else {
                        $hubSelect
                    }
                });
            },
            okClose: false,
            okLabel: 'Pull Image',
            okAction: function(obj){
                // the form panel is 'imageListTemplate' in containers-elements.yaml
                var $form = obj.$modal.find('form');
                var $image = $form.find('input[name=image]');
                var $tag = $form.find('input[name=tag]');

                // validate form inputs, then pull them into the URI querystring and create an XHR request.
                $form.find(':input').removeClass('invalid');

                var errors = 0;
                var errorMsg = 'Errors were found with the following fields: <ul>';

                [$image].forEach(function($el){
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
                } else {
                    // stitch together the image and tag definition, if a tag value was specified.
                    if ($tag.val().length > 0 && $tag.val().indexOf(':') < 0) {
                        $tag.val(':' + $tag.val());
                    }
                    var imageName = $image.val() + $tag.val();

                    xmodal.loading.open({title: 'Submitting Pull Request',height: '110'});

                    XNAT.xhr.post({ url: '/xapi/docker/pull?save-commands=true&image='+imageName })
                        .success(
                            function() {
                                xmodal.closeAll();
                                refreshTable();
                                XNAT.ui.banner.top(2000, 'Pull request complete.', 'success');
                            })
                        .fail(
                            function(e) {
                                xmodal.closeAll();
                                xmodal.alert({ title: 'Error: Could Not Pull Image', content: 'Error '+ e.status+': '+e.statusText });
                            }
                        );
                }
            }
        });
    };

    // create a read-only code editor dialog to view a command definition
    commandDefinition.dialog = function(commandDef,newCommand){
        if (!newCommand) {
            commandDef = commandDef || {};

            var _source = spawn('textarea', JSON.stringify(commandDef, null, 4));

            var _editor = XNAT.app.codeEditor.init(_source, {
                language: 'json'
            });

            _editor.openEditor({
                title: commandDef.name,
                classes: 'plugin-json',
                footerContent: '(read-only)',
                buttons: {
                    close: { label: 'Close' },
                    info: {
                        label: 'View Command Info',
                        action: function(){
                            window.open(commandDef['info-url'],'infoUrl');
                        }
                    }
                },
                afterShow: function(dialog, obj){
                    obj.aceEditor.setReadOnly(true);
                }
            });
        } else {
            var _source = spawn('textarea', null);

            var _editor = XNAT.app.codeEditor.init(_source, {
                language: 'json'
            });

            _editor.openEditor({
                title: 'Add New Command',
                classes: 'plugin-json',
                buttons: {
                    save: {
                        label: 'Save Command',
                        action: function(){
                            var editorContent = _editor.getValue().code;
                            // editorContent = JSON.stringify(editorContent).replace(/\r?\n|\r/g,' ');

                            XNAT.xhr.postJSON({
                                url: commandUrl(),
                                dataType: 'json',
                                data: editorContent,
                                success: function(obj){
                                    refreshTable();
                                    xmodal.close(obj.$modal);
                                    XNAT.ui.banner.top(2000, 'Command definition saved.', 'success');
                                },
                                fail: function(e){
                                    xmodal.alert({ title: 'Error: Could Not Save', content: 'Error '+ e.status+': ' +e.statusText });
                                }
                            });
                        }
                    },
                    cancel: {
                        label: 'Cancel',
                        action: function(obj){
                            xmodal.close(obj.$modal);
                        }
                    }
                }
            });
        }
    };


    // create table for listing comands
    commandListManager.table = function(imageName,callback){

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

        function viewCommandButton(item){
            return spawn('button.btn.sm', {
                onclick: function(e){
                    e.preventDefault();
                    commandDefinition.dialog(item, false);
                }
            }, 'View Command');
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

        commandListManager.getAll(imageName).done(function(data) {
            if (data) {
                for (var i = 0, j = data.length; i < j; i++) {
                    var xnatContexts = '', item = data[i];
                    if (item.xnat) {
                        for (var k = 0, l = item.xnat.length; k < l; k++) {
                            if (xnatContexts.length > 0) xnatContexts += '<br>';
                            xnatContexts += item.xnat[k].description;
                        }
                    } else {
                        xnatContexts = 'N/A';
                    }
                    chTable.tr({title: item.name, data: {id: item.id, name: item.name, image: item.image}})
                        .td([viewLink(item, item.name)]).addClass('name')
                        .td(xnatContexts)
                        .td(item.version)
                        .td(item.image)
                        .td([['div.center', [viewCommandButton(item), spacer(10), deleteCommandButton(item)]]]);
                }
            } else {
                // create a handler when no command data is returned.
                chTable.tr({title: 'No command data found'})
                    .td({colSpan: '5', html: 'No Commands Found'});
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

        var newCommand = spawn('button.new-command.btn.sm', {
            html: 'Add New Command',
            onclick: function(){
                commandDefinition.dialog(null,true); // opens the command code editor dialog with "new command" set to true
            }
        });

        function deleteImageButton(image) {
            return spawn('button.btn.sm',{
                html: 'Delete Image',
                onclick: function(){
                    xmodal.confirm({
                        height: 220,
                        scroll: false,
                        content: "" +
                        "<p>Are you sure you'd like to delete the "+image.tags[0]+" image?</p>" +
                        "<p><strong>This action cannot be undone.</strong></p>",
                        okAction: function(){
                            console.log('delete image id', image['image-id']);
                            XNAT.xhr.delete({
                                url: imageUrl(image['image-id']),
                                success: function(){
                                    console.log(image.tags[0] + ' image deleted');
                                    XNAT.ui.banner.top(1000, '<b>' + image.tags[0] + ' image deleted.', 'success');
                                    refreshTable();
                                },
                                fail: function(e){
                                    xmodal.alert({ title: 'API Error', content: 'Error ' + e.status + ': ' + e.statusText });
                                }
                            })
                        }
                    })
                }
            });
        }

        imageListManager.container = $manager;

        imageListManager.getAll().done(function(data){
            for (var i=0, j=data.length; i<j; i++) {
                var imageInfo = data[i];
                $manager.append(spawn('div.imageContainer',[
                    ['h3.imageTitle',[imageInfo.tags[0], ['span.pull-right',[ deleteImageButton(imageInfo) ]]]],
                    ['div.imageCommandList',[commandListManager.table(imageInfo.tags[0])]]
                ]));
            }

            $manager.append(spawn('div',[ newCommand ]));
        });
    };


    function refreshTable(){
        imageListManager.container.html('');
        imageListManager.init();
    }

    // imageListManager.refresh = refreshTable;

    imageFilterManager.init();
    imageListManager.init();

//    return XNAT.admin.imageListManager = imageListManager;

}));
