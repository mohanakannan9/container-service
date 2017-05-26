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

console.log('commandUiResolver.js');

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

    var defaultConfigInput = function(input){
        var name = input.name || input.label,
            value = input.value,
            label = input.label,
            description = input.description || '',
            required = input.required || false,
            childOf = input['parent'],
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
            className: classes.join(' ')
        }).element;
    };

    var configCheckbox = function(input){
        var name = input.name || input.outerLabel,
            value = input.value,
            checked = input.checked,
            boolean = input.boolean,
            outerLabel = input.outerLabel,
            innerLabel = input.innerLabel,
            description = input.description || '',
            required = input.required || false,
            condensed = input.condensed || false,
            childOf = input['parent'],
            classes = ['panel-element panel-input'],
            dataProps = { name: name },
            disabled = input.disabled || false,
            attr = {};

        if (checked === 'true') attr['checked'] = 'checked';
        if (disabled) attr['disabled'] = 'disabled';

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

        return spawn('div', { className: classes.join(' '), data: dataProps }, [
            spawn('label.element-label', outerLabel),
            spawn('div.element-wrapper', [
                spawn('label', [
                    spawn('input', { type: 'checkbox', name: name, value: value, attr: attr }),
                    innerLabel
                ]),
                helptext(description)
            ]),
            vertSpace(condensed)
        ]);
    };

    var hiddenConfigInput = function(input) {
        var name = input.name || input.label,
            value = input.value,
            childOf = input['parent'],
            dataProps = {},
            attr = (input.disabled) ? { 'disabled':'disabled' } : {};

        return XNAT.ui.input.hidden({
            name: name,
            value: value,
            data: dataProps,
            attr: attr
        }).element;
    };

    var staticConfigInput = function(input) {
        var name = input.name || input.label,
            value = input.value,
            valueLabel = input.valueLabel,
            childOf = input['parent'],
            classes = ['panel-element'],
            dataProps = { name: name },
            attr = (input.disabled) ? { 'disabled':'disabled' } : {};

        return spawn(
            'div', { className: classes.join(' '), data: dataProps }, [
                spawn('label.element-label', name),
                spawn('div.element-wrapper', { style: { 'word-wrap': 'break-word' } }, valueLabel),
                spawn('input',{
                    type: 'hidden',
                    name: name,
                    value: value,
                    data: dataProps,
                    attr: attr
                }),
                spawn('br.clear')
            ]
        );
    };

    var staticConfigList = function(name,list) {
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

    launcher.formInputs = function(inputs,advanced) {
        var formPanelElements = [];
        advanced = advanced || false;

        for (var i in inputs) {
            var input = inputs[i];

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
                    formPanelElements.push(hiddenConfigInput(input));
                    break;
                case 'static':
                    formPanelElements.push(staticConfigInput(input));
                    break;
                case 'staticList':
                    formPanelElements.push(staticConfigList(i,input.value));
                    break;
                case 'boolean':
                    input.boolean = true;
                    input.outerLabel = input.label;
                    input.innerLabel = input.innerLabel || 'True';
                    input.checked = (input.value === 'true') ? 'checked' : false;
                    formPanelElements.push(configCheckbox(input));
                    break;
                default:
                    formPanelElements.push(defaultConfigInput(input));
            }
        }

        if (!advanced) {
            // return just the form elements
            return formPanelElements;
        } else {
            // return a collapsible container containing the form elements
            return spawn('div.advancedSettings',[
                spawn('p.advancedSettingsToggle'),
                spawn('div.advancedSettingsContents',formPanelElements)
                ]
            );
        }
    };


}));