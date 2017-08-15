/*
 * web: containerServices-siteAdmin.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Site-wide Admin UI functions for Container Services
 */

console.log('containerServices-siteAdmin.js');

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

/* ================ *
 * GLOBAL FUNCTIONS *
 * ================ */

    function spacer(width){
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        })
    }

    function errorHandler(e, title, closeAll){
        console.log(e);
        title = (title) ? 'Error Found: '+ title : 'Error';
        closeAll = (closeAll === undefined) ? true : closeAll;
        var errormsg = (e.statusText) ? '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p><p>' + e.responseText + '</p>' : e;
        XNAT.dialog.open({
            width: 450,
            title: title,
            content: errormsg,
            buttons: [
                {
                    label: 'OK',
                    isDefault: true,
                    close: true,
                    action: function(){
                        if (closeAll) {
                            xmodal.closeAll();

                        }
                    }
                }
            ]
        });
    }

    function csValidator(inputs){
        var errorMsg = [];

        if (inputs.length){
            inputs.forEach(function($input){
                if (!$input.val()){
                    errorMsg.push( spawn('li', [
                        spawn('strong', $input.prop('name')),
                        ' requires a value.'
                    ]) );
                    $input.addClass('invalid');
                }
            });

            if (errorMsg.length) {
                return spawn('div', [
                    spawn('p', 'Errors found:'),
                    spawn('ul', errorMsg)
                ]);
            }
        } else return false;
    }


