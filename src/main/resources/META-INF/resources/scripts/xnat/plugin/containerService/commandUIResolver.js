/*
 * web: commandUiResolver.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Flexible script to be used in the UI to launch
 */

console.log('commandUIResolver.js');

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
        selectors,
        undefined,
        rootUrl = XNAT.url.rootUrl,
        csrfUrl = XNAT.url.csrfUrl,
        projectId = XNAT.data.context.projectID,
        xsiType = XNAT.data.context.xsiType;

    XNAT.plugin =
        getObject(XNAT.plugin || {});

    XNAT.plugin.containerService =
        getObject(XNAT.plugin.containerService || {});

    XNAT.plugin.containerService.launcher = launcher =
        getObject(XNAT.plugin.containerService.launcher || {});

    XNAT.plugin.containerService.selectors = selectors =
        getObject(XNAT.plugin.containerService.selectors || {});

    /*
     * Panel form elements for launcher
     */

    var helptext = function(description) {
        return (description) ? '' : spawn('div.description',description);
    };
    var vertSpace = function(condensed) {
        return (condensed) ? '' : spawn('br.clear');
    };

    selectors.defaultConfigInput = function(opts){
        var name = opts.name,
            value = opts.value,
            label = opts.label,
            description = opts.description || '',
            required = opts.required || false,
            childOf = opts['parent'],
            classes = ['panel-input'],
            dataProps = {};
        value = (value === undefined || value === null || value == 'null') ? '' : value;
        label = label || name;
        description = description || '';

        if (required) {
            classes.push('required');
            description += ' (Required)';
        }
        if (childOf) {
            classes.push('hidden');
            dataProps['childOf'] = childOf;
        }

        return XNAT.ui.panel.input.text({
            name: name,
            value: value,
            description: description,
            label: label,
            data: dataProps,
            className: classes.split(' ')
        }).element;
    };

    selectors.configCheckbox = function(opts){
        var name = opts.name,
            value = opts.value,
            checked = opts.checked,
            boolean = opts.boolean,
            outerLabel = opts.outerLabel,
            innerLabel = opts.innerLabel,
            description = opts.description || '',
            required = opts.required || false,
            condensed = opts.condensed || false,
            childOf = opts['parent'],
            classes = ['panel-element panel-input'],
            dataProps = { name: name },
            disabled = false;

        checked = (checked === 'true') ? ' checked' : '';

        value = (boolean) ? 'true' : value;

        if (required) {
            classes.push('required');
            description += ' (Required)';
        }
        if (childOf) {
            classes.push('hidden');
            dataProps['childOf'] = childOf;
            disabled = 'disabled';
        }

        return spawn('div', { className: classes.split(' '), data: dataProps }, [
            spawn('label.element-label', outerLabel),
            spawn('div.element-wrapper', [
                spawn('label', [
                    spawn('input', { type: 'checkbox', name: name, value: value, attr: {
                        'checked': checked,
                        'disabled': disabled
                    } }),
                    innerLabel
                ]),
                helptext(description)
            ]),
            vertSpace(condensed)
        ]);
    };

    selectors.hiddenConfigInput = function(opts) {
        var name = opts.name,
            value = opts.value,
            childOf = opts.childOf,
            dataProps = {},
            disabled = false;

        if (childOf) {
            dataProps['childOf'] = childOf;
            disabled = 'disabled';
        }

        return XNAT.ui.input.hidden({
            name: name,
            value: value,
            data: dataProps,
            attr: {
                'disabled': disabled
            }
        }).element;
    };

    selectors.staticConfigInput = function(opts) {
        var name = opts.name,
            value = opts.value,
            text = opts.description,
            childOf = opts.childOf,
            classes = ['panel-element'],
            dataProps = { name: name },
            disabled = false;

        if (childOf) {
            classes.push('hidden');
            dataProps['childOf'] = childOf;
            disabled = 'disabled';
        }

        return spawn(
            'div', { className: classes.split(' '), data: dataProps }, [
                spawn('label.element-label', name),
                spawn('div.element-wrapper', text),
                XNAT.ui.panel.input.hidden({
                    name: name,
                    value: value,
                    attr: { 'disabled': disabled }
                }).element,
                spawn('br.clear')
            ]
        );
    };

    selectors.staticConfigList = function(name,list) {
        var listArray = list.split(',');
        listArray.forEach(function(item,i){
            listArray[i] = '<li>'+item+'</li>'
        });
        return spawn(
            'div.panel-element', { data: { name: name } }, [
                spawn('label.element-label', name),
                spawn('div.element-wrapper', [
                    spawn('ul',{ style: {
                        'list-style-type': 'none',
                        margin: 0,
                        padding: 0
                    }},listArray.join(''))
                ]),
                spawn('br.clear')
            ]
        )
    };

    launcher.formInputs = function(inputs) {
        var formPanelElements = [];

        for (var i in inputs) {
            var input = inputs[i];

            var opts = {
                name: i,
                label: input.label || i,
                value: input.value,
                childOf: input.childOf || false,
                description: input.description || null,
                required: input.required || false
            };
            // create a panel.input for each input type
            switch (input.type){
                case 'scanSelectMany':
                    launcher.scanList.forEach(function(scan,i){
                        var scanOpts = {
                            name: 'scan',
                            value: fullScanPath(scan.id),
                            innerLabel: scan.id + ' - ' + scan.series_description,
                            condensed: true
                        };
                        if (i === 0) {
                            // first
                            scanOpts.outerLabel = 'scans';
                            formPanelElements.push(configCheckbox(scanOpts));
                        } else if (i < launcher.scanList.length-1){
                            // middle
                            formPanelElements.push(configCheckbox(scanOpts));
                        } else {
                            // last
                            scanOpts.condensed = false;
                            formPanelElements.push(configCheckbox(scanOpts));
                        }
                    });
                    break;
                case 'hidden':
                    formPanelElements.push(hiddenConfigInput(opts));
                    break;
                case 'static':
                    formPanelElements.push(staticConfigInput(opts));
                    break;
                case 'staticList':
                    formPanelElements.push(staticConfigList(i,input.value));
                    break;
                case 'boolean':
                    opts.boolean = true;
                    opts.outerLabel = opts.label;
                    opts.innerLabel = input.innerLabel || 'True';
                    opts.checked = (input.value === 'true') ? 'checked' : false;
                    formPanelElements.push(hiddenConfigInput(opts));
                    break;
                default:
                    formPanelElements.push(defaultConfigInput(opts));
            }
        }


        // return spawned panel.
        return formPanelElements;
    };


}));