/* ====================== *
 * Container Host Manager *
 * ====================== */

    console.log('containerHostManager.js');

    var containerHostManager,
        undefined,
        rootUrl = XNAT.url.rootUrl,
        csrfUrl = XNAT.url.csrfUrl;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.containerService =
        getObject(XNAT.plugin.containerService || {});

    XNAT.plugin.containerService.containerHostManager = containerHostManager =
        getObject(XNAT.plugin.containerService.containerHostManager || {});

    function containerHostUrl(appended){
        appended = isDefined(appended) ? '/' + appended : '';
        return rootUrl('/xapi/docker/server' + appended);
    }

    // get the list of hosts
    containerHostManager.getHosts = containerHostManager.getAll = function(callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: containerHostUrl(),
            dataType: 'json',
            success: function(data){
                containerHostManager.hosts = data;
                callback.apply(this, arguments);
            }
        });
    };

    // dialog to create/edit hosts
    containerHostManager.dialog = function(item, isNew){
        var tmpl = $('#container-host-editor-template').find('form').clone(),
            doWhat = (isNew) ? 'Create' : 'Edit';
        item = item || {};
        XNAT.dialog.open({
            title: doWhat + ' Container Server Host',
            content: spawn('form'),
            width: 450,
            beforeShow: function(obj){
                var $formContainer = obj.$modal.find('.xnat-dialog-content');
                $formContainer.addClass('panel');
                $formContainer.find('form').append(tmpl.html());

                if (item && isDefined(item.host)) {
                    $formContainer.find('form').setValues(item);
                }
            },
            buttons: [
                {
                    label: 'Save',
                    isDefault: true,
                    close: false,
                    action: function(obj){
                        var $form = obj.$modal.find('form');
                        var $host = $form.find('input[name=host]');

                        $form.find(':input').removeClass('invalid');

                        if (csValidator([$host])) {
                            XNAT.dialog.open({
                                title: 'Validation Error',
                                width: 300,
                                content: csValidator([$host])
                            })
                        } else {
                            XNAT.dialog.closeAll();
                            if (isNew) {
                                XNAT.dialog.open({
                                    content: spawn('div',[
                                        spawn('p','This will replace your existing host definition. Are you sure you want to do this?'),
                                        spawn('p', { 'style': { 'font-weight': 'bold' }}, 'This action cannot be undone.')
                                    ]),
                                    buttons: [
                                        {
                                            label: 'OK',
                                            close: true,
                                            isDefault: true,
                                            action: function () {
                                                submitHostEditor($form);
                                            }
                                        },
                                        {
                                            label: 'Cancel',
                                            action: function(){
                                                XNAT.dialog.closeAll();
                                            }
                                        }
                                    ]
                                });
                            } else {
                                submitHostEditor($form);
                            }
                        }
                    }
                },
                {
                    label: 'Cancel',
                    close: true
                }
            ]
        });
    };

    function submitHostEditor($form){
        $form.submitJSON({
            method: 'POST',
            url: containerHostUrl(),
            success: function(){
                containerHostManager.refreshTable();
                xmodal.closeAll();
                XNAT.ui.banner.top(2000, 'Saved.', 'success')
            }
        });
    }

    // create table for Container Hosts
    containerHostManager.table = function(container, callback){

        // initialize the table - we'll add to it below
        var chmTable = XNAT.table({
            className: 'container-hosts xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        chmTable.tr()
            .th({ addClass: 'left', html: '<b>Host Name</b>' })
            .th('<b>Host Path</b>')
            .th('<b>Default</b>')
            .th('<b>Status</b>')
            .th('<b>Actions</b>');

        function editLink(item, text){
            return spawn('a.link|href=#!', {
                onclick: function(e){
                    e.preventDefault();
                    containerHostManager.dialog(item, false);
                }
            }, [['b', text]]);
        }

        function defaultToggle(item){
            var rdo = spawn('input.container-host-enabled', {
                type: 'radio',
                name: 'defaultHost',
                checked: 'checked',
                value: 'default',
                data: { id: item.id, name: item.name },
                onchange: function(){
                    // save the status when clicked
                    var radio = this;
                    xmodal.alert('Cannot set default server yet');
                    /*
                    defaultVal = radio.checked;
                    XNAT.xhr.post({
                        url: containerHostUrl(item.id+'?default=true'),
                        success: function(){
                            radio.value = defaultVal;
                            radio.checked = 'checked';
                            containerHostManager.refreshTable();
                            XNAT.ui.banner.top(1000, '<b>' + item.name + '</b> set as default', 'success');
                        },
                            fail: function(e){
                            radio.checked = false;
                            containerHostManager.refreshTable();
                            errorHandler(e);
                        }
                    });
                    */
                }
            });
            return spawn('div.center', [rdo]);
        }

        function editButton(item) {
            return spawn('button.btn.sm.edit', {
                onclick: function(e){
                    e.preventDefault();
                    containerHostManager.dialog(item, false);
                }
            }, 'Edit');
        }

        function hostPingStatus(ping) {
            var status = {};
            if (ping !== undefined) {
                status = (ping) ? { label: 'OK', message: 'Ping Status: OK'} :  { label: 'Down', message: 'Ping Status: FALSE' };
            } else {
                status = { label: 'Error', message: 'No response to ping' };
            }
            return spawn('span', { title: status.message }, status.label);
        }

        containerHostManager.getAll().done(function(data){
            data = [].concat(data);
            data.forEach(function(item){
                chmTable.tr({ title: item.name, data: { id: item.id, host: item.host, certPath: item.certPath}})
                    .td([editLink(item, item.name)]).addClass('host')
                    .td([ spawn('div.center', [item.host]) ])
                    .td([ spawn('div.center', [defaultToggle(item)]) ])
                    .td([ spawn('div.center', [hostPingStatus(item.ping)]) ])
                    .td([ spawn('div.center', [editButton(item)]) ])
            });

            if (container){
                $$(container).append(chmTable.table);
            }

            if (isFunction(callback)) {
                callback(chmTable.table);
            }

        });

        containerHostManager.$table = $(chmTable.table);

        return chmTable.table;
    };

    containerHostManager.init = function(container){

        var $manager = $$(container||'div#container-host-manager');
        var $footer = $('#container-host-manager').parents('.panel').find('.panel-footer');

        containerHostManager.$container = $manager;

        $manager.append(containerHostManager.table());
        // containerHostManager.table($manager);

        var newReceiver = spawn('button.new-container-host.btn.btn-sm.submit', {
            html: 'New Container Host',
            onclick: function(){
                containerHostManager.dialog(null, true);
            }
        });


        // add the 'add new' button to the panel footer
        $footer.append(spawn('div.pull-right', [
            newReceiver
        ]));
        $footer.append(spawn('div.clear.clearFix'));


        return {
            element: $manager[0],
            spawned: $manager[0],
            get: function(){
                return $manager[0]
            }
        };
    };

    containerHostManager.refresh = containerHostManager.refreshTable = function(){
        containerHostManager.$table.remove();
        containerHostManager.table(null, function(table){
            containerHostManager.$container.prepend(table);
        });
    };

    containerHostManager.init();




/* ===================== *
 * Image Host Management *
 * ===================== */

    console.log('imageHostManagement.js');

    var imageHostManager;

    XNAT.plugin.containerService.imageHostManager = imageHostManager =
        getObject(XNAT.plugin.containerService.imageHostManager || {});

    function imageHostUrl(isDefault,appended){
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
            url: imageHostUrl(),
            dataType: 'json',
            success: function(data){
                imageHostManager.hosts = data;
                callback.apply(this, arguments);
            }
        });
    };

    // dialog to create/edit hosts
    imageHostManager.dialog = function(item, isNew){
        var tmpl = $('#image-host-editor-template').find('form');
        isNew = isNew || false;
        var doWhat = (isNew) ? 'Create' : 'Edit';
        item = item || {};

        XNAT.dialog.open({
            title: doWhat + ' Image Hub',
            content: spawn('form'),
            width: 550,
            beforeShow: function(obj){
                var $formContainer = obj.$modal.find('.xnat-dialog-content');
                $formContainer.addClass('panel').find('form').append(tmpl.html());
                if (item && isDefined(item.url)) {
                    $formContainer.find('form').setValues(item);
                }
            },
            buttons: [
                {
                    label: 'Save',
                    isDefault: true,
                    close: false,
                    action: function(obj){
                        var $form = obj.$modal.find('form');
                        var $url = $form.find('input[name=url]');
                        var $name = $form.find('input[name=name]');
                        var isDefault = $form.find('input[name=default]').val();

                        $form.find(':input').removeClass('invalid');

                        if (csValidator([$url,$name])) {
                            XNAT.dialog.open({
                                title: 'Validation Error',
                                width: 300,
                                content: csValidator([$url,$name])
                            })
                        } else {
                            xmodal.loading.open({ title: 'Validating host URL'});
                            $form.submitJSON({
                                method: 'POST',
                                url: (isNew) ? imageHostUrl(isDefault) : imageHostUrl(isDefault, item.id),
                                success: function () {
                                    imageHostManager.refreshTable();
                                    xmodal.loading.close();
                                    XNAT.dialog.close(obj.$modal);
                                    XNAT.ui.banner.top(2000, 'Saved.', 'success')
                                },
                                fail: function (e) {
                                    xmodal.loading.close();
                                    errorHandler(e);
                                }
                            });
                        }
                    }
                }
            ]
        });

    };

    // create table for Image Hosts
    imageHostManager.table = function(imageHost, callback){

        // initialize the table - we'll add to it below
        var ihmTable = XNAT.table({
            className: 'image-hosts xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        ihmTable.tr()
            .th({ addClass: 'left', html: '<b>ID</b>' })
            .th('<b>Name</b>')
            .th('<b>URL</b>')
            .th('<b>Default</b>')
            .th('<b>Status</b>')
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
                        url: imageHostUrl(true,item.id),
                        success: function(){
                            radio.value = defaultVal;
                            radio.checked = 'checked';
                            imageHostManager.refreshTable();
                            XNAT.ui.banner.top(1000, '<b>' + item.name + '</b> set as default', 'success');
                        },
                        fail: function(e){
                            radio.checked = false;
                            imageHostManager.refreshTable();
                            errorHandler(e,'Could Not Set Default Hub');
                        }
                    });
                }
            });
            return spawn('div.center', [rdo]);
        }

        function isDefault(status,valIfTrue) {
            return (status) ? valIfTrue : false;
        }

        function hubPingStatus(ping) {
            var status = {};
            if (ping !== undefined) {
                status = (ping) ? { label: 'OK', message: 'Ping Status: OK' } : { label: 'Down', message: 'Ping Status: False' };
            } else {
                status = { label: 'Error', message: 'No response to ping' };
            }
            return spawn('span',{ title: status.message }, status.label);
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
                            XNAT.xhr.delete({
                                url: imageHostUrl(false,item.id),
                                success: function(){
                                    XNAT.ui.banner.top(1000, '<b>"'+ item.url + '"</b> deleted.', 'success');
                                    imageHostManager.refreshTable();
                                },
                                fail: function(e){
                                    errorHandler(e);
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
                ihmTable.tr({ title: item.name, data: { id: item.id, name: item.name, url: item.url}})
                    .td( item.id )
                    .td([ editLink(item, item.name) ]).addClass('name')
                    .td( item.url )
                    .td([ defaultToggle(item)] ).addClass('status')
                    .td([ spawn('div.center', [hubPingStatus(item.ping)]) ])
                    .td([ spawn('div.center', [editButton(item), spacer(10), deleteButton(item)]) ]);
            });

            if (imageHost){
                $$(imageHost).empty().append(ihmTable.table);
            }

            if (isFunction(callback)) {
                callback(ihmTable.table);
            }

        });

        imageHostManager.$table = $(ihmTable.table);

        return ihmTable.table;
    };

    imageHostManager.init = function(container){

        var $manager = $$(container||'div#image-host-manager');
        var $footer = $('#image-host-manager').parents('.panel').find('.panel-footer');

        imageHostManager.container = $manager;

        $manager.append(imageHostManager.table());
        // imageHostManager.table($manager);

        var newReceiver = spawn('button.new-image-host.btn.btn-sm.submit', {
            html: 'New Image Hub',
            onclick: function(){
                imageHostManager.dialog(null, true);
            }
        });

        // add the 'add new' button to the panel footer
        $footer.append(spawn('div.pull-right', [
            newReceiver
        ]));
        $footer.append(spawn('div.clear.clearFix'));

        return {
            element: $manager[0],
            spawned: $manager[0],
            get: function(){
                return $manager[0]
            }
        };
    };

    imageHostManager.refresh = imageHostManager.refreshTable = function(container){
        var $manager = $$(container||'div#image-host-manager');

        imageHostManager.$table.remove();
        $manager.append('Loading...');
        imageHostManager.table(null, function(table){
            $manager.empty().prepend(table);
        });
    };

    imageHostManager.init();




/* ===================== *
 * Image List Management *
 * ===================== */

    console.log('imageListManagement.js');

    var imageListManager,
        imageFilterManager,
        addImage,
        commandListManager,
        commandDefinition,
        wrapperList,
        imageHubs;

    XNAT.plugin.containerService.imageListManager = imageListManager =
        getObject(XNAT.plugin.containerService.imageListManager || {});

    XNAT.plugin.containerService.imageFilterManager = imageFilterManager =
        getObject(XNAT.plugin.containerService.imageFilterManager || {});

    XNAT.plugin.containerService.addImage = addImage =
        getObject(XNAT.plugin.containerService.addImage || {});

    XNAT.plugin.containerService.commandListManager = commandListManager =
        getObject(XNAT.plugin.containerService.commandListManager || {});

    XNAT.plugin.containerService.commandDefinition = commandDefinition =
        getObject(XNAT.plugin.containerService.commandDefinition || {});

    XNAT.plugin.containerService.imageHubs = imageHubs =
        getObject(XNAT.plugin.containerService.imageHubs || {});

    XNAT.plugin.containerService.wrapperList = wrapperList = {};

    imageListManager.samples = [
        {
            "label": "Docker Hub",
            "image_id": "https://hub.docker.com",
            "enabled": true
        }
    ];

    function imageUrl(appended,force){
        appended = (appended) ? '/' + appended : '';
        force = (force) ? '?force=true' : '';
        return rootUrl('/xapi/docker/images' + appended + force);
    }

    function commandUrl(appended){
        appended = isDefined(appended) ? appended : '';
        return csrfUrl('/xapi/commands' + appended);
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
        var tmpl = $('#add-image-template').find('form').clone();
        item = item || {};
        XNAT.dialog.open({
            title: 'Pull New Image',
            content: spawn('form'),
            width: 500,
            padding: 0,
            beforeShow: function(obj){
                var $formContainer = obj.$modal.find('.xnat-dialog-content');
                $formContainer.addClass('panel').find('form').append(tmpl.html());

                if (item && isDefined(item.image)) {
                    $formContainer.setValues(item);
                }
                var $hubSelect = $formContainer.find('#hub-id');
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
                        $hubSelect.parents('.panel-element').hide();
                    }
                });
            },
            buttons: [
                {
                    label: 'Pull Image',
                    isDefault: true,
                    close: false,
                    action: function(obj){
                        var $form = obj.$modal.find('form');
                        var $image = $form.find('input[name=image]');
                        var $tag = $form.find('input[name=tag]');

                        // validate form inputs, then pull them into the URI querystring and create an XHR request.
                        $form.find(':input').removeClass('invalid');

                        if (csValidator([$image,$tag])) {
                            XNAT.dialog.open({
                                title: 'Validation Error',
                                width: 300,
                                content: csValidator([$image,$tag])
                            })
                        } else {
                            // stitch together the image and tag definition, if a tag value was specified.
                            if ($tag.val().length > 0 && $tag.val().indexOf(':') < 0) {
                                $tag.val(':' + $tag.val());
                            }
                            var imageName = $image.val() + $tag.val();

                            xmodal.loading.open({ title: 'Submitting Pull Request' });

                            XNAT.xhr.post({
                                url: '/xapi/docker/pull?save-commands=true&image='+imageName,
                                success: function() {
                                    xmodal.loading.close();
                                    XNAT.dialog.closeAll();
                                    imageListManager.refreshTable();
                                    commandConfigManager.refreshTable();
                                    XNAT.ui.banner.top(2000, 'Pull request complete.', 'success');
                                },
                                fail: function(e) {
                                    errorHandler(e, 'Could Not Pull Image');
                                }
                            })
                        }
                    }
                },
                {
                    label: 'Cancel',
                    close: true
                }
            ]
        });
    };

    // create a read-only code editor dialog to view a command definition
    commandDefinition.dialog = function(commandDef,newCommand,imageName){
        var _source,_editor;
        if (!newCommand) {
            commandDef = commandDef || {};

            _source = spawn('textarea', JSON.stringify(commandDef, null, 4));

            _editor = XNAT.app.codeEditor.init(_source, {
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
            _source = spawn('textarea', '{}');

            _editor = XNAT.app.codeEditor.init(_source, {
                language: 'json'
            });

            _editor.openEditor({
                title: 'Add New Command to '+imageName,
                classes: 'plugin-json',
                buttons: {
                    save: {
                        label: 'Save Command',
                        action: function(){
                            var editorContent = _editor.getValue().code;
                            // editorContent = JSON.stringify(editorContent).replace(/\r?\n|\r/g,' ');

                            var url = (imageName) ? commandUrl('?image='+imageName) : commandUrl();

                            XNAT.xhr.postJSON({
                                url: url,
                                dataType: 'json',
                                data: editorContent,
                                success: function(obj){
                                    imageListManager.refreshTable();
                                    commandConfigManager.refreshTable();
                                    xmodal.close(obj.$modal);
                                    XNAT.ui.banner.top(2000, 'Command definition saved.', 'success');
                                },
                                fail: function(e){
                                    errorHandler(e, 'Could Not Save', false);
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


    // create table for listing commands
    commandListManager.table = function(imageName,callback){

        // initialize the table - we'll add to it below
        var clmTable = XNAT.table({
            className: 'enabled-commands xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        clmTable.tr()
            .th({ addClass: 'left', html: '<b>Command</b>' })
            .th('<b>XNAT Actions</b>')
            .th('<b>Site-wide Config</b>')
            .th('<b>Version</b>')
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
                                url: commandUrl('/'+item.id),
                                success: function(){
                                    console.log('"'+ item.name + '" command deleted');
                                    XNAT.ui.banner.top(1000, '<b>"'+ item.name + '"</b> deleted.', 'success');
                                    imageListManager.refreshTable();
                                    commandConfigManager.refreshTable();
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
                    var xnatActions = '', command = data[i];
                    if (command.xnat) {
                        for (var k = 0, l = command.xnat.length; k < l; k++) {
                            if (xnatActions.length > 0) xnatActions += '<br>';
                            xnatActions += command.xnat[k].description;
                            if (command.xnat[k].contexts.length > 0) {
                                var contexts = command.xnat[k].contexts;
                                xnatActions += "<ul>";
                                contexts.forEach(function(context){
                                    xnatActions +="<li>"+context+"</li>";
                                });
                                xnatActions += "</ul>";
                            }
                        }
                    } else {
                        xnatActions = 'N/A';
                    }
                    clmTable.tr({title: command.name, data: {id: command.id, name: command.name, image: command.image}})
                        .td([viewLink(command, command.name)]).addClass('name')
                        .td(xnatActions)
                        .td('N/A')
                        .td(command.version)
                        .td([ spawn('div.center', [viewCommandButton(command), spacer(10), deleteCommandButton(command)]) ]);
                }
            } else {
                // create a handler when no command data is returned.
                clmTable.tr({title: 'No command data found'})
                    .td({colSpan: '5', html: 'No Commands Found'});
            }
        });

        commandListManager.$table = $(clmTable.table);

        return clmTable.table;
    };

    imageFilterManager.init = function(container){

        var $manager = $$(container||'div#image-filter-bar');
        var $footer = $('#image-filter-bar').parents('.panel').find('.panel-footer');

        imageFilterManager.container = $manager;

        var newImage = spawn('button.new-image.btn.btn-sm.submit', {
            html: 'Add New Image',
            onclick: function(){
                addImage.dialog(null);
            }
        });

        // add the 'add new' button to the panel footer
        $footer.append(spawn('div.pull-right', [
            newImage
        ]));
        $footer.append(spawn('div.clear.clearFix'));

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

        function newCommandButton(image) {
            return spawn('button.btn.sm',{
                html: 'Add New Command',
                onclick: function(){
                    commandDefinition.dialog(null,true,image.tags[0])
                }
            });
        }

        function deleteImage(image,force) {
            var content;
            force = !!(force);
            if (!force) {
                content = spawn('div',[
                    spawn('p','Are you sure you\'d like to delete the '+image.tags[0]+' image?'),
                    spawn('p', [ spawn('strong', 'This action cannot be undone.' )])
                ]);
            } else {
                content = spawn('p','Containers have been run using '+image.tags[0]+'. Please confirm that you want to delete this image.');
            }
            XNAT.dialog.open({
                width: 400,
                content: content,
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: false,
                        action: function(){
                            XNAT.xhr.delete({
                                url: imageUrl(image['image-id'],force),
                                success: function(){
                                    console.log(image.tags[0] + ' image deleted');
                                    XNAT.ui.banner.top(1000, '<b>' + image.tags[0] + ' image deleted.', 'success');
                                    imageListManager.refreshTable();
                                    XNAT.dialog.closeAll();
                                },
                                fail: function(e){
                                    if (e.status === 500) {
                                        XNAT.dialog.closeAll();
                                        deleteImage(image,true);
                                    } else {
                                        errorHandler(e);
                                    }
                                }
                            })
                        }
                    },
                    {
                        label: 'Cancel',
                        close: true
                    }
                ]

            });
        }

        function deleteImageButton(image) {
            return spawn('button.btn.sm',{
                html: 'Delete Image',
                onclick: function(){
                    deleteImage(image);
                }
            });
        }

        imageListManager.container = $manager;

        imageListManager.getAll().done(function(data){
            if (data.length > 0) {
                for (var i=0, j=data.length; i<j; i++) {
                    var imageInfo = data[i];
                    $manager.append(spawn('div.imageContainer',[
                        spawn('h3.imageTitle',[
                            (imageInfo.tags[0])=="<none>:<none>"?imageInfo['image-id']:imageInfo.tags[0],
                            spawn( 'span.pull-right',[
                                deleteImageButton(imageInfo)
                            ]),
                            spawn( 'span.pull-right.pad10h',[
                                newCommandButton(imageInfo)
                            ])
                        ]),
                        spawn('div.imageCommandList',[
                            commandListManager.table(imageInfo.tags[0])
                        ])
                    ]));
                }
            } else {
                $manager.append(spawn('p',['There are no images installed in this XNAT.']));
            }

        });
    };


    imageListManager.refresh = imageListManager.refreshTable = function(container){
        container = $$(container || 'div#image-list-container');
        container.html('');
        imageListManager.init();
    };

    imageFilterManager.init();
    imageListManager.init();




/* ================================ *
 * Command Configuration Management *
 * ================================ */

    console.log('commandConfigManagement.js');

    var commandConfigManager,
        configDefinition;

    XNAT.plugin.containerService.commandConfigManager = commandConfigManager =
        getObject(XNAT.plugin.containerService.commandConfigManager || {});

    XNAT.plugin.containerService.configDefinition = configDefinition =
        getObject(XNAT.plugin.containerService.configDefinition || {});


    function configUrl(command,wrapperName,appended){
        appended = isDefined(appended) ? '?' + appended : '';
        if (!command || !wrapperName) return false;
        return csrfUrl('/xapi/commands/'+command+'/wrappers/'+wrapperName+'/config' + appended);
    }

    function configEnableUrl(commandObj,wrapperObj,flag){
        var command = commandObj.id,
            wrapperName = wrapperObj.name;
        return csrfUrl('/xapi/commands/'+command+'/wrappers/'+wrapperName+'/' + flag);
    }

    function siteConfigUrl(){
        return csrfUrl('/xapi/siteConfig');
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
            },
            fail: function (e) {
                errorHandler(e);
            }
        });
    };

    commandConfigManager.getEnabledStatus = function(command,wrapper,callback){
        callback = isFunction(callback) ? callback : function(){};
        return XNAT.xhr.get({
            url: configEnableUrl(command,wrapper,'enabled'),
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
        var cceditTable = XNAT.table({
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
            return XNAT.ui.input.hidden({
                name: name,
                value: value
            }).element;
        }


        // determine which type of table to build.
        if (config.type === 'inputs') {
            var inputs = config.inputs;

            // add table header row
            cceditTable.tr()
                .th({ addClass: 'left', html: '<b>Input</b>' })
                .th('<b>Default Value</b>')
                .th('<b>Matcher Value</b>')
                .th('<b>User-Settable?</b>')
                .th('<b>Advanced?</b>');

            for (i in inputs) {
                var input = inputs[i];
                cceditTable.tr({ data: { input: i }, className: 'input' })
                    .td( { data: { key: 'key' }, addClass: 'left'}, i )
                    .td( { data: { key: 'property', property: 'default-value' }}, basicConfigInput('defaultVal',input['default-value']) )
                    .td( { data: { key: 'property', property: 'matcher' }}, basicConfigInput('matcher',input['matcher']) )
                    .td( { data: { key: 'property', property: 'user-settable' }}, [['div', [configCheckbox('userSettable',input['user-settable']) ]]])
                    .td( { data: { key: 'property', property: 'advanced' }}, [['div', [configCheckbox('advanced',input['advanced']) ]]]);

            }

        } else if (config.type === 'outputs') {
            var outputs = config.outputs;

            // add table header row
            cceditTable.tr()
                .th({ addClass: 'left', html: '<b>Output</b>' })
                .th({ addClass: 'left', width: '75%', html: '<b>Label</b>' });

            for (o in outputs) {
                var output = outputs[o];
                cceditTable.tr({ data: { output: o }, className: 'output' })
                    .td( { data: { key: 'key' }, addClass: 'left'}, o )
                    .td( { data: { key: 'property', property: 'label' }}, basicConfigInput('label',output['label']) );
            }

        }

        configDefinition.$table = $(cceditTable.table);

        return cceditTable.table;
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
                            fail: function(e){ errorHandler(e); }
                        });
                    }
                });

            })
            .fail(function(e){
                errorHandler(e, 'Could Not Open Config Definition');
            });

    };


    commandConfigManager.table = function(){

        // initialize the table - we'll add to it below
        var ccmTable = XNAT.table({
            className: 'sitewide-command-configs xnat-table',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        ccmTable.tr()
            .th({ addClass: 'left', html: '<b>XNAT Command Label</b>' })
            .th('<b>Container</b>')
            .th('<b>Enabled</b>')
            .th({ width: 150, html: '<b>Actions</b>' });

        // add master switch
        ccmTable.tr({ 'style': { 'background-color': '#f3f3f3' }})
            .td({className: 'name', html: 'Enable / Disable All Commands', colSpan: 2 })
            .td([ spawn('div',[masterCommandCheckbox()]) ])
            .td();

        function viewLink(item, wrapper, text){
            return spawn('a.link|href=#!', {
                onclick: function(e){
                    e.preventDefault();
                    configDefinition.dialog(item.id, wrapper.name, false);
                }
            }, [['b', text]]);
        }

        function editConfigButton(item,wrapper){
            return spawn('button.btn.sm', {
                onclick: function(e){
                    e.preventDefault();
                    configDefinition.dialog(item.id, wrapper.name, false);
                }
            }, 'Set Defaults');
        }

        function enabledCheckbox(command,wrapper){
            commandConfigManager.getEnabledStatus(command,wrapper).done(function(data){
                var enabled = data;
                $('#wrapper-'+wrapper.id+'-enable').prop('checked',enabled);
                commandConfigManager.setMasterEnableSwitch();
            });

            var ckbox = spawn('input.config-enabled.wrapper-enable', {
                type: 'checkbox',
                checked: false,
                value: 'true',
                id: 'wrapper-'+wrapper.id+'-enable',
                data: { name: wrapper.name },
                onchange: function(){
                    // save the status when clicked
                    var checkbox = this;
                    enabled = checkbox.checked;
                    var enabledFlag = (enabled) ? 'enabled' : 'disabled';

                    XNAT.xhr.put({
                        url: configEnableUrl(command,wrapper,enabledFlag),
                        success: function(){
                            var status = (enabled ? ' enabled' : ' disabled');
                            checkbox.value = enabled;
                            XNAT.ui.banner.top(1000, '<b>' + wrapper.name+ '</b> ' + status, 'success');
                        },
                        fail: function(e){
                            errorHandler(e);
                        }
                    });

                    commandConfigManager.setMasterEnableSwitch();
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

        commandConfigManager.getAll().done(function(data) {
            if (data) {
                for (var i = 0, j = data.length; i < j; i++) {
                    var command = data[i];
                    if (command.xnat) {
                        for (var k = 0, l = command.xnat.length; k < l; k++) {
                            var wrapper = command.xnat[k];
                            ccmTable.tr({title: wrapper.name, data: {wrapperid: wrapper.id, commandid: command.id, name: wrapper.name, image: command.image}})
                                .td([ viewLink(command, wrapper, wrapper.description) ]).addClass('name')
                                .td([ spawn('span.truncate.truncate200', command.image ) ])
                                .td([ spawn('div', [enabledCheckbox(command,wrapper)]) ])
                                .td([ spawn('div.center', [editConfigButton(command,wrapper)]) ]);
                        }
                    }
                }
            } else {
                // create a handler when no command data is returned.
                ccmTable.tr({title: 'No command config data found'})
                    .td({colSpan: '5', html: 'No XNAT-enabled Commands Found'});
            }
        });

        commandConfigManager.$table = $(ccmTable.table);

        return ccmTable.table;
    };

    // examine all command toggles and set master switch to "ON" if all are checked
    commandConfigManager.setMasterEnableSwitch = function(){
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

    // auto-save to site-wide opt-in preference on click of the switchbox.
    $('#opt-into-sitewide-commands').on('change',function(){
        var optIn = $(this).prop('checked');
        var paramToPut = JSON.stringify({ optIntoSitewideCommands: optIn });
        XNAT.xhr.post({
            url: siteConfigUrl(),
            data: paramToPut,
            dataType: 'json',
            contentType: 'application/json',
            success: function(){
                XNAT.ui.banner.top(1000, '<b>Success: </b> Default Project Opt-in Preference Setting set to <b>' + optIn + '</b>.', 'success');
            },
            fail: function(e){
                errorHandler(e,'Could not set site-wide project preference for command opt-in');
            }
        })
    });

    commandConfigManager.refresh = commandConfigManager.refreshTable = function(container){
        var $manager = $$(container||'div#command-config-list-container');

        $manager.html('');
        $manager.append(commandConfigManager.table());
        commandConfigManager.setMasterEnableSwitch();
    };

    commandConfigManager.init = function(container){
        var $manager = $$(container||'div#command-config-list-container');

        commandConfigManager.container = $manager;

        $manager.append(commandConfigManager.table());
    };

    commandConfigManager.init();

    /* ================================= *
     * Command Automation Administration *
     * ================================= */

    console.log('commandAutomationAdmin.js');

    var commandAutomationAdmin, projectList;

    XNAT.plugin.containerService.commandAutomation = commandAutomationAdmin =
        getObject(XNAT.plugin.containerService.commandAutomation || {});

    XNAT.plugin.containerService.projectList = projectList = [];

    function getProjectListUrl(){
        return rootUrl('/data/projects?format=json');
    }
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

    commandAutomationAdmin.getProjects = function(callback){
        callback = isFunction(callback) ? callback : function(){};

        return XNAT.xhr.getJSON({
            url: getProjectListUrl(),
            success: function(data){
                if (data){
                    projectList = data.ResultSet.Result;
                    return projectList;
                }
                callback.apply(this, arguments);
            },
            fail: function(e){
                errorHandler(e,'Could not get project list.');
            }
        });
    };

    commandAutomationAdmin.deleteAutomation = function(id){
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

    commandAutomationAdmin.addDialog = function(){
        // get all commands and wrappers, then open a dialog to allow user to configure an automation.

        function projectSelector(name,label){
            name = (name) ? name : 'project';
            label = (label) ? label : 'For Project';
            var projectOptions = {};

            // build options object for standard XNAT panel select
            projectList.forEach(function(project){
                projectOptions[project.ID] = project['secondary_ID'];
            });

            return XNAT.ui.panel.select.single({
                name: name,
                label: label,
                options: projectOptions
            });
        }

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

        commandConfigManager.getAll().done(function(data) {
            if (data.length && projectList.length) {

                // build array of commands that can be selected
                var commandOptions = [];
                data.forEach(function(command){
                    command.xnat.forEach(function(wrapper){
                        commandOptions.push({
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

                if (Object.keys(commandOptions).length > 0) {
                    XNAT.ui.dialog.open({
                        title: 'Create Command Automation',
                        width: 500,
                        content: '<div class="panel pad20"></div>',
                        beforeShow: function(obj){
                            // populate form elements
                            var panel = obj.$modal.find('.panel');
                            panel.append( spawn('p','Please enter values for each field.') );
                            panel.append( projectSelector() );
                            panel.append( XNAT.ui.panel.select.single({
                                name: 'event-type',
                                label: 'On Event',
                                options: eventOptions
                            }));
                            panel.append( eventCommandSelector(
                                'xnat-command-wrapper',
                                commandOptions,
                                'Run Command')
                            );
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
                                        project = panel.find('select[name=project]').find('option:selected').val(),
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
                                        console.log(project,command,wrapper,event);
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
                errorHandler({},'Could not add a command. Check the command list.');

            }
        });
    };

    commandAutomationAdmin.table = function(){
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
            .th('<b>Project</b>')
            .th('<b>Event</b>')
            .th('<b>Command</b>')
            .th('<b>Created By</b>')
            .th('<b>Enabled</b>')
            .th('<b>Action</b>');

        function displayDate(timestamp){
            var d = new Date(timestamp);
            return d.toISOString().replace('T',' ').replace('Z',' ');
        }

        function deleteAutomationButton(id){
            return spawn('button.deleteAutomationButton',{ data: { id: id }, html: 'Delete' });
        }

        XNAT.xhr.getJSON({
            url: getCommandAutomationUrl(),
            fail: function(e){
                errorHandler(e);
            },
            success: function(data){
                // data returns an array of known command event mappings
                if (data.length){
                    data.forEach(function(mapping){
                        caTable.tr()
                            .td( '<b>'+mapping['id']+'</b>' )
                            .td( mapping['project'] )
                            .td( mapping['event-type'] )
                            .td( mapping['xnat-command-wrapper'] )
                            .td( mapping['subscription-user-name'] )
                            .td( mapping['enabled'] )
                            .td([ deleteAutomationButton(mapping['id']) ])
                    });

                } else {
                    caTable.tr()
                        .td({ colSpan: '7', html: 'No command event mappings exist for this project.' });
                }
            }
        });

        commandAutomationAdmin.$table = $(caTable.table);

        return caTable.table;
    };

    commandAutomationAdmin.init = function(refresh){
        // initialize the list of command automations
        var manager = $('#command-automation-admin-list');
        var $footer = manager.parents('.panel').find('.panel-footer');

        manager.html('');
        manager.append(commandAutomationAdmin.table());

        if (!refresh) {
            commandAutomationAdmin.getProjects().done(function(){
                var newAutomation = spawn('button.new-command-automation.btn.btn-sm.submit', {
                    html: 'Add New Command Automation',
                    onclick: function(){
                        commandAutomationAdmin.addDialog();
                    }
                });

                // add the 'add new' button to the panel footer
                $footer.append(spawn('div.pull-right', [
                    newAutomation
                ]));
                $footer.append(spawn('div.clear.clearFix'));
            });
        }
    };

    commandAutomationAdmin.init();


/* =============== *
 * Command History *
 * =============== */

    console.log('commandHistory.js');

    var historyTable, containerHistory;

    XNAT.plugin.containerService.historyTable = historyTable =
        getObject(XNAT.plugin.containerService.historyTable || {});

    XNAT.plugin.containerService.containerHistory = containerHistory =
        getObject(XNAT.plugin.containerService.containerHistory || {});

    function getCommandHistoryUrl(appended){
        appended = (appended) ? '?'+appended : '';
        return rootUrl('/xapi/containers' + appended);
    }

    historyTable.table = function(callback){
        // initialize the table - we'll add to it below
        var chTable = XNAT.table({
            className: 'sitewide-command-configs xnat-table compact',
            style: {
                width: '100%',
                marginTop: '15px',
                marginBottom: '15px'
            }
        });

        // add table header row
        chTable.tr()
            .th({ addClass: 'left', html: '<b>ID</b>' })
            .th('<b>Image</b>')
            .th('<b>Command</b>')
            .th('<b>User</b>')
            .th('<b>Date</b>')
            .th('<b>Project</b>');

        function displayDate(timestamp){
            var d = new Date(timestamp);
            return d.toISOString().replace('T',' ').replace('Z',' ');
        }

        function displayInput(inputObj){
            for (var i in inputObj){
                if (i=="scan") {
                    var sessionId = inputObj[i].split('/')[2];
                    var scanId = inputObj[i].split('/scans/')[1];
                    return spawn(['a|href='+rootUrl('/data/experiments/'+sessionId), sessionId+': '+scanId ]);
                }
            }
        }

        function displayProject(mounts){
            // assume that the first mount of a container is an input from a project. Parse the URI for that mount and return the project ID.
            if (mounts.length) {
                var inputMount = mounts[0]['xnat-host-path'];
                if (inputMount === undefined) return 'unknown';

                inputMount = inputMount.replace('/data/xnat/archive/','');
                inputMount = inputMount.replace('/data/archive/','');
                inputMount = inputMount.replace('/REST/archive/','');
                var inputMountEls = inputMount.split('/');
                return spawn('a',{ href: '/data/projects/'+ inputMountEls[0] + '?format=html', html: inputMountEls[0] });
            } else {
                return 'unknown';
            }

        }

        function displayOutput(outputArray){
            var o = outputArray[0];
            return o.label;
        }

        function displayCommandWithPopup(historyEntry){
            var commandLabel = XNAT.plugin.containerService.wrapperList[historyEntry['wrapper-id']];
            return spawn ('a',{
                href: 'javascript:XNAT.plugin.containerService.historyTable.viewHistory(\''+historyEntry['id']+'\')',
                title: 'View Full History Entry',
                html: commandLabel
            });
        }

        commandListManager.getAll().done(function(commands){
            // populate the list of wrappers
            commands.forEach(function(command){
                var wrappers = command.xnat;
                wrappers.forEach(function(wrapper){
                    XNAT.plugin.containerService.wrapperList[wrapper.id] = wrapper.description;
                })

            });

            XNAT.xhr.getJSON({
                url: getCommandHistoryUrl(),
                fail: function(e){
                    errorHandler(e);
                },
                success: function(data){
                    if (data.length > 0) {
                        data.sort(function(a,b){
                            return (a.id < b.id) ? -1 : 1;
                        });

                        data.forEach(function(historyEntry){
                            containerHistory[historyEntry['id']] = historyEntry;
                            containerHistory[historyEntry['id']]['wrapper-name'] = XNAT.plugin.containerService.wrapperList[historyEntry['wrapper-id']];

                            var timestamp = 0;
                            historyEntry['history'].forEach(function(h){
                                if(h['status'] == 'Created') {
                                    timestamp = h['time-recorded'];
                                }
                            })

                            chTable.tr({title: historyEntry['id'], id: historyEntry['id'] })
                                .td({ addClass: 'left', html: '<b>'+historyEntry['id']+'</b>' })
                                .td(historyEntry['docker-image'])
                                .td([ displayCommandWithPopup(historyEntry) ])
                                .td(historyEntry['user-id'])
                                .td([ displayDate(timestamp) ])
                                .td([ displayProject(historyEntry['mounts']) ]);
                        });
                    } else {
                        chTable.tr()
                            .td({ colspan: 7, html: "No history entries found" });
                    }

                }
            });

        });

        historyTable.$table = $(chTable.table);

        return chTable.table;
    };

    historyTable.viewHistory = function(id){
        if (containerHistory[id]) {
            var historyEntry = XNAT.plugin.containerService.containerHistory[id];

            // build nice-looking history entry table
            var pheTable = XNAT.table({
                className: 'xnat-table compact',
                style: {
                    width: '100%',
                    marginTop: '15px',
                    marginBottom: '15px'
                }
            });

            // add table header row
            pheTable.tr()
                .th({ addClass: 'left', html: '<b>Key</b>' })
                .th({ addClass: 'left', html: '<b>Value</b>' });

            for (var key in historyEntry){
                var val = historyEntry[key], formattedVal = '';
                if (Array.isArray(val)) {
                    var items = [];
                    val.forEach(function(item){
                        if (typeof item === 'object') item = JSON.stringify(item);
                        items.push(spawn('li',[ spawn('code',item) ]));
                    });
                    formattedVal = spawn('ul',{ style: { 'list-style-type': 'none', 'padding-left': '0' }}, items);
                } else if (typeof val === 'object' ) {
                    formattedVal = spawn('code', JSON.stringify(val));
                } else if (!val) {
                    formattedVal = spawn('code','false');
                } else {
                    formattedVal = spawn('code',val);
                }

                pheTable.tr()
                    .td('<b>'+key+'</b>')
                    .td([ spawn('div',{ style: { 'word-break': 'break-all','max-width':'600px' }}, formattedVal) ]);
            }

            // display history
            XNAT.ui.dialog.open({
                title: historyEntry['wrapper-name'],
                width: 800,
                scroll: true,
                content: pheTable.table,
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: true
                    }
                ]
            });
        } else {
            XNAT.ui.dialog.open({
                content: 'Sorry, could not display this history item.',
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: true
                    }
                ]
            });
        }
    };

    historyTable.init = function(container){
        var manager = $$(container || '#command-history-container');
        manager.html('');

        manager.append(historyTable.table());
    };

    historyTable.init();

}));
